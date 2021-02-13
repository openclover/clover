package com.atlassian.clover.eclipse.core.launching.actions;

import com.atlassian.clover.eclipse.core.launching.LaunchingConstants;
import org.eclipse.debug.ui.actions.AbstractLaunchToolbarAction;

public class CloverLaunchToolbarAction extends AbstractLaunchToolbarAction {
    public CloverLaunchToolbarAction() {
        super(LaunchingConstants.CLOVER_LAUNCH_GROUP_ID);
    }
}
