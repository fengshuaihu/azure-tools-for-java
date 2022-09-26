/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.ide.appservice.model.DeploymentSlotConfig;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.function.FunctionAppComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTable;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.table.FunctionAppSettingsTableUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui.components.DeploymentSlotComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;


public class FunctionDeploymentPanel extends AzureSettingPanel<FunctionDeployConfiguration> {

    private JPanel pnlRoot;
    private HyperlinkLabel lblCreateFunctionApp;
    private JPanel pnlAppSettings;
    private JComboBox<Module> cbFunctionModule;
    private FunctionAppComboBox functionAppComboBox;
    private JLabel lblModule;
    private JLabel lblFunction;
    private JLabel lblAppSettings;
    private JCheckBox chkSlot;
    private DeploymentSlotComboBox cbDeploymentSlot;
    private JLabel lblDeploymentSlot;
    private FunctionAppSettingsTable appSettingsTable;
    private String appSettingsKey;
    private String appSettingsResourceId;
    private Module previousModule = null;

    public FunctionDeploymentPanel(@NotNull Project project, @NotNull FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.appSettingsKey = StringUtils.firstNonBlank(functionDeployConfiguration.getAppSettingsKey(), UUID.randomUUID().toString());
        $$$setupUI$$$();

        cbFunctionModule.setRenderer(new ListCellRendererWrapper<>() {
            @Override
            public void customize(JList list, Module module, int i, boolean b, boolean b1) {
                if (module != null) {
                    setText(module.getName());
                    setIcon(AllIcons.Nodes.Module);
                }
            }
        });
        functionAppComboBox.setRequired(true);
        chkSlot.addItemListener(e -> onSelectSlot());

        lblModule.setLabelFor(cbFunctionModule);
        lblFunction.setLabelFor(functionAppComboBox);
        lblAppSettings.setLabelFor(appSettingsTable);
        lblDeploymentSlot.setLabelFor(cbDeploymentSlot);
        fillModules();
    }

    @NotNull
    @Override
    public String getPanelName() {
        return message("function.deploy.title");
    }

    @Override
    public void disposeEditor() {

    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlRoot;
    }

    @NotNull
    @Override
    protected JComboBox<Artifact> getCbArtifact() {
        return new ComboBox<>();
    }

    @NotNull
    @Override
    protected JLabel getLblArtifact() {
        return new JLabel();
    }

    @NotNull
    @Override
    protected JComboBox<MavenProject> getCbMavenProject() {
        return new ComboBox<>();
    }

    @NotNull
    @Override
    protected JLabel getLblMavenProject() {
        return new JLabel();
    }

    @Override
    protected void resetFromConfig(@NotNull FunctionDeployConfiguration configuration) {
        if (MapUtils.isNotEmpty(configuration.getAppSettings())) {
            this.appSettingsTable.setAppSettings(configuration.getAppSettings());
        }
        if (StringUtils.isNotEmpty(configuration.getAppSettingsKey())) {
            this.appSettingsKey = configuration.getAppSettingsKey();
        }
        Optional.ofNullable(configuration.getConfig()).ifPresent(config -> {
            this.functionAppComboBox.setValue(config);
            this.functionAppComboBox.setConfigModel(config);
            this.chkSlot.setSelected(config.getDeploymentSlot() != null);
            this.toggleDeploymentSlot(config.getDeploymentSlot() != null);
            this.appSettingsResourceId = StringUtils.isAllEmpty(config.getResourceId(), config.getName()) ? null :
                    getResourceId(config, config.getDeploymentSlot());
            Optional.ofNullable(config.getDeploymentSlot()).ifPresent(cbDeploymentSlot::setValue);
            Optional.ofNullable(config.getAppSettings()).ifPresent(appSettingsTable::setAppSettings);
        });
        this.previousModule = configuration.getModule();
        selectModule(previousModule);
    }

    @Override
    protected void apply(@NotNull FunctionDeployConfiguration configuration) {
        configuration.setAppSettingsKey(appSettingsKey);
        configuration.setAppSettings(appSettingsTable.getAppSettings());
        Optional.ofNullable((Module) cbFunctionModule.getSelectedItem()).ifPresent(configuration::saveTargetModule);
        Optional.ofNullable(functionAppComboBox.getValue())
                .map(value -> value.toBuilder()
                        .deploymentSlot(cbDeploymentSlot.getValue())
                        .appSettings(appSettingsTable.getAppSettings()).build())
                .ifPresent(configuration::saveConfig);
    }

    private void createUIComponents() {
        final String localSettingPath = Paths.get(project.getBasePath(), "local.settings.json").toString();
        appSettingsTable = new FunctionAppSettingsTable(localSettingPath);
        pnlAppSettings = FunctionAppSettingsTableUtils.createAppSettingPanel(appSettingsTable);

        functionAppComboBox = new FunctionAppComboBox(project);
        functionAppComboBox.addValueChangedListener(this::onSelectFunctionApp);
        functionAppComboBox.reloadItems();

        cbDeploymentSlot = new DeploymentSlotComboBox(project);
        cbDeploymentSlot.addValueChangedListener(this::onSelectFunctionSlot);
        cbDeploymentSlot.reloadItems();
    }

    private void onSelectFunctionSlot(final DeploymentSlotConfig value) {
        if (value == null) {
            return;
        }
        if (chkSlot.isSelected()) {
            if (value.isNewCreate()) {
                appSettingsTable.clear();
            } else {
                loadAppSettings(getResourceId(functionAppComboBox.getValue(), value));
            }
        }
    }

    private void onSelectFunctionApp(final FunctionAppConfig value) {
        if (value == null) {
            return;
        }
        // disable slot for draft function
        if (StringUtils.isEmpty(value.getResourceId())) {
            this.chkSlot.setSelected(false);
        }
        this.chkSlot.setEnabled(StringUtils.isNotEmpty(value.getResourceId()));
        this.toggleDeploymentSlot(chkSlot.isSelected());
        this.cbDeploymentSlot.setAppService(value.getResourceId());
        if (!this.chkSlot.isSelected()) {
            loadAppSettings(getResourceId(value, null));
        }
    }

    private void loadAppSettings(@Nonnull final String resourceId) {
        if (StringUtils.equalsIgnoreCase(resourceId, this.appSettingsResourceId) && MapUtils.isNotEmpty(this.appSettingsTable.getAppSettings())) {
            return;
        }
        this.appSettingsResourceId = resourceId;
        this.appSettingsTable.loadAppSettings(() -> {
            final AbstractAzResource<?, ?, ?> resource = Azure.az().getById(resourceId);
            return resource instanceof AppServiceAppBase<?, ?, ?> ? ((AppServiceAppBase<?, ?, ?>) resource).getAppSettings() : Collections.emptyMap();
        });
    }

    private void fillModules() {
        AzureTaskManager.getInstance()
                .runOnPooledThreadAsObservable(new AzureTask<>(() -> FunctionUtils.listFunctionModules(project)))
                .subscribe(modules -> AzureTaskManager.getInstance().runLater(() -> {
                    Arrays.stream(modules).forEach(cbFunctionModule::addItem);
                    selectModule(previousModule);
                }, AzureTask.Modality.ANY));
    }

    // todo: @hanli migrate to use AzureComboBox<Module>
    private void selectModule(final Module target) {
        if (target == null) {
            return;
        }
        for (int i = 0; i < cbFunctionModule.getItemCount(); i++) {
            final Module module = cbFunctionModule.getItemAt(i);
            if (Paths.get(module.getModuleFilePath()).equals(Paths.get(target.getModuleFilePath()))) {
                cbFunctionModule.setSelectedIndex(i);
                break;
            }
        }
    }

    private void onSelectSlot() {
        toggleDeploymentSlot(chkSlot.isSelected());
        if (!chkSlot.isSelected() && functionAppComboBox.getValue() != null) {
            // reload app settings for function app
            loadAppSettings(getResourceId(functionAppComboBox.getValue(), null));
        }
    }

    private void toggleDeploymentSlot(boolean isDeployToSlot) {
        cbDeploymentSlot.setEnabled(isDeployToSlot);
        cbDeploymentSlot.setRequired(isDeployToSlot);
        cbDeploymentSlot.validateValueAsync();
    }

    // todo: @hanli migrate to AzureFormInput
    public List<AzureValidationInfo> getAllValidationInfos(final boolean revalidateIfNone) {
        final List<AzureFormInput<?>> inputs = Arrays.asList(functionAppComboBox, cbDeploymentSlot);
        return inputs.stream()
                .map(input -> input.getValidationInfo(revalidateIfNone))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private String getResourceId(@Nonnull FunctionAppConfig config, @Nullable DeploymentSlotConfig slotConfig) {
        if (Objects.isNull(slotConfig)) {
            return StringUtils.isNoneBlank(config.getResourceId()) ? config.getResourceId() :
                    Azure.az(AzureFunctions.class).functionApps(config.getSubscriptionId()).getOrTemp(config.getName(), config.getResourceGroupName()).getId();
        } else {
            return Azure.az(AzureFunctions.class).functionApp(config.getResourceId()).slots().getOrTemp(slotConfig.getName(), null).getId();
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
