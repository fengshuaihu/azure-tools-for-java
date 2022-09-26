/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingsEditor;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui.FunctionDeploymentPanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FunctionDeploymentSettingEditor extends AzureSettingsEditor<FunctionDeployConfiguration> {

    private final FunctionDeploymentPanel mainPanel;
    private final FunctionDeployConfiguration functionDeployConfiguration;

    public FunctionDeploymentSettingEditor(Project project, @NotNull FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.mainPanel = new FunctionDeploymentPanel(project, functionDeployConfiguration);
        this.functionDeployConfiguration = functionDeployConfiguration;
    }

    @Override
    protected void applyEditorTo(@NotNull FunctionDeployConfiguration conf) throws ConfigurationException {
        super.applyEditorTo(conf);
        final AzureValidationInfo error = mainPanel.getAllValidationInfos(true).stream()
                .filter(i -> !i.isValid())
                .findAny().orElse(null);
        if (Objects.nonNull(error)) {
            final String message = error.getType() == AzureValidationInfo.Type.PENDING ? "Validating..." : error.getMessage();
            throw new ConfigurationException(message);
        }
    }

    @Override
    @NotNull
    protected AzureSettingPanel<FunctionDeployConfiguration> getPanel() {
        return this.mainPanel;
    }
}
