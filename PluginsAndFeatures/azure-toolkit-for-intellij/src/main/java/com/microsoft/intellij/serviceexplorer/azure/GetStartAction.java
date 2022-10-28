/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer.azure;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.ide.guidance.action.ShowGettingStartAction;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class GetStartAction extends NodeAction {
    private static boolean isActionTriggered = false;
    private static final String PLACE = "AzureExplorerNode";

    public GetStartAction(@Nonnull AzureModule azureModule) {
        super(azureModule, "Getting Started");
        addListener(new NodeActionListener() {
            @Override
            @AzureOperation(name = "guidance.show_courses_view", type = AzureOperation.Type.ACTION)
            protected void actionPerformed(NodeActionEvent e) {
                if (!isActionTriggered) {
                    isActionTriggered = true;
                    AzureStoreManager.getInstance().getIdeStore().setProperty(ShowGettingStartAction.GUIDANCE, ShowGettingStartAction.IS_ACTION_TRIGGERED, String.valueOf(true));
                }
                OperationContext.action().setTelemetryProperty("FromPlace", PLACE);
                OperationContext.action().setTelemetryProperty("ShowBlueIcon", String.valueOf(isActionTriggered));
                GuidanceViewManager.getInstance().showCoursesView((Project) Objects.requireNonNull(azureModule.getProject()));
            }
        });
    }

    @Override
    public AzureIcon getIconSymbol() {
        if (!isActionTriggered) {
            final String isActionTriggerVal = AzureStoreManager.getInstance().getIdeStore().getProperty(ShowGettingStartAction.GUIDANCE, ShowGettingStartAction.IS_ACTION_TRIGGERED);
            isActionTriggered = Optional.ofNullable(isActionTriggerVal).map(Boolean::parseBoolean).orElse(false);
        }
        return isActionTriggered ? AzureIcons.Common.GET_START : AzureIcons.Common.GET_START_NEW;
    }
}
