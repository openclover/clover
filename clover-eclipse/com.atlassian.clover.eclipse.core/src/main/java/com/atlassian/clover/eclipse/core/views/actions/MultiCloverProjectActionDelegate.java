package com.atlassian.clover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.eclipse.core.CloverPlugin;

/**
 *
 */
public class MultiCloverProjectActionDelegate extends CloverProjectActionDelegate {
    @Override
    protected void updateStateForSelection(IAction action) {
        try {
            if (projects.size() > 0 && projects.size() <= getMaxSelectedProjectCount() && CloverPlugin.getInstance().isLicensePresent()) {
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
            CloverPlugin.logError("Unable to enable/disable " + getClass().getName(), e);
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

