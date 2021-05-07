package org.openclover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.CloverPlugin;

public class ToggleCloverCompilerActionDelegate extends SingleCloverProjectActionDelegate {
    @Override
    protected void updateStateForSelection(IAction action) {
        super.updateStateForSelection(action);
        if (action.isEnabled()) {
            try {
                CloverProject project = CloverProject.getFor((IProject)projects.iterator().next());
                action.setChecked(project.getSettings().isInstrumentationEnabled());
            } catch (CoreException e) {
                CloverPlugin.logError("Unable to check/uncheck " + getClass().getName(), e);
            }
        }
    }

    @Override
    public void run(IAction action) {
        try {
            CloverProject project = CloverProject.getFor((IProject)projects.iterator().next());
            if (project != null) {
                project.getSettings().setInstrumentationEnabled(action.isChecked());
                CloverPlugin.getInstance().getCoverageMonitor().fireCoverageChange(project);

                if (project.okayToRebuild(getShell())) {
                    project.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
                }
            }
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to toggle Clover compiler", e);
        }
    }
}
