/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.appservice.actions;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.common.FileChooser;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.function.Function;

// todo: Clean up duplicate codes in UIHelper and IDEHelper
public class AppServiceFileAction {
    private static final String APP_SERVICE_FILE_EDITING = "App Service File Editing";
    private static final String FILE_HAS_BEEN_DELETED = "File '%s' has been deleted from remote server, " +
        "do you want to create a new file with the changed content?";
    private static final String FILE_HAS_BEEN_MODIFIED = "File '%s' has been modified since you view it, do you still want to save your changes?";
    private static final String SAVE_CHANGES = "Do you want to save your changes?";
    private static final Key<String> APP_SERVICE_FILE_ID = new Key<>("APP_SERVICE_FILE_ID");
    private static final String ERROR_DOWNLOADING = "Failed to download file[%s] to [%s].";
    private static final String FILE_HAS_BEEN_SAVED = "File %s has been saved to Azure";

    @AzureOperation(
        name = "appservice.open_file.file",
        params = {"target.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    @SneakyThrows
    public void openAppServiceFile(AppServiceFile target, Object context) {
        final AppServiceAppBase<?, ?, ?> appService = target.getApp();
        final FileEditorManager manager = FileEditorManager.getInstance((Project) context);
        final AzureString title = OperationBundle.description("appservice.open_file.file", target.getName());
        final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            indicator.setText2("Checking file existence");
            final AppServiceFile file = appService.getFileByPath(target.getPath());
            if (file == null) {
                final String failureFileDeleted = String.format("Target file (%s) has been deleted", target.getName());
                UIUtil.invokeLaterIfNeeded(() -> Messages.showWarningDialog(failureFileDeleted, "Open File"));
                return;
            }
            indicator.setText2("Loading file content");
            final String failure = String.format("Can not open file (%s). Try downloading it first and open it manually.", target.getName());
            if (target.getSize() > 10 * FileUtils.ONE_MB) {
                AzureTaskManager.getInstance().runLater(() -> Messages.showWarningDialog(failure, "Open File"));
                return;
            }
            final VirtualFile vf = VirtualFileActions.getVirtualFile(file.getId(), manager);
            if (Objects.nonNull(vf)) {
                AzureTaskManager.getInstance().runLater(() -> manager.openFile(vf, true));
            } else {
                downloadAndOpen(file, manager.getProject());
            }
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    @SneakyThrows
    private void downloadAndOpen(AppServiceFile file, Object context) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) context);
        final File temp = FileUtil.createTempFile("", file.getName(), true);
        final FileOutputStream output = new FileOutputStream(temp);
        final Function<String, Boolean> onSave = content -> {
            saveFileToAzure(file, content, fileEditorManager.getProject());
            return true;
        };
        final Runnable onClose = () -> {
            if (Path.of(file.getPath()).toString().contains(Path.of(FileUtil.getTempDirectory()).toString())) {
                WriteAction.run(() -> FileUtil.delete(temp));
            }
        };
        file.getApp()
            .getFileContent(file.getPath())
            .doOnComplete(() -> AzureTaskManager.getInstance().runLater(() -> {
                final VirtualFile virtualFile = VirtualFileActions.createVirtualFile(file.getId(), file.getFullName(), temp, fileEditorManager);
                if (!VirtualFileActions.openFileInEditor(virtualFile, onSave, onClose, fileEditorManager)) {
                    final String failure = String.format("Can not open file (%s). Try downloading it first and open it manually.", file.getName());
                    Messages.showWarningDialog(failure, "Open File");
                }
            }, AzureTask.Modality.NONE))
            .doAfterTerminate(() -> IOUtils.closeQuietly(output, null))
            .subscribe(bytes -> {
                try {
                    if (bytes != null) {
                        output.write(bytes.array(), 0, bytes.limit());
                    }
                } catch (final IOException e) {
                    final String error = "failed to load data into editor";
                    final String action = "try later or downloading it first";
                    final Action<Void> retry = Action.retryFromFailure((() -> this.openAppServiceFile(file, context)));
                    throw new AzureToolkitRuntimeException(error, e, action, retry);
                }
            }, AppServiceFileAction::onRxException);
    }

    @AzureOperation(
        name = "appservice.save_file.file",
        params = {"appServiceFile.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    private void saveFileToAzure(final AppServiceFile appServiceFile, final String content, final Project project) {
        final AzureString title = OperationBundle.description("appservice.save_file.file", appServiceFile.getName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            final AppServiceAppBase<?, ?, ?> appService = appServiceFile.getApp();
            final AppServiceFile target = appService.getFileByPath(appServiceFile.getPath());
            final boolean deleted = target == null;
            final boolean outDated = !deleted && ZonedDateTime.parse(target.getMtime()).isAfter(ZonedDateTime.parse(appServiceFile.getMtime()));
            boolean toSave = true;
            if (deleted) {
                toSave = AzureMessager.getMessager().confirm(String.format(FILE_HAS_BEEN_DELETED, appServiceFile.getName()), APP_SERVICE_FILE_EDITING);
            } else if (outDated) {
                toSave = AzureMessager.getMessager().confirm(String.format(FILE_HAS_BEEN_MODIFIED, appServiceFile.getName()), APP_SERVICE_FILE_EDITING);
            }
            if (toSave) {
                appService.uploadFileToPath(content, appServiceFile.getPath());
                AzureMessager.getMessager().success(AzureString.format(FILE_HAS_BEEN_SAVED, appServiceFile.getName()), APP_SERVICE_FILE_EDITING);
            }
        }));
    }

    @SneakyThrows
    public void saveAppServiceFile(@NotNull AppServiceFile file, @Nullable Project project, @Nullable File dest) {
        if (!file.getApp().getFormalStatus().isRunning()) {
            AzureMessager.getMessager().warning(AzureString.format(
                "Can not download files from app service(%s) because it's not running", file.getApp().getName()));
            return;
        }
        final Action<Void> retry = Action.retryFromFailure((() -> this.saveAppServiceFile(file, project, dest)));
        final File destFile = Objects.isNull(dest) ? FileChooser.showFileSaver("Download", file.getName()) : dest;
        if (Objects.isNull(destFile)) {
            return;
        }
        final OutputStream output = new FileOutputStream(destFile);
        final AzureString title = OperationBundle.description("appservice.download_file.file", file.getName());
        final AzureTask<Void> task = new AzureTask<>(project, title, false, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            file.getApp()
                .getFileContent(file.getPath())
                .doOnComplete(() -> VirtualFileActions.notifyDownloadSuccess(file.getName(), destFile, project))
                .doOnTerminate(() -> IOUtils.closeQuietly(output, null))
                .subscribe(bytes -> {
                    try {
                        if (bytes != null) {
                            output.write(bytes.array(), 0, bytes.limit());
                        }
                    } catch (final IOException e) {
                        final String error = "failed to write data into local file";
                        throw new AzureToolkitRuntimeException(error, e, retry);
                    }
                }, AppServiceFileAction::onRxException);
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    private static void onRxException(Throwable e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        AzureMessager.getMessager().error(e);
    }
}
