package org.openclover.eclipse.functest.runner;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaCore;
import org.openclover.eclipse.core.projects.CloverProject;

import java.io.File;

/** Checks that a project built cleanly and Clover instrumentation ran. */
public class BuildVerifier {

    public static void verify(IProject project, TestResult result) {
        checkNoErrorMarkers(project, result);
        checkCloverDb(project, result);
        if (!result.hasBuildErrors()) {
            checkClassFiles(project, result);
        }
    }

    private static void checkNoErrorMarkers(IProject project, TestResult result) {
        try {
            IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            for (IMarker marker : markers) {
                Object severity = marker.getAttribute(IMarker.SEVERITY);
                if (severity instanceof Integer && IMarker.SEVERITY_ERROR == (Integer) severity) {
                    String msg = (String) marker.getAttribute(IMarker.MESSAGE);
                    result.fail("Build error: " + msg);
                }
            }
        } catch (Exception e) {
            result.fail("Build error: could not read markers: " + e.getMessage());
        }
    }

    private static void checkCloverDb(IProject project, TestResult result) {
        try {
            File db = CloverProject.getFor(project).getRegistryFile();
            result.assertTrue(db != null && db.exists(),
                    "Clover DB not found at: " + (db != null ? db.getAbsolutePath() : "null"));
        } catch (Exception e) {
            result.fail("Could not locate Clover DB: " + e.getMessage());
        }
    }

    private static void checkClassFiles(IProject project, TestResult result) {
        try {
            org.eclipse.core.runtime.IPath outputPath =
                    JavaCore.create(project).getOutputLocation();
            File outputDir = project.getWorkspace().getRoot()
                    .getFolder(outputPath).getLocation().toFile();
            result.assertTrue(hasClassFile(outputDir), "No .class files found in " + outputDir);
        } catch (Exception e) {
            result.fail("Could not check class files: " + e.getMessage());
        }
    }

    private static boolean hasClassFile(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        File[] entries = dir.listFiles();
        if (entries == null) {
            return false;
        }
        for (File f : entries) {
            if (f.isFile() && f.getName().endsWith(".class")) {
                return true;
            }
            if (f.isDirectory() && hasClassFile(f)) {
                return true;
            }
        }
        return false;
    }
}
