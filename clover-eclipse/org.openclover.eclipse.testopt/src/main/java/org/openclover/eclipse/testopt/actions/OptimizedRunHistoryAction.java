package org.openclover.eclipse.testopt.actions;

import org.eclipse.debug.ui.actions.AbstractLaunchHistoryAction;
import org.openclover.eclipse.testopt.OptimizedLaunchingConstants;

public class OptimizedRunHistoryAction extends AbstractLaunchHistoryAction {
    public OptimizedRunHistoryAction() {
        super(OptimizedLaunchingConstants.OPTIMIZED_LAUNCH_GROUP_ID);
    }
}