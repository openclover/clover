package com.atlassian.clover.eclipse.core.views.actions;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import org.eclipse.jface.action.IAction;

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
