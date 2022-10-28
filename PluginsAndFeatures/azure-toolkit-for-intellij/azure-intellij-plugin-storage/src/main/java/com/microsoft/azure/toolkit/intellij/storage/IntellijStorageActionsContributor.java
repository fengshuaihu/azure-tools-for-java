/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.storage.StorageActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.storage.component.StorageCreationDialog;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.storage.creation.CreateStorageAccountAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainerModule;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import com.microsoft.azure.toolkit.lib.storage.queue.QueueModule;
import com.microsoft.azure.toolkit.lib.storage.share.ShareModule;
import com.microsoft.azure.toolkit.lib.storage.table.TableModule;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijStorageActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzureStorageAccount;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateStorageAccountAction.create(e.getProject(), null);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);

        am.<AzResource, AnActionEvent>registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof StorageAccount,
            (r, e) -> AzureTaskManager.getInstance().runLater(OperationBundle.description("storage.open_azure_storage_explorer.account", r.getName()), () -> {
                final ConnectorDialog dialog = new ConnectorDialog(e.getProject());
                dialog.setResource(new AzureServiceResource<>(((StorageAccount) r), StorageAccountResourceDefinition.INSTANCE));
                dialog.show();
            }));

        am.registerHandler(ResourceCommonActionsContributor.CREATE, (m, e) -> m instanceof BlobContainerModule, this::createStorage);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (m, e) -> m instanceof ShareModule, this::createStorage);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (m, e) -> m instanceof QueueModule, this::createStorage);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (m, e) -> m instanceof TableModule, this::createStorage);
        am.registerHandler(StorageActionsContributor.OPEN_FILE, (file, e) -> StorageFileActions.openFileInEditor(file, ((AnActionEvent) e).getProject()));
        am.registerHandler(StorageActionsContributor.CREATE_BLOB, (file, e) -> StorageFileActions.createBlob(file, ((AnActionEvent) e).getProject()));
        am.registerHandler(StorageActionsContributor.CREATE_FILE, (file, e) -> StorageFileActions.createFile(file, ((AnActionEvent) e).getProject()));
        am.registerHandler(StorageActionsContributor.CREATE_DIRECTORY, (file, e) -> StorageFileActions.createDirectory(file, ((AnActionEvent) e).getProject()));
        am.registerHandler(StorageActionsContributor.UPLOAD_FILES, (file, e) -> StorageFileActions.uploadFiles(file, ((AnActionEvent) e).getProject()));
        am.registerHandler(StorageActionsContributor.UPLOAD_FILE, (file, e) -> StorageFileActions.uploadFile(file, ((AnActionEvent) e).getProject()));
        am.registerHandler(StorageActionsContributor.DOWNLOAD_FILE, (file, e) -> StorageFileActions.downloadFile(file, ((AnActionEvent) e).getProject()));
        am.registerHandler(StorageActionsContributor.COPY_FILE_URL, (file, e) -> StorageFileActions.copyUrl(file, ((AnActionEvent) e).getProject()));
        am.registerHandler(StorageActionsContributor.COPY_FILE_SAS_URL, (file, e) -> StorageFileActions.copySasUrl(file, ((AnActionEvent) e).getProject()));

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateAccountHandler = (r, e) -> {
            final StorageAccountConfig config = StorageAccountConfig.builder().build();
            config.setSubscription(r.getSubscription());
            config.setRegion(r.getRegion());
            config.setResourceGroup(r);
            CreateStorageAccountAction.create(e.getProject(), config);
        };
        am.registerHandler(StorageActionsContributor.GROUP_CREATE_ACCOUNT, (r, e) -> true, groupCreateAccountHandler);
    }

    private void createStorage(Object m, Object e) {
        @SuppressWarnings("unchecked") final AbstractAzResourceModule<?, StorageAccount, ?> module = (AbstractAzResourceModule<?, StorageAccount, ?>) m;
        final AnActionEvent event = (AnActionEvent) e;
        AzureTaskManager.getInstance().runLater(() -> {
            final StorageCreationDialog dialog = new StorageCreationDialog(module, event.getProject());
            dialog.setOkActionListener((name) -> {
                dialog.close();
                final AzureString title = OperationBundle.description("storage.create_storage.type|storage", module.getResourceTypeName(), name);
                AzureTaskManager.getInstance().runInBackground(title, () -> module.create(name, "").createIfNotExist());
            });
            dialog.show();
        });
    }

    @Override
    public int getOrder() {
        return StorageActionsContributor.INITIALIZE_ORDER + 1;
    }
}
