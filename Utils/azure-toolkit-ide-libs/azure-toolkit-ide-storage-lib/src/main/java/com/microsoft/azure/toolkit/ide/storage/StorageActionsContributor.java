/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.storage;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.storage.action.OpenAzureStorageExplorerAction;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobFile;
import com.microsoft.azure.toolkit.lib.storage.blob.IBlobFile;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import com.microsoft.azure.toolkit.lib.storage.share.IShareFile;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class StorageActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.storage.service";
    public static final String ACCOUNT_ACTIONS = "actions.storage.account";
    public static final String FILE_ACTIONS = "actions.storage.file";
    public static final String DIRECTORY_ACTIONS = "actions.storage.directory";
    public static final String CONTAINER_ACTIONS = "actions.storage.container";
    public static final String SHARE_ACTIONS = "actions.storage.share";
    public static final String QUEUE_ACTIONS = "actions.storage.queue";
    public static final String TABLE_ACTIONS = "actions.storage.table";
    public static final String STORAGE_MODULE_ACTIONS = "actions.storage.module";

    public static final Action.Id<AzResource> OPEN_AZURE_STORAGE_EXPLORER = Action.Id.of("storage.open_azure_storage_explorer");
    public static final Action.Id<StorageAccount> COPY_CONNECTION_STRING = Action.Id.of("storage.copy_connection_string");
    public static final Action.Id<StorageAccount> COPY_PRIMARY_KEY = Action.Id.of("storage.copy_primary_key");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_ACCOUNT = Action.Id.of("group.create_storage_account");

    public static final Action.Id<IBlobFile> CREATE_BLOB = Action.Id.of("storage.create_blob");
    public static final Action.Id<StorageFile> OPEN_FILE = Action.Id.of("storage.open_file");
    public static final Action.Id<StorageFile> CREATE_FILE = Action.Id.of("storage.create_file");
    public static final Action.Id<StorageFile> CREATE_DIRECTORY = Action.Id.of("storage.create_directory");
    public static final Action.Id<StorageFile> DOWNLOAD_FILE = Action.Id.of("storage.download_file");
    public static final Action.Id<StorageFile> UPLOAD_FILES = Action.Id.of("storage.upload_files");
    public static final Action.Id<StorageFile> UPLOAD_FILE = Action.Id.of("storage.upload_file");
    public static final Action.Id<StorageFile> UPLOAD_FOLDER = Action.Id.of("storage.upload_folder");
    public static final Action.Id<StorageFile> COPY_FILE_URL = Action.Id.of("storage.copy_file_url");
    public static final Action.Id<StorageFile> COPY_FILE_SAS_URL = Action.Id.of("storage.copy_file_sas_url");
    public static final Action.Id<StorageFile> DELETE_DIRECTORY = Action.Id.of("storage.delete_directory");

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<AzResource> openAzureStorageExplorer = resource -> {
            if (resource instanceof StorageAccount) {
                new OpenAzureStorageExplorerAction().openResource((StorageAccount) resource);
            } else if (resource instanceof AbstractAzResource && ((AbstractAzResource<?, ?, ?>) resource).getParent() instanceof StorageAccount) {
                //noinspection unchecked
                new OpenAzureStorageExplorerAction().openResource((AbstractAzResource<?, StorageAccount, ?>) resource);
            } else {
                AzureMessager.getMessager().warning("Only Azure Storages can be opened with Azure Storage Explorer.");
            }
        };
        final ActionView.Builder openAzureStorageExplorerView = new ActionView.Builder("Open Azure Storage Explorer")
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.open_azure_storage_explorer.account", ((AzResource) r).getName())).orElse(null))
            .enabled(s -> (s instanceof StorageAccount && ((AzResource) s).getFormalStatus().isConnected()) || s instanceof AzResource);
        final Action<AzResource> openAzureStorageExplorerAction = new Action<>(OPEN_AZURE_STORAGE_EXPLORER, openAzureStorageExplorer, openAzureStorageExplorerView);
        openAzureStorageExplorerAction.setShortcuts(am.getIDEDefaultShortcuts().edit());
        am.registerAction(OPEN_AZURE_STORAGE_EXPLORER, openAzureStorageExplorerAction);

        final Consumer<StorageAccount> copyConnectionString = resource -> {
            copyContentToClipboard(resource.getConnectionString());
            AzureMessager.getMessager().info("Connection string copied");
        };
        final ActionView.Builder copyConnectionStringView = new ActionView.Builder("Copy Connection String")
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.copy_connection_string.account", ((StorageAccount) r).getName())).orElse(null))
            .enabled(s -> s instanceof StorageAccount && ((StorageAccount) s).getFormalStatus().isConnected());
        final Action<StorageAccount> copyConnectionStringAction = new Action<>(COPY_CONNECTION_STRING, copyConnectionString, copyConnectionStringView);
        am.registerAction(COPY_CONNECTION_STRING, copyConnectionStringAction);

        final Consumer<StorageAccount> copyPrimaryKey = resource -> {
            copyContentToClipboard(resource.getKey());
            AzureMessager.getMessager().info("Primary key copied");
        };
        final ActionView.Builder copyPrimaryView = new ActionView.Builder("Copy Primary Key")
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.copy_primary_key.account", ((StorageAccount) r).getName())).orElse(null))
            .enabled(s -> s instanceof StorageAccount && ((StorageAccount) s).getFormalStatus().isConnected());
        final Action<StorageAccount> copyPrimaryKeyAction = new Action<>(COPY_PRIMARY_KEY, copyPrimaryKey, copyPrimaryView);
        am.registerAction(COPY_PRIMARY_KEY, copyPrimaryKeyAction);

        final ActionView.Builder openFileView = new ActionView.Builder("Open in Editor")
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.open_file.file", ((StorageFile) r).getName())).orElse(null))
            .enabled(s -> s instanceof StorageFile);
        am.registerAction(OPEN_FILE, new Action<>(OPEN_FILE, openFileView).setShortcuts(am.getIDEDefaultShortcuts().edit()));

        final ActionView.Builder createBlobView = new ActionView.Builder("Create Empty Blob", AzureIcons.Action.CREATE.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.create_blob.blob", ((StorageFile) r).getPath())).orElse(null))
            .enabled(s -> s instanceof IBlobFile && ((StorageFile) s).isDirectory());
        am.registerAction(CREATE_BLOB, new Action<>(CREATE_BLOB, createBlobView));

        final ActionView.Builder createFileView = new ActionView.Builder("Create Empty File", AzureIcons.Action.CREATE.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.create_file.file", ((StorageFile) r).getName())).orElse(null))
            .enabled(s -> s instanceof IShareFile && ((StorageFile) s).isDirectory());
        am.registerAction(CREATE_FILE, new Action<>(CREATE_FILE, createFileView));

        final ActionView.Builder createDirView = new ActionView.Builder("Create Subdirectory", AzureIcons.Action.CREATE.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.create_directory.dir", ((StorageFile) r).getName())).orElse(null))
            .enabled(s -> s instanceof IShareFile && ((StorageFile) s).isDirectory());
        am.registerAction(CREATE_DIRECTORY, new Action<>(CREATE_DIRECTORY, createDirView));

        final ActionView.Builder uploadFilesView = new ActionView.Builder("Upload Files", AzureIcons.Action.UPLOAD.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.upload_files.dir", ((StorageFile) r).getName())).orElse(null))
            .enabled(s -> s instanceof StorageFile && ((StorageFile) s).isDirectory());
        am.registerAction(UPLOAD_FILES, new Action<>(UPLOAD_FILES, uploadFilesView));

        final ActionView.Builder uploadFileView = new ActionView.Builder("Upload File", AzureIcons.Action.UPLOAD.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.upload_file.file", ((StorageFile) r).getName())).orElse(null))
            .enabled(s -> s instanceof StorageFile && !((StorageFile) s).isDirectory());
        am.registerAction(UPLOAD_FILE, new Action<>(UPLOAD_FILE, uploadFileView));

        final ActionView.Builder uploadFolderView = new ActionView.Builder("Upload Folder", AzureIcons.Action.UPLOAD.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.upload_folder.dir", ((StorageFile) r).getName())).orElse(null))
            .enabled(s -> s instanceof StorageFile && ((StorageFile) s).isDirectory());
        am.registerAction(UPLOAD_FOLDER, new Action<>(UPLOAD_FOLDER, uploadFolderView));

        final ActionView.Builder downloadFileView = new ActionView.Builder("Download", AzureIcons.Action.DOWNLOAD.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.download_file.file", ((StorageFile) r).getName())).orElse(null))
            .enabled(s -> s instanceof StorageFile && !((StorageFile) s).isDirectory());
        am.registerAction(DOWNLOAD_FILE, new Action<>(DOWNLOAD_FILE, downloadFileView));

        final ActionView.Builder copyUrlView = new ActionView.Builder("Copy URL")
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.copy_file_url.file", ((StorageFile) r).getName())).orElse(null))
            .enabled(s -> s instanceof StorageFile);
        am.registerAction(COPY_FILE_URL, new Action<>(COPY_FILE_URL, copyUrlView));

        final ActionView.Builder copySasUrlView = new ActionView.Builder("Generate and Copy SAS Token and URL")
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.copy_file_sas_url.file", ((StorageFile) r).getName())).orElse(null))
            .enabled(s -> s instanceof StorageFile);
        am.registerAction(COPY_FILE_SAS_URL, new Action<>(COPY_FILE_SAS_URL, copySasUrlView));

        final Consumer<StorageFile> deleteDir = s -> {
            if (AzureMessager.getMessager().confirm(AzureString.format("Are you sure to delete directory \"%s\" and all its contents?", s.getName()))) {
                ((Deletable) s).delete();
            }
        };
        final ActionView.Builder deleteDirView = new ActionView.Builder("Delete Directory", AzureIcons.Action.DELETE.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.delete_directory.dir", ((StorageFile) r).getName())).orElse(null))
            .enabled(s -> s instanceof StorageFile && ((StorageFile) s).isDirectory());
        final Action<StorageFile> deleteAction = new Action<>(DELETE_DIRECTORY, deleteDir, deleteDirView);
        deleteAction.setShortcuts(am.getIDEDefaultShortcuts().delete());
        am.registerAction(DELETE_DIRECTORY, deleteAction);

        final ActionView.Builder createAccountView = new ActionView.Builder("Storage Account")
            .title(s -> Optional.ofNullable(s).map(r -> description("storage.create_account.group", ((ResourceGroup) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceGroup && ((ResourceGroup) s).getFormalStatus().isConnected());
        am.registerAction(GROUP_CREATE_ACCOUNT, new Action<>(GROUP_CREATE_ACCOUNT, createAccountView));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup accountActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            StorageActionsContributor.COPY_CONNECTION_STRING,
            StorageActionsContributor.COPY_PRIMARY_KEY,
            "---",
            ResourceCommonActionsContributor.CONNECT,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(ACCOUNT_ACTIONS, accountActionGroup);

        final ActionGroup moduleActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(STORAGE_MODULE_ACTIONS, moduleActionGroup);

        final ActionGroup fileActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            "---",
            StorageActionsContributor.CREATE_BLOB,
            StorageActionsContributor.CREATE_FILE,
            StorageActionsContributor.CREATE_DIRECTORY,
            "---",
            StorageActionsContributor.DOWNLOAD_FILE,
            "---",
            StorageActionsContributor.COPY_FILE_URL,
            StorageActionsContributor.COPY_FILE_SAS_URL,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(FILE_ACTIONS, fileActionGroup);

        final ActionGroup dirActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            "---",
            StorageActionsContributor.CREATE_BLOB,
            StorageActionsContributor.CREATE_FILE,
            StorageActionsContributor.CREATE_DIRECTORY,
            "---",
            StorageActionsContributor.UPLOAD_FILES,
            StorageActionsContributor.DOWNLOAD_FILE,
            "---",
            StorageActionsContributor.COPY_FILE_URL,
            StorageActionsContributor.COPY_FILE_SAS_URL,
            "---",
            StorageActionsContributor.DELETE_DIRECTORY
        );
        am.registerGroup(DIRECTORY_ACTIONS, dirActionGroup);

        final ActionGroup containerActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
            "---",
            StorageActionsContributor.CREATE_BLOB,
            StorageActionsContributor.UPLOAD_FILES,
            StorageActionsContributor.UPLOAD_FOLDER,
            "---",
            StorageActionsContributor.COPY_FILE_URL,
            StorageActionsContributor.COPY_FILE_SAS_URL,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(CONTAINER_ACTIONS, containerActionGroup);

        final ActionGroup shareActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
            "---",
            StorageActionsContributor.CREATE_FILE,
            StorageActionsContributor.CREATE_DIRECTORY,
            StorageActionsContributor.UPLOAD_FILES,
            StorageActionsContributor.UPLOAD_FOLDER,
            "---",
            StorageActionsContributor.COPY_FILE_URL,
            StorageActionsContributor.COPY_FILE_SAS_URL,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(SHARE_ACTIONS, shareActionGroup);

        final ActionGroup queueActionGroup = new ActionGroup(
            StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(QUEUE_ACTIONS, queueActionGroup);

        final ActionGroup tableActionGroup = new ActionGroup(
            StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(TABLE_ACTIONS, tableActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_ACCOUNT);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }

    public static void copyContentToClipboard(final String content) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(content), null);
    }
}
