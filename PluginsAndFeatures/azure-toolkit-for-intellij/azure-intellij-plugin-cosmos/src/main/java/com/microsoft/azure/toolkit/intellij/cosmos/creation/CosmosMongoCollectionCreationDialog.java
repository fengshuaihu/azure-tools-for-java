/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cosmos.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.cosmos.model.ThroughputConfig;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCollectionDraft;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class CosmosMongoCollectionCreationDialog extends AzureDialog<MongoCollectionDraft.MongoCollectionConfig> implements AzureForm<MongoCollectionDraft.MongoCollectionConfig> {
    private AzureTextInput txtCollectionId;
    private JRadioButton rdoUnsharded;
    private JRadioButton rdoSharded;
    private JCheckBox chkDedicatedThroughput;
    private ThroughputConfigPanel pnlThroughput;
    private AzureTextInput txtShardKey;
    private JPanel pnlRoot;
    private JLabel lblSharedKey;
    private JLabel lblCollectionId;

    private final Project project;

    public CosmosMongoCollectionCreationDialog(Project project, MongoDatabase database) {
        super(project);
        this.project = project;
        init();
    }

    @Override
    protected void init() {
        super.init();
        txtCollectionId.setRequired(true);
        txtCollectionId.addValidator(this::validateCollectionId);
        pnlThroughput.setVisible(chkDedicatedThroughput.isSelected());
        chkDedicatedThroughput.addItemListener(e -> pnlThroughput.setVisible(chkDedicatedThroughput.isSelected()));

        final ButtonGroup group = new ButtonGroup();
        group.add(rdoUnsharded);
        group.add(rdoSharded);
        rdoSharded.addItemListener(e -> toggleSharedKey(rdoSharded.isSelected()));
        rdoUnsharded.addItemListener(e -> toggleSharedKey(rdoSharded.isSelected()));

        lblSharedKey.setLabelFor(txtShardKey);
        lblCollectionId.setLabelFor(txtCollectionId);
    }

    private AzureValidationInfo validateCollectionId() {
        final String value = txtCollectionId.getValue();
        return StringUtils.endsWith(value, StringUtils.SPACE) || StringUtils.containsAny(value, "\\", "/", "#", "?", "%") ?
                AzureValidationInfo.error("Collection Id should not end with space nor contain characters '\\', '/', '#', '?', '%'", txtCollectionId) :
                AzureValidationInfo.success(txtCollectionId);
    }

    @Override
    public AzureForm<MongoCollectionDraft.MongoCollectionConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Mongo Collection";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    private void toggleSharedKey(boolean enableSharedKey) {
        lblSharedKey.setVisible(enableSharedKey);
        txtShardKey.setVisible(enableSharedKey);
        txtShardKey.setRequired(enableSharedKey);
        txtShardKey.validateValueAsync();
    }

    @Override
    public MongoCollectionDraft.MongoCollectionConfig getValue() {
        final ThroughputConfig throughput = pnlThroughput.getValue();
        final MongoCollectionDraft.MongoCollectionConfig result = new MongoCollectionDraft.MongoCollectionConfig();
        result.setCollectionId(txtCollectionId.getValue());
        if (rdoSharded.isSelected()) {
            result.setShardKey(txtShardKey.getValue());
        }
        if (chkDedicatedThroughput.isSelected()) {
            result.setThroughput(throughput.getThroughput());
            result.setMaxThroughput(throughput.getMaxThroughput());
        }
        return result;
    }

    @Override
    public void setValue(MongoCollectionDraft.MongoCollectionConfig data) {
        txtCollectionId.setValue(data.getCollectionId());
        if (data.getShardKey() != null) {
            rdoSharded.setSelected(true);
            txtShardKey.setValue(data.getShardKey());
        } else {
            rdoUnsharded.setSelected(true);
        }
        if (ObjectUtils.anyNotNull(data.getThroughput(), data.getMaxThroughput())) {
            chkDedicatedThroughput.setSelected(true);
            pnlThroughput.setValue(data);
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtCollectionId, txtShardKey, pnlThroughput);
    }
}
