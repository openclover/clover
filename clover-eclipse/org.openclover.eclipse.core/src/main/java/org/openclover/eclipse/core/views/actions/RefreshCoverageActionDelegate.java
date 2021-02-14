package org.openclover.eclipse.core.views.actions;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;

public class RefreshCoverageActionDelegate extends SingleCloverProjectActionDelegate {
    @Override
    public void run(IAction action) {
        IProject project = projects.iterator().next();

        try {
            if (CloverProject.isAppliedTo(project)) {
                CloverProject cloverProject = CloverProject.getFor(project);
                cloverProject.refreshModel(false, true);
            }
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to reload coverage", e);
        }
    }
}
