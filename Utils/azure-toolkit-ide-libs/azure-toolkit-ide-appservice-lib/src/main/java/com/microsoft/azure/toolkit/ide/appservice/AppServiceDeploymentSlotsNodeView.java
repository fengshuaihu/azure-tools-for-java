/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice;

import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AppServiceDeploymentSlotsNodeView implements NodeView {
    @Nonnull
    @Getter
    private final AppServiceAppBase<?, ?, ?> app;
    private final AzureEventBus.EventListener listener;

    @Nullable
    @Setter
    @Getter
    private NodeView.Refresher refresher;

    public AppServiceDeploymentSlotsNodeView(@Nonnull AppServiceAppBase<?, ?, ?> app) {
        this.app = app;
        this.listener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("resource.refreshed.resource", listener);
        AzureEventBus.on("appservice.slot.refresh", listener);
        this.refreshView();
    }

    @Override
    public String getLabel() {
        return "Deployment Slots";
    }

    @Override
    public String getIconPath() {
        return AzureIcons.WebApp.DEPLOYMENT_SLOT.getIconPath();
    }

    @Override
    public String getDescription() {
        return "Deployment Slots";
    }

    @Override
    public void dispose() {
        AzureEventBus.off("resource.refreshed.resource", listener);
        AzureEventBus.off("appservice.slot.refresh", listener);
        this.refresher = null;
    }

    public void onEvent(AzureEvent event) {
        final Object source = event.getSource();
        if (source instanceof AzResource && ((AzResource<?, ?, ?>) source).id().equals(this.app.getId())) {
            AzureTaskManager.getInstance().runLater(this::refreshChildren);
        }
    }
}
