package org.openclover.eclipse.core.views.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

import static org.openclover.eclipse.core.CloverPlugin.logError;

/**
 *
 */
public class MultiCloverProjectActionDelegate extends CloverProjectActionDelegate {
    @Override
    protected void updateStateForSelection(IAction action) {
        try {
            if (projects.size() > 0 && projects.size() <= getMaxSelectedProjectCount()) {
                boolean shouldEnable = true;
                for (IProject project : projects) {
                    shouldEnable &= enableFor(project);
                    if (!shouldEnable) {
                        break;
                    }
                }

                action.setEnabled(shouldEnable);
            } else {
                action.setEnabled(false);
            }
        } catch (CoreException e) {
            logError("Unable to enable/disable " + getClass().getName(), e);
            action.setEnabled(false);
        }
    }

    protected boolean enableFor(IProject project) throws CoreException {
        return CloverProject.isAppliedTo(project);
    }

    public int getMaxSelectedProjectCount() {
        return Integer.MAX_VALUE;
    }
}

