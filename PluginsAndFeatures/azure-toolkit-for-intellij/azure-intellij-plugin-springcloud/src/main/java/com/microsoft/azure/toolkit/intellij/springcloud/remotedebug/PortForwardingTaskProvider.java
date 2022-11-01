/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.Key;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.springcloud.portforwarder.SpringPortForwarder;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PortForwardingTaskProvider extends BeforeRunTaskProvider<PortForwardingTaskProvider.PortForwarderBeforeRunTask> {
    private static final String NAME_TEMPLATE = "Attach to %s";
    private static final Key<PortForwarderBeforeRunTask> ID = Key.create("PortForwardingTaskProviderId");
    private static final Icon ICON = IntelliJAzureIcons.getIcon(AzureIcons.Action.REMOTE_DEBUG);
    @Getter
    public Key<PortForwarderBeforeRunTask> id = ID;
    @Getter
    public String name = String.format(NAME_TEMPLATE, "Spring App Instance");
    @Getter
    public Icon icon = ICON;

    @Override
    public @Nullable
    Icon getTaskIcon(PortForwarderBeforeRunTask task) {
        return ICON;
    }

    @Nullable
    @Override
    public PortForwarderBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
        return new PortForwarderBeforeRunTask(runConfiguration);
    }

    @Override
    public boolean executeTask(@NotNull DataContext context, @NotNull RunConfiguration configuration, @NotNull ExecutionEnvironment environment, @Nonnull PortForwarderBeforeRunTask task) {
        if (configuration instanceof RemoteConfiguration) {
            return task.startPortForwarding(Integer.parseInt(((RemoteConfiguration) configuration).PORT));
        }
        return false;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription(PortForwarderBeforeRunTask task) {
        return Objects.isNull(task.appInstance) ? name : String.format(NAME_TEMPLATE, task.appInstance.getName());
    }

    @Getter
    @Setter
    public static class PortForwarderBeforeRunTask extends BeforeRunTask<PortForwarderBeforeRunTask> implements PersistentStateComponent<PortForwarderBeforeRunTaskState> {
        private PortForwarderBeforeRunTaskState state = new PortForwarderBeforeRunTaskState();
        @Nullable
        private SpringPortForwarder forwarder;
        private RunConfiguration config;
        private SpringCloudAppInstance appInstance;

        protected PortForwarderBeforeRunTask(RunConfiguration config) {
            super(ID);
            this.config = config;
        }

        public boolean startPortForwarding(int localPort) {
            final String resourceId = this.state.properties.get(PortForwarderBeforeRunTaskState.RESOURCE_ID);
            this.appInstance = (SpringCloudAppInstance) Azure.az().getById(resourceId);
            if (this.config instanceof RemoteConfiguration && Objects.nonNull(appInstance)) {
                this.forwarder = new SpringPortForwarder(appInstance);
                AzureTaskManager.getInstance().runOnPooledThread(() ->  this.forwarder.startForward(localPort));
                return true;
            }
            return false;
        }

        public void setAppInstance(SpringCloudAppInstance appInstance) {
            this.appInstance = appInstance;
            this.state.properties.put(PortForwarderBeforeRunTaskState.RESOURCE_ID, appInstance.getId());
        }

        @Nullable
        @Override
        public PortForwarderBeforeRunTaskState getState() {
            return this.state;
        }

        @Override
        public void loadState(@Nonnull PortForwarderBeforeRunTaskState state) {
            XmlSerializerUtil.copyBean(state, this.state);
        }
    }

    @Getter
    @Setter
    static class PortForwarderBeforeRunTaskState {
        private Map<String, String> properties = new HashMap<>();
        private final static String RESOURCE_ID = "resourceId";
    }
}
