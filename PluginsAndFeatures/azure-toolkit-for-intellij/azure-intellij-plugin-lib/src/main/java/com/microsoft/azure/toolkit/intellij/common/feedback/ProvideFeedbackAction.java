/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ProvideFeedbackAction extends AnAction implements DumbAware {
    public static final Key<String> ID = new Key<>("ProvideFeedbackAction");

    @Override
    @AzureOperation(name = "feedback.open_monkey_survey", type = AzureOperation.Type.ACTION)
    public void actionPerformed(@Nonnull AnActionEvent event) {
        MonkeySurvey.openInIDE(Objects.requireNonNull(event.getProject()));
    }
}
