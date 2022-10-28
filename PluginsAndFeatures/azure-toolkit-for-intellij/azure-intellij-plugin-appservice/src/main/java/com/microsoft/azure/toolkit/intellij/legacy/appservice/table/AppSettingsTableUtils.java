/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.table;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.ToolbarDecorator;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.nimbusds.jose.util.ArrayUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class AppSettingsTableUtils {
    public static JPanel createAppSettingPanel(@Nonnull final AppSettingsTable appSettingsTable, AnActionButton... additionalActions) {
        final AnActionButton btnAdd = new AnActionButton(message("common.add"), AllIcons.General.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                appSettingsTable.addAppSettings();
            }
        };
        btnAdd.registerCustomShortcutSet(KeyEvent.VK_ADD, InputEvent.ALT_DOWN_MASK, appSettingsTable);

        final AnActionButton btnRemove = new AnActionButton(message("common.remove"), AllIcons.General.Remove) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                appSettingsTable.removeAppSettings();
            }
        };
        btnRemove.registerCustomShortcutSet(KeyEvent.VK_SUBTRACT, InputEvent.ALT_DOWN_MASK, appSettingsTable);
        final AnActionButton[] actionButtons = {btnAdd, btnRemove};
        final ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(appSettingsTable)
                .addExtraActions(ArrayUtils.concat(actionButtons, additionalActions))
                .setMinimumSize(new Dimension(-1, 120))
                .setToolbarPosition(ActionToolbarPosition.TOP);
        final SearchTextField searchTextField = new SearchTextField();
        searchTextField.getTextEditor().addActionListener(e -> appSettingsTable.filter(e.getActionCommand()));
        searchTextField.addDocumentListener((TextDocumentListenerAdapter) () -> {
            final String stringToFilter = searchTextField.getText();
            appSettingsTable.filter(stringToFilter);
            AzureTaskManager.getInstance().runLater(() -> {
                if (appSettingsTable.getRowCount() <= 0) {
                    appSettingsTable.getEmptyText().setText("No app settings found");
                }
            }, AzureTask.Modality.ANY);
        });
        appSettingsTable.setDefaultRenderer(Object.class, new HighLightedCellRenderer(searchTextField.getTextEditor()));
        final JPanel panel = tableToolbarDecorator.createPanel();
        tableToolbarDecorator.getActionsPanel().add(searchTextField, BorderLayout.WEST);
        return panel;
    }
}
