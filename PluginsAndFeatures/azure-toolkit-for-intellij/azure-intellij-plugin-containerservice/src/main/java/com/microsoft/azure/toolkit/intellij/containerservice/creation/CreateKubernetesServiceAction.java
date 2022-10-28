/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice.creation;

import com.google.common.base.Preconditions;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.containerservice.AzureContainerService;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesClusterDraft;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesClusterModule;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class CreateKubernetesServiceAction {
    public static void create(@Nonnull Project project, @Nullable KubernetesClusterDraft.Config data) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final KubernetesCreationDialog dialog = new KubernetesCreationDialog(project);
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

    public static KubernetesClusterDraft.Config getDefaultConfig(@Nullable final ResourceGroup resourceGroup) {
        final List<Subscription> subs = az(AzureAccount.class).account().getSelectedSubscriptions();
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(subs), "There are no subscriptions in your account.");
        final String name = String.format("kubernetes-service-%s", Utils.getTimestamp());
        final String defaultResourceGroupName = String.format("rg-%s", name);

        final Subscription historySub = CacheManager.getUsageHistory(Subscription.class).peek(subs::contains);
        final ResourceGroup historyRg = CacheManager.getUsageHistory(ResourceGroup.class)
            .peek(r -> Objects.isNull(historySub) ? subs.stream().anyMatch(s -> s.getId().equals(r.getSubscriptionId())) : r.getSubscriptionId().equals(historySub.getId()));
        final ResourceGroup group = Optional.ofNullable(resourceGroup)
            .or(() -> Optional.ofNullable(historyRg))
            .orElse(az(AzureResources.class).groups(subs.get(0).getId())
                .create(defaultResourceGroupName, defaultResourceGroupName));
        final Subscription subscription = Optional.of(group).map(AzResource::getSubscription)
            .or(() -> Optional.ofNullable(historySub))
            .orElse(subs.get(0));

        final KubernetesClusterDraft.Config config = new KubernetesClusterDraft.Config();
        config.setName(name);
        config.setSubscription(subscription);
        config.setResourceGroup(group);
        config.setDnsPrefix(String.format("%s-dns", name));
        config.setMinVMCount(3);
        config.setMaxVMCount(5);
        return config;
    }

    @AzureOperation(name = "kubernetes.create_cluster.cluster", params = {"config.getName()"}, type = AzureOperation.Type.ACTION)
    private static void doCreate(final KubernetesClusterDraft.Config config, final Project project) {
        final AzureString title = OperationBundle.description("kubernetes.create_cluster.cluster", config.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final ResourceGroup rg = config.getResourceGroup();
            if (rg.isDraftForCreating()) {
                new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getRegion()).execute();
            }
            final KubernetesClusterModule module = Azure.az(AzureContainerService.class).kubernetes(config.getSubscription().getId());
            final KubernetesClusterDraft draft = module.create(config.getName(), config.getResourceGroup().getName());
            draft.setConfig(config);
            draft.commit();
            CacheManager.getUsageHistory(KubernetesCluster.class).push(draft);
        });
    }
}
