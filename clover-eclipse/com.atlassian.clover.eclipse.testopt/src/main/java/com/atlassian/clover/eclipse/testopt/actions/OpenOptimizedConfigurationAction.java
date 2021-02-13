package com.atlassian.clover.eclipse.testopt.actions;

import com.atlassian.clover.eclipse.testopt.OptimizedLaunchingConstants;
import org.eclipse.debug.ui.actions.OpenLaunchDialogAction;

public class OpenOptimizedConfigurationAction extends OpenLaunchDialogAction {
    public OpenOptimizedConfigurationAction() {
        super(OptimizedLaunchingConstants.OPTIMIZED_LAUNCH_GROUP_ID);
    }
}