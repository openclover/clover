package org.openclover.eclipse.core.launching.actions;

import org.eclipse.debug.ui.actions.LaunchShortcutsAction;
import org.openclover.eclipse.core.launching.LaunchingConstants;

public class RunWithCloverAsAction extends LaunchShortcutsAction {
    public RunWithCloverAsAction() {
        super(LaunchingConstants.CLOVER_LAUNCH_GROUP_ID);
    }
}
