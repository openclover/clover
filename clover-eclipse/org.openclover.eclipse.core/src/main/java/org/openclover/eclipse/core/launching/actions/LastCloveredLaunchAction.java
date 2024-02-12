package org.openclover.eclipse.core.launching.actions;

import org.eclipse.debug.ui.actions.RelaunchLastAction;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.launching.LaunchingConstants;

public class LastCloveredLaunchAction extends RelaunchLastAction {
    @Override
    public String getMode() {
        return LaunchingConstants.CLOVER_MODE;
    }

    @Override
    public String getLaunchGroupId() {
        return LaunchingConstants.CLOVER_LAUNCH_GROUP_ID;
    }

    @Override
    protected String getText() {
        return CloverEclipsePluginMessages.LAUNCH_TOOLBAR_LAST_CLOVERED_NAME();
    }

    @Override
    protected String getTooltipText() {
        return CloverEclipsePluginMessages.LAUNCH_TOOLBAR_LAST_CLOVERED_DESCRIPTION();
    }

    @Override
    protected String getCommandId() {
        return "org.openclover.eclipse.core.launching.commands.LastCloveredLaunchShortcut";
    }

    @Override
    protected String getDescription() {
        return CloverEclipsePluginMessages.LAUNCH_TOOLBAR_LAST_CLOVERED_DESCRIPTION();
    }
}
