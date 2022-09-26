/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.helpers;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheNode;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public interface UIHelper {
    void showException(@NotNull String message,
            Throwable ex,
            @NotNull String title,
            boolean appendEx,
            boolean suggestDetail);

    void showError(@NotNull String message, @NotNull String title);

    default void showError(Component component, @NotNull String message, @NotNull String title) {
        JOptionPane.showMessageDialog(component, message, title, JOptionPane.ERROR_MESSAGE);
    }

    boolean showConfirmation(@NotNull String message, @NotNull String title, @NotNull String[] options, String defaultOption);

    default boolean showConfirmation(@NotNull Component component, @NotNull String message, @NotNull String title, @NotNull String[] options,
                                     String defaultOption) {
        return showConfirmation(message, title, options, defaultOption);
    }

    void showInfo(@NotNull Node node, @NotNull String message);

    void showError(@NotNull Node node, @NotNull String message);

    void logError(String message, Throwable ex);

    File showFileChooser(String title);

    File showFileSaver(String title, String fileName);

    void openRedisPropertyView(@NotNull RedisCacheNode node);

    void openRedisExplorer(@NotNull RedisCacheNode node);

    void openInBrowser(String link);

    void openContainerRegistryPropertyView(@NotNull ContainerRegistryNode node);

    default void openMySQLPropertyView(@NotNull String id, @NotNull Object project) {

    }

    default void openSqlServerPropertyView(@NotNull String id, @NotNull Object project) {

    }

    boolean isDarkTheme();

    default void closeSpringCloudAppPropertyView(@NotNull Object project, @NotNull String appId) {

    }

    default void showMessageDialog(Component component, String message, String title, Icon icon) {
        JOptionPane.showMessageDialog(component, message, title, JOptionPane.INFORMATION_MESSAGE, icon);
    }

    default int showConfirmDialog(Component component, String message, String title, String[] options, String defaultOption, Icon icon) {
        return JOptionPane.showOptionDialog(component,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                icon,
                options,
                defaultOption);
    }

    default boolean showYesNoDialog(Component component, String message, String title, Icon icon) {
        return JOptionPane.showConfirmDialog(
                component,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                icon) == JOptionPane.YES_OPTION;
    }

    default String showInputDialog(Component component, String message, String title, Icon icon) {
        return (String) JOptionPane.showInputDialog(component, message, title, JOptionPane.QUESTION_MESSAGE, icon, null, null);
    }

    default void showInfoNotification(String title, String message) {

    }

    default void showErrorNotification(String title, String message) {

    }

    default void showWarningNotification(String title, String message) {

    }

}
