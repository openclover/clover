package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

import java.util.Date;
import java.util.Map;

import static org.openclover.eclipse.core.CloverPlugin.logVerbose;
import static org.openclover.eclipse.core.CloverPlugin.logWarning;

public class PreJavaCloverBuilder extends BaseCloverBuilder {
    public static final String ID = CloverPlugin.ID + ".prejavabuilder";

    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
        logVerbose("----OPENCLOVER: PRE JAVA BUILDER (" + kindToString(kind) + "): " + new Date() + "----");
        Markers.deleteCloverBuilderErrorMarkers(getProject());

        //If instrumentation is turned off (project driven by Maven/Ant, user turning Clover off for a while)
        //Clear staledb markers as a full build *should* fix it (but we have no way of knowing)
        CloverProject targetProject = CloverProject.getFor(getProject());
        if (targetProject != null
            && kind == IncrementalProjectBuilder.FULL_BUILD
            && !targetProject.getSettings().isInstrumentationEnabled()) {
            Markers.deleteCloverStaleDbMarkers(getProject());
        }
        return new IProject[]{};
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        logVerbose("----OPENCLOVER: PRE JAVA BUILDER CLEAN: " + new Date() + "----");
        CloverProject targetProject = CloverProject.getFor(getProject());

        if (targetProject != null) {
            if (!targetProject.getSettings().isInstrumentationEnabled()) {
                logVerbose("PreJavaCloverBuilder: not cleaning as OpenClover compilation not enabled");
            } else {
                targetProject.getBuildCoordinator().onClean(monitor);
            }
        } else {
            logWarning("PreJavaCloverBuilder: not cleaning as user project no longer open");
        }
    }
}
