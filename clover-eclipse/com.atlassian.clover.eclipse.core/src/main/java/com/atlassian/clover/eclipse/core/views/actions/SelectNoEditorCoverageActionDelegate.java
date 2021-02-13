package com.atlassian.clover.eclipse.core.views.actions;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.settings.InstallationSettings;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

public class SelectNoEditorCoverageActionDelegate extends SelectionUntargetedCoverageViewActionDelegate {

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        action.setChecked(
            CloverPlugin.getInstance().getInstallationSettings().getEditorCoverageStyle() == InstallationSettings.Values.SHOW_NO_COVERAGE_IN_EDITORS);
    }

    @Override
    protected void onSelection(IAction action) {
        CloverPlugin.getInstance().getInstallationSettings().setEditorCoverageStyle(
            InstallationSettings.Values.SHOW_NO_COVERAGE_IN_EDITORS);
    }
}
