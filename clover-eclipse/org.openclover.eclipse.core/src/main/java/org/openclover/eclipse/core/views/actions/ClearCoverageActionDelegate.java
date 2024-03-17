package org.openclover.eclipse.core.views.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

import static org.openclover.eclipse.core.CloverPlugin.logError;

public class ClearCoverageActionDelegate extends SingleCloverProjectActionDelegate {

    @Override
    public void run(IAction action) {
        IProject project = (IProject)projects.iterator().next();

        try {
            if (CloverProject.isAppliedTo(project)) {
                CloverProject cloverProject = CloverProject.getFor(project);
                cloverProject.clearCoverage();
            }
        } catch (Exception e) {
            logError("Unable to clear coverage", e);
        }
    }
}
