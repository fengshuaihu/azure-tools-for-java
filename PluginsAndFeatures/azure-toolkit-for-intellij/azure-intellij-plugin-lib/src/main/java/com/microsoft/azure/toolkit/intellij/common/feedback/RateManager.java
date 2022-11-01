/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.lib.common.operation.Operation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationListener;
import com.microsoft.azure.toolkit.lib.common.operation.OperationManager;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class RateManager {
    public static final String SERVICE = "rate";
    public static final String RATED_AT = "rated_at";
    public static final String RATED_SCORE = "rated_score";
    public static final String POPPED_AT = "popped_at";
    public static final String POPPED_TIMES = "popped_times";
    public static final String NEXT_POP_AFTER = "next_pop_after";
    public static final String TOTAL_SCORE = "action_total_score";

    private static final RateManager manager = new RateManager();
    private static final String SCORES_YML = "/com/microsoft/azure/toolkit/intellij/common/feedback/action-scores.yml";
    private static final Map<String, ScoreConfig> scores = loadScores();
    private static final AtomicInteger score = new AtomicInteger(0);
    private static final int THRESHOLD = 10;

    private static Map<String, ScoreConfig> loadScores() {
        final ObjectMapper YML_MAPPER = new YAMLMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (final InputStream inputStream = RateManager.class.getResourceAsStream(SCORES_YML)) {
            return YML_MAPPER.readValue(inputStream, new TypeReference<>() {
            });
        } catch (final IOException ignored) {
        }
        return Collections.emptyMap();
    }

    public static void addScore(int score) {
        final int total = RateManager.score.addAndGet(score);
        if (total > THRESHOLD && RatePopup.tryPopup(null)) {
            RateManager.score.set(THRESHOLD / 2);
        }
        AzureStoreManager.getInstance().getIdeStore().setProperty(SERVICE, TOTAL_SCORE, String.valueOf(total));
    }

    public static int getScore() {
        return RateManager.score.get();
    }

    public static void clearScore() {
        RateManager.score.set(0);
    }

    public static class WhenToPopup implements StartupActivity, OperationListener {
        @Override
        public void runActivity(@Nonnull Project project) {
            final char c = InstallationIdUtils.getHashMac().toLowerCase().charAt(0);
            if (Character.digit(c, 16) % 4 == 0) { // enabled for only 1/4
                OperationManager.getInstance().addListener(this);
            }
        }

        @Override
        public void afterReturning(Operation operation, Object source) {
            final ScoreConfig config = scores.get(operation.getId());
            if (config != null) {
                final String actionId = Optional.ofNullable(operation.getActionParent()).map(Operation::getId).orElse(null);
                if (ArrayUtils.isEmpty(config.getActions()) || Arrays.asList(config.getActions()).contains(actionId)) {
                    RateManager.addScore(config.getSuccess());
                }
            }
        }

        @Override
        public void afterThrowing(Throwable e, Operation operation, Object source) {
            final ScoreConfig config = scores.get(operation.getId());
            if (config != null) {
                final String actionId = Optional.ofNullable(operation.getActionParent()).map(Operation::getId).orElse(null);
                if (ArrayUtils.isEmpty(config.getActions()) || Arrays.asList(config.getActions()).contains(actionId)) {
                    RateManager.addScore(config.getFailure());
                }
            }
        }
    }

    @Data
    public static class ScoreConfig {
        private String[] actions;
        private int success;
        private int failure;
    }
}
