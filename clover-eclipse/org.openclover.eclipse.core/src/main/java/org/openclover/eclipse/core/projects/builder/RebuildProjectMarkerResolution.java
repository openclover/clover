package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.openclover.eclipse.core.CloverPlugin;

import static org.openclover.eclipse.core.CloverPlugin.logError;

class RebuildProjectMarkerResolution implements IMarkerResolution {
    @Override
    public String getLabel() {
        return "Rebuild the project";
    }

    @Override
    public void run(IMarker iMarker) {
        final IProject project = (IProject) iMarker.getResource();
        try {
            project.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
        } catch (CoreException e) {
            logError("Unable to rebuild project", e);
        }
    }
}
