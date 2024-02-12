package org.openclover.eclipse.core.launching.actions;

import org.eclipse.debug.ui.actions.AbstractLaunchToolbarAction;
import org.openclover.eclipse.core.launching.LaunchingConstants;

public class CloverLaunchToolbarAction extends AbstractLaunchToolbarAction {
    public CloverLaunchToolbarAction() {
        super(LaunchingConstants.CLOVER_LAUNCH_GROUP_ID);
    }
}
