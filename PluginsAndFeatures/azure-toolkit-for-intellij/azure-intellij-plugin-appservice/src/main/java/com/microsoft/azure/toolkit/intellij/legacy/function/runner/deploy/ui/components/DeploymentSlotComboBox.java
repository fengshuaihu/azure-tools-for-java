/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.ide.appservice.model.DeploymentSlotConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.IDeploymentSlotModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DeploymentSlotComboBox extends AzureComboBox<DeploymentSlotConfig> {
    private final Project project;
    private final List<DeploymentSlotConfig> draftItems = new LinkedList<>();

    private String appServiceId;

    public DeploymentSlotComboBox(@Nullable final Project project) {
        super(true);
        this.project = project;
    }

    public void setAppService(final String appServiceId) {
        if (StringUtils.equals(this.appServiceId, appServiceId)) {
            return;
        }
        this.appServiceId = appServiceId;
        reloadItems();
    }

    @Override
    public void setValue(DeploymentSlotConfig val) {
        if (isDraftResource(val)) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val);
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        final List<ExtendableTextComponent.Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("Create (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension addEx = ExtendableTextComponent.Extension.create(AllIcons.General.Add, tooltip, this::createResource);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void createResource() {
        final List<DeploymentSlotConfig> existingSlots = this.getItems().stream()
                .filter(config -> StringUtils.isNoneBlank(config.getConfigurationSource()))
                .collect(Collectors.toList());
        final DeploymentSlotCreationDialog dialog = new DeploymentSlotCreationDialog(project, existingSlots);
        dialog.setOkActionListener(config -> {
            DeploymentSlotComboBox.this.setValue(config);
            AzureTaskManager.getInstance().runLater(dialog::close);
        });
        dialog.show();
    }

    @Nonnull
    @Override
    protected List<? extends DeploymentSlotConfig> loadItems() throws Exception {
        final AbstractAzResource<?, ?, ?> resource = StringUtils.isEmpty(appServiceId) ? null : Azure.az().getById(appServiceId);
        final IDeploymentSlotModule<?, ?, ?> module = Optional.ofNullable(resource)
                .map(r -> r.getSubModules().stream().filter(m -> m instanceof IDeploymentSlotModule).findFirst().orElse(null))
                .map(m -> (IDeploymentSlotModule<?, ?, ?>) m)
                .orElse(null);
        if (module == null) {
            return Collections.emptyList();
        }
        final List<DeploymentSlotConfig> result = module.list().stream().map(slot ->
                DeploymentSlotConfig.builder().name(slot.getName()).build()).collect(Collectors.toList());
        this.draftItems.stream().filter(config -> !result.contains(config)).forEach(result::add);
        return result;
    }

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof DeploymentSlotConfig) {
            final DeploymentSlotConfig selectedItem = (DeploymentSlotConfig) item;
            return isDraftResource(selectedItem) ? String.format("(New) %s", selectedItem.getName()) : selectedItem.getName();
        } else {
            return super.getItemText(item);
        }
    }

    private boolean isDraftResource(DeploymentSlotConfig val) {
        return StringUtils.isNotEmpty(val.getConfigurationSource()) || val.isNewCreate();
    }
}
