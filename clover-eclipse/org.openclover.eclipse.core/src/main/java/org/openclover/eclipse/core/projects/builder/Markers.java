package org.openclover.eclipse.core.projects.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.openclover.eclipse.core.CloverPlugin;

import java.util.HashMap;

public class Markers {
    public static final String ID = CloverPlugin.ID + ".markers";
    public static final String PROBLEM_ID = ID + ".problem";
    public static final String STALEDB_ID = PROBLEM_ID + ".staledb";

    public static final QualifiedName STALEDB_PROPERTY_NAME = new QualifiedName(CloverPlugin.ID, "staledb");

    public static void createCloverBuilderProblemMarker(IResource resource, String message, int severity) throws CoreException {
        createCloverBuilderProblemMarker(resource, message, Markers.PROBLEM_ID, severity);
    }

    public static void createCloverStaleDbMarker(IResource resource, final String message) throws CoreException {
        createCloverBuilderProblemMarker(resource, message, Markers.STALEDB_ID);
    }

    public static void createCloverBuilderProblemMarker(IResource resource, final String message, String markerId) throws CoreException {
        createCloverBuilderProblemMarker(resource, message, markerId, IMarker.SEVERITY_ERROR);
    }

    public static void createCloverBuilderProblemMarker(IResource resource, final String message, String markerId, final int severity) throws CoreException {
        MarkerUtilities.createMarker(
            resource,
            new HashMap() {
                {put(IMarker.SEVERITY, severity);}
                {put(IMarker.MESSAGE, message);}
            }, markerId);
    }

    public static void deleteCloverBuilderErrorMarkers(IProject project) throws CoreException {
        deleteMarkersFor(project, PROBLEM_ID);
    }

    public static void deleteCloverStaleDbMarkers(IProject project) throws CoreException {
        deleteMarkersFor(project, STALEDB_ID);
    }

    public static void deleteMarkersFor(IResource resource, String type) throws CoreException {
        IMarker[] markers = resource.findMarkers(type, true, IResource.DEPTH_ZERO);
        for (IMarker marker : markers) {
            marker.delete();
        }
    }
}
