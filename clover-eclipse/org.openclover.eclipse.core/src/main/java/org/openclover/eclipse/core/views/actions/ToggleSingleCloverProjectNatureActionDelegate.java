package org.openclover.eclipse.core.views.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

import static org.openclover.eclipse.core.CloverPlugin.logError;

public class ToggleSingleCloverProjectNatureActionDelegate
    extends SingleCloverProjectActionDelegate {

    @Override
    public void run(IAction action) {
        boolean showCloverViews = false;
        for (final IProject project : projects) {
            IJavaProject javaProject = JavaCore.create(project);

            // Cannot modify closed or non-existant projects.
            if (javaProject != null && project.exists() && project.isOpen()) {
                //show Clover views if any one project was made Clover-enabled
                showCloverViews |=
                        !isCloverEnabled(project)
                                & CloverProject.toggleWithUserFeedback(getShell(), javaProject);
            }
        }

        try {
            // Automatically show Clover views if project will have Clover enabled
            // and "Auto open clover views" toggle in Preferences in enabled
            if ( CloverPlugin.getInstance().getInstallationSettings().isAutoOpenCloverViews()
                    && showCloverViews ) {
                CloverPlugin.getInstance().showViews(getPage());
            }
        } catch (CoreException e) {
            logError("Unable to show views after toggling OpenClover", e);
        }
    }

    private boolean isCloverEnabled(IProject project) {
        try {
            return CloverProject.isAppliedTo(project);
        } catch (CoreException e) {
            return false;
        }
    }

    @Override
    protected boolean enableFor(IProject project) throws CoreException {
        return project.isAccessible();
    }
}
