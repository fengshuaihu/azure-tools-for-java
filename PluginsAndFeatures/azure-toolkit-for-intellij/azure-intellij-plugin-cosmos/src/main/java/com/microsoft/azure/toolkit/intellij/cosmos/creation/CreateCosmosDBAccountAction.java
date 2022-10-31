/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccountDraft;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccountModule;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class CreateCosmosDBAccountAction {
    public static void create(@Nullable Project project, @Nullable CosmosDBAccountDraft.Config data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CosmosDBAccountCreationDialog dialog = new CosmosDBAccountCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            dialog.setOkActionListener(config -> {
                doCreate(config, project);
                dialog.close();
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "cosmos.create_account.account", params = {"config.getName()"}, type = AzureOperation.Type.ACTION)
    private static void doCreate(final CosmosDBAccountDraft.Config config, final Project project) {
        final AzureString title = OperationBundle.description("cosmos.create_account.account", config.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final ResourceGroup rg = config.getResourceGroup();
            if (rg.isDraftForCreating()) {
                new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getRegion()).execute();
            }
            final CosmosDBAccountModule module = az(AzureCosmosService.class).databaseAccounts(config.getSubscription().getId());
            final CosmosDBAccountDraft draft = module.create(config.getName(), config.getResourceGroup().getName());
            CacheManager.getUsageHistory(CosmosDBAccount.class).push(draft);
            draft.setConfig(config);
            draft.commit();
        });
    }
}
