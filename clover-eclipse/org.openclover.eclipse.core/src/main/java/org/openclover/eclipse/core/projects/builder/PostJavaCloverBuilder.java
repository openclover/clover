package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

import java.util.Date;
import java.util.Map;

import static org.openclover.eclipse.core.CloverPlugin.logVerbose;
import static org.openclover.eclipse.core.CloverPlugin.logWarning;

public class PostJavaCloverBuilder extends BaseCloverBuilder {
    public static final String ID = CloverPlugin.ID + ".postjavabuilder";

    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
        logVerbose("----OPENCLOVER: POST JAVA BUILDER (" + kindToString(kind) + "): " + new Date() + "----");
        final CloverProject targetProject = CloverProject.getFor(getProject());

        if (targetProject != null) {
            if (!targetProject.getSettings().isInstrumentationEnabled()) {
                logVerbose("PostJavaCloverBuilder: not building as OpenClover compilation not enabled");
            } else {
                logVerbose("OPENCLOVER: BUILD[" + kindToString(kind) + "] ENDING : " + new Date() + "----");
                targetProject.getBuildCoordinator().onEndOfBuild(kind, monitor);
                logVerbose("OPENCLOVER: BUILD[" + kindToString(kind) + "] COMPLETED : " + new Date() + "----");
            }
        } else {
            logWarning("PostJavaCloverBuilder: not building as user project no longer open");
        }
        return new IProject[]{};
    }
}