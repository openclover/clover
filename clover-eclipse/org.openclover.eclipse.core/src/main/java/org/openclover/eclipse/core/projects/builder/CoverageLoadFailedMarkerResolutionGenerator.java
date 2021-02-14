package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

public class CoverageLoadFailedMarkerResolutionGenerator implements IMarkerResolutionGenerator {
    @Override
    public IMarkerResolution[] getResolutions(IMarker iMarker) {
        if (iMarker.getResource() instanceof IProject) {
            return new IMarkerResolution[]{new RebuildProjectMarkerResolution()};
        } else {
            return new IMarkerResolution[0];
        }
    }
}
