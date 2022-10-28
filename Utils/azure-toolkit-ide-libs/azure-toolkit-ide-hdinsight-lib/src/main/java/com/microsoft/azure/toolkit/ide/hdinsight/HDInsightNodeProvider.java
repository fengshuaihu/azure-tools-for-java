package com.microsoft.azure.toolkit.ide.hdinsight;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HDInsightNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "HDInsight";
    private static final String ICON = AzureIcons.HDInsight.MODULE.getIconPath();

    // GET ROOT

    @Override
    public boolean accept(@NotNull Object data, @Nullable Node<?> parent, ViewType type) {
        return false;
    }

    @Nullable
    @Override
    public Node<?> createNode(@NotNull Object data, @Nullable Node<?> parent, @NotNull IExplorerNodeProvider.Manager manager) {
        return null;
    }
}
