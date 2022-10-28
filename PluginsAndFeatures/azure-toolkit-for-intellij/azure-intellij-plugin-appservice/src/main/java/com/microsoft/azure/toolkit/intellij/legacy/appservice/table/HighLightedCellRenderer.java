/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.table;

import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HighLightedCellRenderer extends ColoredTableCellRenderer {
    private final JTextField searchField;

    public HighLightedCellRenderer(JTextField searchField) {
        super();
        this.searchField = searchField;
    }

    @Override
    protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
        final String textToMatch = this.searchField.getText().toLowerCase();
        final String labelText = String.valueOf(value);
        if (StringUtils.isEmpty(textToMatch)) {
            this.append(labelText);
            return;
        }
        int beginIndex = 0, endIndex;
        while (beginIndex < labelText.length()) {
            endIndex = labelText.toLowerCase().indexOf(textToMatch, beginIndex);
            endIndex = endIndex < 0 ? labelText.length() : endIndex;
            this.append(labelText.substring(beginIndex, endIndex));
            if (endIndex != labelText.length()) {
                this.append(labelText.substring(endIndex, endIndex + textToMatch.length()), new SimpleTextAttributes(SimpleTextAttributes.STYLE_SEARCH_MATCH, null));
            }
            beginIndex = endIndex + textToMatch.length();
        }
    }
}

