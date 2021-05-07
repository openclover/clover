package org.openclover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.settings.InstallationSettings;

public class SelectAllEditorCoverageActionDelegate extends SelectionUntargetedCoverageViewActionDelegate {

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        action.setChecked(
            CloverPlugin.getInstance().getInstallationSettings().getEditorCoverageStyle() == InstallationSettings.Values.SHOW_ALL_COVERAGE_IN_EDITORS);
    }

    @Override
    protected void onSelection(IAction action) {
        CloverPlugin.getInstance().getInstallationSettings().setEditorCoverageStyle(
            InstallationSettings.Values.SHOW_ALL_COVERAGE_IN_EDITORS);
    }
}
