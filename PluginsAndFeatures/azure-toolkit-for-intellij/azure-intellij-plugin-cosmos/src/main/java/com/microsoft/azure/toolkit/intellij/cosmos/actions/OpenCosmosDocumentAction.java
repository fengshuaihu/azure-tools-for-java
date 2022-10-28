/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.AppTopics;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocument;
import lombok.SneakyThrows;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class OpenCosmosDocumentAction {
    private static final String APP_SERVICE_FILE_EDITING = "App Service File Editing";
    private static final String FILE_HAS_BEEN_DELETED = "File '%s' has been deleted from remote server, " +
            "do you want to create a new file with the changed content?";
    private static final String FILE_HAS_BEEN_MODIFIED = "File '%s' has been modified since you view it, do you still want to save your changes?";
    private static final String SAVE_CHANGES = "Do you want to save your changes?";
    private static final Key<String> DOCUMENT_FILE_ID = new Key<>("DOCUMENT_FILE_ID");
    private static final String ERROR_DOWNLOADING = "Failed to download file[%s] to [%s].";
    private static final String SUCCESS_DOWNLOADING = "File[%s] is successfully downloaded to [%s].";
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String FILE_HAS_BEEN_SAVED = "File %s has been saved to Azure";

    @AzureOperation(
            name = "appservice.open_file.file",
            params = {"target.getName()"},
            type = AzureOperation.Type.SERVICE
    )
    @SneakyThrows
    public static void open(ICosmosDocument target, Project project) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final VirtualFile virtualFile = getOrCreateVirtualFile(target, fileEditorManager);
        final OutputStream output = virtualFile.getOutputStream(null);
        final AzureString title = OperationBundle.description("appservice.open_file.file", virtualFile.getName());
        AzureTaskManager.getInstance().runLater(new AzureTask<>(title, () -> {
            openFileInEditor(target::updateDocument, virtualFile, fileEditorManager);
        }));
    }

    private static boolean openFileInEditor(final Consumer<? super ObjectNode> contentSaver, VirtualFile virtualFile, FileEditorManager fileEditorManager) {
        final FileEditor[] editors = fileEditorManager.openFile(virtualFile, true, true);
        if (editors.length == 0) {
            return false;
        }
        for (final FileEditor fileEditor : editors) {
            if (fileEditor instanceof TextEditor) {
                final Document sqlDocument = FileDocumentManager.getInstance().getDocument(virtualFile);
                final MessageBusConnection messageBusConnection = fileEditorManager.getProject().getMessageBus().connect(fileEditor);
                messageBusConnection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerListener() {
                    @Override
                    public void beforeDocumentSaving(@NotNull Document document) {
                        if (!ObjectUtils.equals(document, sqlDocument)) {
                            return;
                        }
                        final AzureString title = AzureString.format("Saving document %s to Azure", virtualFile.getName());
                        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(title, () -> {
                            try {
                                final String content = getTextEditorContent((TextEditor) fileEditor);
                                final ObjectNode node = new ObjectMapper().readValue(content, ObjectNode.class);
                                contentSaver.consume(node);
                                AzureMessager.getMessager().info(String.format("Save document %s to Azure successfully.", virtualFile.getName()));
                            } catch (RuntimeException | JsonProcessingException e) {
                                AzureMessager.getMessager().error(e);
                            }
                        }));
                    }
                });
            }
        }
        return true;
    }

    private static String getTextEditorContent(TextEditor textEditor) {
        return textEditor.getEditor().getDocument().getText();
    }

    private static synchronized VirtualFile getOrCreateVirtualFile(final ICosmosDocument document, final FileEditorManager manager) {
        synchronized (document) {
            return Arrays.stream(manager.getOpenFiles())
                    .filter(f -> StringUtils.equals(f.getUserData(DOCUMENT_FILE_ID), document.getName()))
                    .findFirst().orElse(createVirtualFile(document, manager));
        }
    }

    @SneakyThrows
    private static VirtualFile createVirtualFile(final ICosmosDocument document, FileEditorManager manager) {
        final File tempFile = FileUtil.createTempFile(document.getName(), ".json", true);
        FileUtil.writeToFile(tempFile, document.getDocument().toPrettyString());
        final VirtualFile result = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile);
        result.setCharset(StandardCharsets.UTF_8);
        result.putUserData(DOCUMENT_FILE_ID, document.getName());
        result.setWritable(true);
        return result;
    }
}
