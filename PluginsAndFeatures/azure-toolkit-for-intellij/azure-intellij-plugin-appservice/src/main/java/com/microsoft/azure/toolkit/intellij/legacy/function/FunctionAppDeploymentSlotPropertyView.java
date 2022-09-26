/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppBasePropertyView;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlotDraft;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

public class FunctionAppDeploymentSlotPropertyView extends WebAppBasePropertyView {
    private static final String ID = "com.microsoft.azure.toolkit.intellij.function.FunctionAppDeploymentSlotPropertyView";

    public static WebAppBasePropertyView create(@Nonnull final Project project, @Nonnull final String resourceId, @Nonnull final VirtualFile virtualFile) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final FunctionAppDeploymentSlotPropertyView view = new FunctionAppDeploymentSlotPropertyView(project, id, virtualFile);
        view.onLoadWebAppProperty(id.subscriptionId(), id.parent().id(), id.name());
        return view;
    }

    protected FunctionAppDeploymentSlotPropertyView(@Nonnull Project project, @Nonnull ResourceId id, @Nonnull final VirtualFile virtualFile) {
        super(project, id.subscriptionId(), id.parent().id(), id.name(), virtualFile);
    }

    @Override
    protected String getId() {
        return ID;
    }

    @Override
    protected WebAppBasePropertyViewPresenter createPresenter() {
        return new WebAppBasePropertyViewPresenter() {
            @Override
            protected FunctionAppDeploymentSlot getWebAppBase(String subscriptionId, String functionAppId, String name) {
                return Azure.az(AzureFunctions.class).functionApp(functionAppId).slots().get(name, null);
            }

            @Override
            protected void updateAppSettings(String subscriptionId, String functionAppId, String name, Map toUpdate, Set toRemove) {
                final FunctionAppDeploymentSlot functionApp = getWebAppBase(subscriptionId, functionAppId, name);
                final FunctionAppDeploymentSlotDraft draft = (FunctionAppDeploymentSlotDraft) functionApp.update();
                draft.setAppSettings(toUpdate);
                toRemove.forEach(key -> draft.removeAppSetting((String) key));
                draft.updateIfExist();
            }
        };
    }
}
