package org.openclover.eclipse.core.projects.builder;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.Date;
import java.util.Map;

public class PostJavaCloverBuilder extends BaseCloverBuilder {
    public static final String ID = CloverPlugin.ID + ".postjavabuilder";

    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
        CloverPlugin.logVerbose("----CLOVER: POST JAVA BUILDER (" + kindToString(kind) + "): " + new Date() + "----");
        final CloverProject targetProject = CloverProject.getFor(getProject());

        if (targetProject != null) {
            if (CloverPlugin.getInstance().isLicensePresent()) {
                if (!targetProject.getSettings().isInstrumentationEnabled()) {
                    CloverPlugin.logVerbose("PostJavaCloverBuilder: not building as Clover compilation not enabled");
                } else {
                    CloverPlugin.logVerbose("CLOVER: BUILD[" + kindToString(kind) + "] ENDING : " + new Date() + "----");
                    targetProject.getBuildCoordinator().onEndOfBuild(kind, monitor);
                    CloverPlugin.logVerbose("CLOVER: BUILD[" + kindToString(kind) + "] COMPLETED : " + new Date() + "----");
                }
            } else {
                CloverPlugin.logVerbose("PostJavaCloverBuilder: not building as license terminated");
            }
        } else {
            CloverPlugin.logWarning("PostJavaCloverBuilder: not building as user project no longer open");
        }
        return new IProject[]{};
    }
}