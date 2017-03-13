package com.atlassian.clover.eclipse.core.launching.actions;

import org.eclipse.debug.ui.actions.LaunchShortcutsAction;
import com.atlassian.clover.eclipse.core.launching.LaunchingConstants;

public class RunWithCloverAsAction extends LaunchShortcutsAction {
    public RunWithCloverAsAction() {
        super(LaunchingConstants.CLOVER_LAUNCH_GROUP_ID);
    }
}
