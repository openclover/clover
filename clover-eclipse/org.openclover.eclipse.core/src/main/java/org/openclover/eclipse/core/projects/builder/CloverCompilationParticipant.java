package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

import java.util.Date;

public class CloverCompilationParticipant extends CompilationParticipant {
    @Override
    public void buildStarting(BuildContext[] files, boolean isBatch) {
        if (files.length == 0) {
            return;
        } else {
            CloverPlugin.logVerbose("----CLOVER: BUILD ITERATION STARTED : " + new Date() + "----");
            try {
                final CloverProject project = CloverProject.getFor(files[0].getFile().getProject());
                if (project != null) {
                    project.getBuildCoordinator().registerFilesForInstrumentation(files);
                }
            } catch (CoreException e) {
                CloverPlugin.logError("Unable to instrument and compile", e);
                try {
                    Markers.createCloverStaleDbMarker(
                        files[0].getFile().getProject(),
                        "Clover was unable to fully instrument and compile for this project. Please perform a clean rebuild. " + e.getMessage());
                } catch (CoreException e1) {
                    //Ignore. Look, we tried really hard to tell the user, give us a break.
                }
            }
            CloverPlugin.logVerbose("----CLOVER: BUILD ITERATION ENDED : " + new Date() + "----");
        }
        super.buildStarting(files, isBatch);
    }

    @Override
    public void cleanStarting(IJavaProject project) {
        //Do nothing, coverage clean confirmation happens from PreJavaBuilder
    }

    @Override
    public boolean isActive(IJavaProject project) {
        try {
            return CloverProject.isAppliedTo(project);
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to determine if compilation participant is active for project " + project.getProject().getName(), e);
            return false;
        }
    }
}
