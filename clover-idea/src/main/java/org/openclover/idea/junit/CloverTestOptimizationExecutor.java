package org.openclover.idea.junit;

import com.intellij.execution.executors.DefaultRunExecutor;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.util.ui.CloverIcons;

import javax.swing.Icon;

public class CloverTestOptimizationExecutor extends DefaultRunExecutor {
    public static final String EXECUTOR_ID = "OpenClover Test Optimization";

    @NotNull
    @Override
    public Icon getIcon() {
        return CloverIcons.CLOVERIZED_RUN;
    }

    @Override
    public Icon getToolWindowIcon() {
        return CloverIcons.CLOVERIZED_RUN;
    }

    @NotNull
    @Override
    public String getActionName() {
        return "Run optimized";
    }

    @NotNull
    @Override
    public String getId() {
        return EXECUTOR_ID;
    }

    @NotNull
    @Override
    public String getStartActionText() {
        return "Run Optimized";
    }

    @Override
    public String getDescription() {
        return "Run selected tests optimized with OpenClover";
    }

    @Override
    public String getContextActionId() {
        return "RunClassOptimized";
    }
}
