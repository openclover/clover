package org.openclover.eclipse.testopt.actions;

import org.openclover.eclipse.testopt.OptimizedLaunchingConstants;
import org.eclipse.debug.ui.actions.AbstractLaunchHistoryAction;

public class OptimizedRunHistoryAction extends AbstractLaunchHistoryAction {
    public OptimizedRunHistoryAction() {
        super(OptimizedLaunchingConstants.OPTIMIZED_LAUNCH_GROUP_ID);
    }
}