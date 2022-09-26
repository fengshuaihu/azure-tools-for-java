/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.DatabaseServerConfig;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServerDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class CreateSqlServerAction {
    public static void create(@Nonnull Project project, @Nullable DatabaseServerConfig data) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final SqlServerCreationDialog dialog = new SqlServerCreationDialog(project);
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

    @AzureOperation(name = "sqlserver.create_server.server", params = {"config.getName()"}, type = AzureOperation.Type.ACTION)
    private static void doCreate(final DatabaseServerConfig config, final Project project) {
        final AzureString title = OperationBundle.description("sqlserver.create_server.server", config.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final ResourceGroup rg = config.getResourceGroup();
            if (Objects.nonNull(rg) && rg.isDraftForCreating()) {
                new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getRegion()).execute();
            }
            final MicrosoftSqlServerDraft draft = Azure.az(AzureSqlServer.class).servers(config.getSubscription().getId())
                .create(config.getName(), config.getResourceGroup().getName());
            draft.setConfig(config);
            draft.commit();
            CacheManager.getUsageHistory(MicrosoftSqlServer.class).push(draft);
        });
    }
}
