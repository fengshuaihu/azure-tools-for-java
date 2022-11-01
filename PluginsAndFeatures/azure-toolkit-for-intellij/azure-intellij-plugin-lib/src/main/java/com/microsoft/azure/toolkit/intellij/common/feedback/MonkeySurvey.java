/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFileType;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class MonkeySurvey {
    public static final Key<String> SURVEY_URL = Key.create("survey_url");
    public static final Key<Integer> ORIGINAL_CORE = Key.create("original_core");
    @Nonnull
    private static final String url = "https://www.surveymonkey.com/r/PNB5NBL?mode=simple";

    public static void openInIDE(@Nonnull Project project) {
        openInIDE(project, -1);
    }

    public static void openInIDE(@Nonnull Project project, int score) {
        final FileEditorManager manager = FileEditorManager.getInstance(project);
        if (manager == null) {
            return;
        }
        LightVirtualFile survey = searchExistingFile(manager);
        if (survey == null) {
            final String title = "Provide Feedback";
            survey = new LightVirtualFile(title);
            survey.putUserData(SURVEY_URL, url);
            survey.putUserData(ORIGINAL_CORE, score);
            survey.setFileType(new AzureFileType(MonkeySurveyEditorProvider.TYPE, IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE)));
        }
        final LightVirtualFile finalItemVirtualFile = survey;
        AzureTaskManager.getInstance().runLater(() -> manager.openFile(finalItemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/));
    }

    private static LightVirtualFile searchExistingFile(FileEditorManager fileEditorManager) {
        LightVirtualFile virtualFile = null;
        for (final VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            final String surveyUrl = editedFile.getUserData(SURVEY_URL);
            if (surveyUrl != null && surveyUrl.equals(url)) {
                virtualFile = (LightVirtualFile) editedFile;
                break;
            }
        }
        return virtualFile;
    }

    /**
     * @param score 1, 2, 3, 4, 5
     */
    public static CompletableFuture<HttpResponse<String>> send(int score) {
        final String json = buildRequestBody(score);
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.surveymonkey.com/v3/collectors/445843454/responses"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer CMT0R-q0dcs6i9ZZ0fGmpA4KK-w9Adi5g6vrFoaC-82k.iRq50DhM5VkjVn8ArITMPRtikm9SBWAlUxAQJwnYdY9xwTxT-DYN4SyKN938PwE37hxGQhMSP4tJNTeVr3T")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    @Nonnull
    private static String buildRequestBody(int score) {
        return "{" +
            "  \"pages\": [" +
            "    {" +
            "      \"id\": \"27440878\"," +
            "      \"questions\": [" +
            "        {" +
            "          \"id\": \"72189355\"," +
            "          \"answers\": [" +
            "            {" +
            "              \"choice_id\": \"58129617" + score + "\"," +
            "              \"row_id\": \"581296170\"" +
            "            }" +
            "          ]" +
            "        }," +
            "        {" +
            "          \"id\": \"70321780\"," +
            "          \"answers\": [" +
            "            {" +
            "              \"text\": \"" + score + "\"" +
            "            }" +
            "          ]" +
            "        }," +
            "        {" +
            "          \"id\": \"70321940\"," +
            "          \"answers\": [" +
            "            {" +
            "              \"text\": \"" + InstallationIdUtils.getHashMac() + "@AzureToolkitForIntelliJ\"" +
            "            }" +
            "          ]" +
            "        }" +
            "      ]" +
            "    }" +
            "  ]" +
            "}";
    }
}