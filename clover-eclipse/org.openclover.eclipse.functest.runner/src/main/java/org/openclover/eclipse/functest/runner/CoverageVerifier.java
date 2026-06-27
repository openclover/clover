package org.openclover.eclipse.functest.runner;

import org.eclipse.core.resources.IProject;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageDataSpec;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.eclipse.core.projects.CloverProject;

/** Loads CloverDatabase and asserts instrumentation and coverage data. */
public class CoverageVerifier {

    /** After build only (no tests run): verify registry has packages but skip hit-count check. */
    public static void verifyDbOnly(IProject project, TestResult result) {
        CloverDatabase db = loadDb(project, result);
        if (db == null) {
            return;
        }
        result.assertFalse(
                db.getRegistry().getProject().getPackages(HasMetricsFilter.ACCEPT_ALL).isEmpty(),
                "No packages found in Clover registry — was instrumentation enabled?");
    }

    /** After test run: verify registry has packages AND coverage data was recorded. */
    public static void verify(IProject project, TestResult result) {
        CloverDatabase db = loadDb(project, result);
        if (db == null) {
            return;
        }
        result.assertFalse(
                db.getRegistry().getProject().getPackages(HasMetricsFilter.ACCEPT_ALL).isEmpty(),
                "No packages found in Clover registry — was instrumentation enabled?");
        result.assertFalse(
                db.getCoverageData().isEmpty(),
                "Clover coverage data is empty — did the test run record hits?");
        int covered = db.getRegistry().getProject().getMetrics().getNumCoveredElements();
        result.assertTrue(covered > 0, "Zero covered elements in Clover registry after test run");
    }

    private static CloverDatabase loadDb(IProject project, TestResult result) {
        String initString;
        try {
            initString = CloverProject.getFor(project).getRegistryFile().getAbsolutePath();
        } catch (Exception e) {
            result.fail("Could not resolve Clover DB path: " + e.getMessage());
            return null;
        }
        try {
            return CloverDatabase.loadWithCoverage(initString, new CoverageDataSpec());
        } catch (Exception e) {
            result.fail("Could not load Clover DB from " + initString + ": " + e.getMessage());
            return null;
        }
    }
}
