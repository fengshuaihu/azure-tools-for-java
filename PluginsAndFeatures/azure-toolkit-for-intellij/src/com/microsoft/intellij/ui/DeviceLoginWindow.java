/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.microsoft.aad.adal4j.AdalErrorCode;
import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DeviceLoginWindow extends AzureDialogWrapper {
    private static final String TITLE = "Azure Device Login";
    private JPanel panel;
    private JEditorPane editorPanel;
    private AuthenticationResult authenticationResult = null;
    private Future<?> authExecutor;
    private final DeviceCode deviceCode;

    public AuthenticationResult getAuthenticationResult() {
        return authenticationResult;
    }

    public DeviceLoginWindow(final AuthenticationContext ctx, final DeviceCode deviceCode,
                             final AuthenticationCallback<AuthenticationResult> callBack) {
        super(null, false, IdeModalityType.PROJECT);
        super.setOKButtonText("Copy&&Open");
        this.deviceCode = deviceCode;
        setModal(true);
        setTitle(TITLE);
        editorPanel.setBackground(panel.getBackground());
        editorPanel.setText(createHtmlFormatMessage());
        editorPanel.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.open(e.getURL().toString());
            }
        });
        editorPanel.setFocusable(false);
        // Apply JLabel's font and color to JEditorPane
        final Font font = UIManager.getFont("Label.font");
        final Color foregroundColor = UIManager.getColor("Label.foreground");
        editorPanel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        if (font != null && foregroundColor != null) {
            editorPanel.setFont(font);
            editorPanel.setForeground(foregroundColor);
        }

        authExecutor = ApplicationManager.getApplication()
            .executeOnPooledThread(() -> pullAuthenticationResult(ctx, deviceCode, callBack));
        init();
    }

    private void pullAuthenticationResult(final AuthenticationContext ctx, final DeviceCode deviceCode,
                                          final AuthenticationCallback<AuthenticationResult> callback) {
        final long interval = deviceCode.getInterval();
        long remaining = deviceCode.getExpiresIn();
        // Close adal logger for it will write useless error log
        // for issue #2368 https://github.com/Microsoft/azure-tools-for-java/issues/2368
        Logger authLogger = Logger.getLogger(AuthenticationContext.class);
        Level authLoggerLevel = authLogger.getLevel();
        authLogger.setLevel(Level.OFF);
        try {
            while (remaining > 0 && authenticationResult == null) {
                try {
                    remaining -= interval;
                    Thread.sleep(interval * 1000);
                    authenticationResult = ctx.acquireTokenByDeviceCode(deviceCode, callback).get();
                } catch (ExecutionException | InterruptedException e) {
                    if (e.getCause() instanceof AuthenticationException &&
                        ((AuthenticationException) e.getCause()).getErrorCode() == AdalErrorCode.AUTHORIZATION_PENDING) {
                        // swallow the pending exception
                    } else {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        } finally {
            authLogger.setLevel(authLoggerLevel);
        }
        closeDialog();
    }

    private String createHtmlFormatMessage() {
        final String verificationUrl = deviceCode.getVerificationUrl();
        return "<p>"
            + deviceCode.getMessage().replace(verificationUrl, String.format("<a href=\"%s\">%s</a>", verificationUrl,
            verificationUrl))
            + "</p><p>Waiting for signing in with the code ...</p>";
    }

    @Override
    public void doCancelAction() {
        authExecutor.cancel(true);
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        final StringSelection selection = new StringSelection(deviceCode.getUserCode());
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        BrowserUtil.open(deviceCode.getVerificationUrl());
    }

    private void closeDialog() {
        ApplicationManager.getApplication().invokeLater(() -> {
            final Window w = getWindow();
            w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
        }, ModalityState.stateForComponent(panel));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }
}
