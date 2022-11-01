/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.applicationinsights.core.dependencies.javaxannotation.Nonnull;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.LRUStack;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraTableDraft;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCollectionDraft;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainerDraft;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;

import javax.annotation.Nullable;

public class CreateCosmosContainerAction {
    public static void createSQLContainer(@Nonnull Project project, @Nonnull SqlDatabase database,
                                          @Nonnull final SqlContainerDraft.SqlContainerConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CosmosSQLContainerCreationDialog dialog = new CosmosSQLContainerCreationDialog(project, database);
            dialog.getForm().setValue(data);
            dialog.setOkActionListener((config) -> {
                dialog.close();
                doCreateSqlContainer(database, config);
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "cosmos.create_container.container|database", params = {"config.getContainerId()", "database.getName()"}, type = AzureOperation.Type.ACTION)
    private static void doCreateSqlContainer(@Nonnull SqlDatabase database, @Nullable final SqlContainerDraft.SqlContainerConfig config) {
        final AzureString title = OperationBundle.description("cosmos.create_container.container|database", config.getContainerId(), database.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final SqlContainerDraft draft = database.containers().create(config.getContainerId(), database.getResourceGroupName());
            draft.setConfig(config);
            draft.commit();
            final LRUStack history = CacheManager.getUsageHistory(draft.getClass());
            history.push(draft);
        });
    }

    public static void createMongoCollection(@Nonnull Project project, @Nonnull MongoDatabase database,
                                             @Nonnull final MongoCollectionDraft.MongoCollectionConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CosmosMongoCollectionCreationDialog dialog = new CosmosMongoCollectionCreationDialog(project, database);
            dialog.getForm().setValue(data);
            dialog.setOkActionListener((config) -> {
                dialog.close();
                doCreateMongoCollection(database, config);
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "cosmos.create_collection.collection|database", params = {"config.getCollectionId()", "database.getName()"}, type = AzureOperation.Type.ACTION)
    private static void doCreateMongoCollection(@Nonnull MongoDatabase database, @Nullable final MongoCollectionDraft.MongoCollectionConfig config) {
        final AzureString title = OperationBundle.description("cosmos.create_collection.collection|database", config.getCollectionId(), database.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final MongoCollectionDraft draft = database.collections().create(config.getCollectionId(), database.getResourceGroupName());
            draft.setConfig(config);
            draft.commit();
            final LRUStack history = CacheManager.getUsageHistory(draft.getClass());
            history.push(draft);
        });
    }

    public static void createCassandraTable(@Nonnull Project project, @Nonnull CassandraKeyspace keyspace,
                                            @Nonnull final CassandraTableDraft.CassandraTableConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CosmosCassandraTableCreationDialog dialog = new CosmosCassandraTableCreationDialog(project, keyspace);
            dialog.getForm().setValue(data);
            dialog.setOkActionListener((config) -> {
                dialog.close();
                doCreateCassandraTable(keyspace, config);
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "cosmos.create_table.table|keyspace", params = {"config.getTableId()", "keyspace.getName()"}, type = AzureOperation.Type.ACTION)
    private static void doCreateCassandraTable(@Nonnull CassandraKeyspace keyspace, @Nullable final CassandraTableDraft.CassandraTableConfig config) {
        final AzureString title = OperationBundle.description("cosmos.create_table.table|keyspace", config.getTableId(), keyspace.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final CassandraTableDraft draft = keyspace.tables().create(config.getTableId(), keyspace.getResourceGroupName());
            draft.setConfig(config);
            draft.commit();
            final LRUStack history = CacheManager.getUsageHistory(draft.getClass());
            history.push(draft);
        });
    }
}
