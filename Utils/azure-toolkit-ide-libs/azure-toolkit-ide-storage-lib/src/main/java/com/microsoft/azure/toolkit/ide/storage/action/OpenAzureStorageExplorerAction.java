/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.storage.action;

import com.microsoft.azure.toolkit.ide.storage.action.explorer.AbstractAzureStorageExplorerHandler;
import com.microsoft.azure.toolkit.ide.storage.action.explorer.LinuxOpenAzureStorageExplorerHandler;
import com.microsoft.azure.toolkit.ide.storage.action.explorer.MacOSOpenAzureStorageExplorerHandler;
import com.microsoft.azure.toolkit.ide.storage.action.explorer.WindowsOpenAzureStorageExplorerHandler;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import org.apache.commons.lang3.SystemUtils;

public class OpenAzureStorageExplorerAction {
    private final AbstractAzureStorageExplorerHandler handler;

    public OpenAzureStorageExplorerAction() {
        if (SystemUtils.IS_OS_WINDOWS) {
            this.handler = new WindowsOpenAzureStorageExplorerHandler();
        } else if (SystemUtils.IS_OS_MAC) {
            this.handler = new MacOSOpenAzureStorageExplorerHandler();
        } else {
            this.handler = new LinuxOpenAzureStorageExplorerHandler();
        }
    }

    @AzureOperation(name = "storage.open_azure_storage_explorer.account", params = {"storageAccount.getName()"}, type = AzureOperation.Type.ACTION)
    public void openResource(final StorageAccount storageAccount) {
        this.handler.openResource(storageAccount);
    }

    @AzureOperation(name = "storage.open_azure_storage_explorer.storage", params = {"storage.getName()"}, type = AzureOperation.Type.ACTION)
    public void openResource(final AbstractAzResource<?, StorageAccount, ?> storage) {
        this.handler.openResource(storage);
    }
}
