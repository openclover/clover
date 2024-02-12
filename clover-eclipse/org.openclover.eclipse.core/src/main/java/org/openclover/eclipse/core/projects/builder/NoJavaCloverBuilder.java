package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.openclover.eclipse.core.CloverPlugin;

import java.util.Map;

public class NoJavaCloverBuilder extends IncrementalProjectBuilder {
    public static final String ID = CloverPlugin.ID + ".nojavabuilder";

    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
        Markers.deleteCloverBuilderErrorMarkers(getProject());
        Markers.createCloverBuilderProblemMarker(
            getProject(),
            "Coverage might not be tracked for this project as the standard Java Builder is not used to build it. " +
            "If you build the project with Maven/Ant instead of the Eclipse Java Builder, integrate Clover-for-Maven or Clover-for-Ant and Clover-for-Eclispe will show your coverage.",
            IMarker.SEVERITY_WARNING
        );
        //Delete stale db marker as we may stil flag these errors
        //during a background refresh and the only way
        //(as uncertain it is) to clear it is when we detect a full build.
        if (kind == IncrementalProjectBuilder.FULL_BUILD) {
            Markers.deleteCloverStaleDbMarkers(getProject());
        }

        return new IProject[] {};
    }
}
