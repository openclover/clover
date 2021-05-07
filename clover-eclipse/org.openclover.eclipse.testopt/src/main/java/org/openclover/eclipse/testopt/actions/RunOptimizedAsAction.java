package org.openclover.eclipse.testopt.actions;

import org.openclover.eclipse.testopt.OptimizedLaunchingConstants;
import org.eclipse.debug.ui.actions.LaunchShortcutsAction;

public class RunOptimizedAsAction extends LaunchShortcutsAction {
    public RunOptimizedAsAction() {
        super(OptimizedLaunchingConstants.OPTIMIZED_LAUNCH_GROUP_ID);
    }
}