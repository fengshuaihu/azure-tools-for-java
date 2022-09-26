/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.azure.resourcemanager.appservice.models.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import lombok.Data;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class WebAppSettingModel {

    // common settings
    private boolean creatingNew = false;
    private String subscriptionId = "";
    // deploy related
    private String webAppId = "";
    private String targetPath = "";
    private String targetName = "";
    private String projectBase = "";
    private String projectType = "";
    private boolean deployToRoot = true;
    private boolean deployToSlot = false;
    private String slotName;
    private String newSlotName;
    private String newSlotConfigurationSource;
    // create related
    private String webAppName = "";
    private String resourceGroup = "";
    private boolean creatingAppServicePlan = false;
    private String appServicePlanName = "";
    private String appServicePlanResourceGroupName = "";
    private String region = "";
    private String pricing = "";

    // todo: change string values to app service library model
    private String operatingSystem;
    private String webAppContainer;
    private String webAppJavaVersion;

    // web server log
    private boolean enableWebServerLogging = false;
    private Integer webServerLogQuota = 35;
    private Integer webServerRetentionPeriod = null;
    private boolean enableDetailedErrorMessage = false;
    private boolean enableFailedRequestTracing = false;
    // application log
    private boolean enableApplicationLog = false;
    private String applicationLogLevel = LogLevel.ERROR.toString();

    @Nullable
    public Runtime getRuntime() {
        if (StringUtils.isAllEmpty(operatingSystem, webAppContainer, webAppJavaVersion)) {
            return null;
        }
        final OperatingSystem system =
                OperatingSystem.fromString(operatingSystem);
        final WebContainer container = WebContainer.fromString(webAppContainer);
        final JavaVersion javaVersion =
                JavaVersion.fromString(webAppJavaVersion);
        return Runtime.getRuntime(system, container, javaVersion);
    }

    public void saveRuntime(Runtime runtime) {
        this.operatingSystem = Optional.ofNullable(runtime).map(Runtime::getOperatingSystem).map(OperatingSystem::getValue).orElse(null);
        this.webAppContainer = Optional.ofNullable(runtime).map(Runtime::getWebContainer).map(WebContainer::getValue).orElse(null);
        this.webAppJavaVersion = Optional.ofNullable(runtime).map(Runtime::getJavaVersion).map(JavaVersion::getValue).orElse(null);
    }

    public Map<String, String> getTelemetryProperties(Map<String, String> properties) {
        final Map<String, String> result = new HashMap<>();
        try {
            if (properties != null) {
                result.putAll(properties);
            }
            final Runtime runtime = getRuntime();
            final String osValue = Optional.ofNullable(runtime.getOperatingSystem())
                    .map(OperatingSystem::toString).orElse(StringUtils.EMPTY);
            final String webContainerValue = Optional.ofNullable(runtime.getWebContainer()).map(WebContainer::getValue).orElse(StringUtils.EMPTY);
            final String javaVersionValue = Optional.ofNullable(runtime.getJavaVersion())
                    .map(JavaVersion::getValue).orElse(StringUtils.EMPTY);
            result.put(TelemetryConstants.RUNTIME, String.format("%s-%s-%s", osValue, webContainerValue, javaVersionValue));
            result.put(TelemetryConstants.WEBAPP_DEPLOY_TO_SLOT, String.valueOf(isDeployToSlot()));
            result.put(TelemetryConstants.SUBSCRIPTIONID, getSubscriptionId());
            result.put(TelemetryConstants.CREATE_NEWWEBAPP, String.valueOf(isCreatingNew()));
            result.put(TelemetryConstants.CREATE_NEWASP, String.valueOf(isCreatingAppServicePlan()));
            result.put(TelemetryConstants.FILETYPE, FilenameUtils.getExtension(getTargetName()));
            result.put(TelemetryConstants.PRICING_TIER, pricing);
            result.put(TelemetryConstants.REGION, region);
        } catch (final Exception ignore) {
        }
        return result;
    }
}
