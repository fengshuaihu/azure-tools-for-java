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
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainerDraft;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CosmosSQLContainerCreationDialog extends AzureDialog<SqlContainerDraft.SqlContainerConfig> implements AzureForm<SqlContainerDraft.SqlContainerConfig> {
    private JCheckBox chkDedicatedThroughput;
    private ThroughputConfigPanel pnlThroughput;
    private AzureTextInput txtPartitionKey;
    private AzureTextInput txtContainerId;
    private JPanel pnlRoot;
    private JLabel lblContainerId;
    private JLabel lblPartitionKey;

    private final Project project;

    public CosmosSQLContainerCreationDialog(Project project, SqlDatabase database) {
        super(project);
        this.project = project;
        init();
    }

    @Override
    protected void init() {
        super.init();
        txtContainerId.setRequired(true);
        txtContainerId.addValidator(this::validateContainerId);
        txtPartitionKey.setRequired(true);
        pnlThroughput.setVisible(chkDedicatedThroughput.isSelected());
        chkDedicatedThroughput.addItemListener(e -> pnlThroughput.setVisible(chkDedicatedThroughput.isSelected()));

        lblPartitionKey.setLabelFor(txtPartitionKey);
        lblContainerId.setLabelFor(txtContainerId);
    }

    private AzureValidationInfo validateContainerId() {
        final String value = txtContainerId.getValue();
        return StringUtils.endsWith(value, StringUtils.SPACE) || StringUtils.containsAny(value, "\\", "/", "#", "?", "%") ?
                AzureValidationInfo.error("Container Id should not end with space nor contain characters '\\', '/', '#', '?', '%'", txtContainerId) :
                AzureValidationInfo.success(txtContainerId);
    }

    @Override
    public AzureForm<SqlContainerDraft.SqlContainerConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create SQL Container";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public SqlContainerDraft.SqlContainerConfig getValue() {
        final ThroughputConfig throughput = this.pnlThroughput.getValue();
        final SqlContainerDraft.SqlContainerConfig result = new SqlContainerDraft.SqlContainerConfig();
        result.setContainerId(txtContainerId.getValue());
        result.setPartitionKey(txtPartitionKey.getValue());
        if (chkDedicatedThroughput.isSelected()) {
            result.setThroughput(throughput.getThroughput());
            result.setMaxThroughput(throughput.getMaxThroughput());
        }
        return result;
    }

    @Override
    public void setValue(SqlContainerDraft.SqlContainerConfig data) {
        this.txtContainerId.setValue(data.getContainerId());
        Optional.ofNullable(data.getPartitionKey()).ifPresent(value -> this.txtPartitionKey.setValue(StringUtils.startsWith(value, "/") ? value : "/" + value));
        this.pnlThroughput.setValue(data);
        this.chkDedicatedThroughput.setSelected(ObjectUtils.anyNotNull(data.getThroughput(), data.getMaxThroughput()));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtContainerId, txtPartitionKey, pnlThroughput);
    }
}
