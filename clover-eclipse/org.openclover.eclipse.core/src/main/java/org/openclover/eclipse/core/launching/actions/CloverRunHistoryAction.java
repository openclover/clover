package org.openclover.eclipse.core.launching.actions;

import org.eclipse.debug.ui.actions.AbstractLaunchHistoryAction;
import org.openclover.eclipse.core.launching.LaunchingConstants;

public class CloverRunHistoryAction extends AbstractLaunchHistoryAction {
    public CloverRunHistoryAction() {
        super(LaunchingConstants.CLOVER_LAUNCH_GROUP_ID);
    }
}
