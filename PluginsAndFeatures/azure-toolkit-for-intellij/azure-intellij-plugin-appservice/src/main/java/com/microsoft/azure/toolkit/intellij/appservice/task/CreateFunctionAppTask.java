package com.microsoft.azure.toolkit.intellij.appservice.task;

import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.task.SignInTask;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionAppService;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask.APPINSIGHTS_INSTRUMENTATION_KEY;

public class CreateFunctionAppTask implements Task {
    public static final String FUNCTION_APP_NAME = "functionAppName";
    public static final String FUNCTION_ID = "functionId";
    public static final String DEFAULT_FUNCTION_APP_NAME = "defaultFunctionAppName";
    public static final String RESOURCE_GROUP = "resourceGroup";
    public static final String INSIGHTS_INSTRUMENT_KEY = "insightsInstrumentKey";
    private final ComponentContext context;

    public CreateFunctionAppTask(@Nonnull final ComponentContext context) {
        this.context = context;
        init();
    }

    @Override
    @AzureOperation(name = "guidance.create_function_app", type = AzureOperation.Type.SERVICE)
    public void execute() throws Exception {
        final String name = (String) Objects.requireNonNull(context.getParameter(FUNCTION_APP_NAME), "`name` is required to create function app");
        final Subscription subscription = Optional.ofNullable((String) context.getParameter(SignInTask.SUBSCRIPTION_ID))
                .map(id -> Azure.az(AzureAccount.class).account().getSubscription(id))
                .orElseThrow(() -> new AzureToolkitRuntimeException("Failed to get subscription to create function app"));
        final FunctionAppConfig functionAppConfig = FunctionAppConfig.getFunctionAppDefaultConfig(name);
        functionAppConfig.setName(name);
        functionAppConfig.setSubscription(subscription);
        functionAppConfig.setRuntime(Runtime.FUNCTION_WINDOWS_JAVA11);
        final FunctionAppBase<?, ?, ?> app =  FunctionAppService.getInstance().createOrUpdateFunctionApp(functionAppConfig);
        context.applyResult(FUNCTION_ID, app.getId());
        context.applyResult(RESOURCE_GROUP, app.getResourceGroupName());
        context.applyResult(INSIGHTS_INSTRUMENT_KEY, app.getAppSettings().get(APPINSIGHTS_INSTRUMENTATION_KEY));
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.function.create_app";
    }

    private void init() {
        final String defaultFunctionAppName = String.format("%s-%s", context.getCourse().getName(), Utils.getTimestamp());
        context.applyResult(DEFAULT_FUNCTION_APP_NAME, defaultFunctionAppName);
    }
}
