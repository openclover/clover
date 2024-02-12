package org.openclover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

/**
 * Class toggles displaying of coverage for:
 *  - passed tests only
 *  - everything
 * It influences metrics and hit counts.
 */
public class ToggleIncludePassedCoverageOnlyActionDelegate extends UntargetedViewActionDelegate {

    @Override
    public void run(IAction action) {
            CloverPlugin.getInstance().getInstallationSettings().setIncludeFailedCoverage(!action.isChecked());
            CloverProject.refreshAllModels(false, true);
    }
}
