package org.openclover.eclipse.testopt.actions;

import org.eclipse.debug.ui.actions.RelaunchLastAction;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.testopt.OptimizedLaunchingConstants;
import org.openclover.eclipse.testopt.TestOptimizationPluginMessages;

public class LastOptimizedLaunchAction extends RelaunchLastAction {
    @Override
    public String getMode() {
        return OptimizedLaunchingConstants.OPTIMIZED_MODE;
    }

    @Override
    public String getLaunchGroupId() {
        return OptimizedLaunchingConstants.OPTIMIZED_LAUNCH_GROUP_ID;
    }

    @Override
    protected String getText() {
        return TestOptimizationPluginMessages.getString("launch.optimized.toolbar.last.optimized.name");
    }

    @Override
    protected String getTooltipText() {
        return TestOptimizationPluginMessages.getString("launch.optimized.commands.last.optimized.description");
    }

    @Override
    protected String getCommandId() {
        return "org.openclover.eclipse.core.launching.optimized.commands.LastCloveredLaunchShortcut";
    }

    @Override
    protected String getDescription() {
        return CloverEclipsePluginMessages.LAUNCH_TOOLBAR_LAST_CLOVERED_DESCRIPTION();
    }
}