package org.openclover.eclipse.core.launching.actions;

import org.eclipse.debug.ui.actions.OpenLaunchDialogAction;
import org.openclover.eclipse.core.launching.LaunchingConstants;

public class OpenCloverConfigurationAction extends OpenLaunchDialogAction {
    public OpenCloverConfigurationAction() {
        super(LaunchingConstants.CLOVER_LAUNCH_GROUP_ID);
    }
}
