package org.openclover.eclipse.functest.runner;

import org.eclipse.core.resources.IProject;

/** Loads CloverDatabase and asserts coverage data. Implemented in step 5. */
public class CoverageVerifier {

    public static void verifyDbOnly(IProject project, TestResult result) {
        // TODO step 5: CloverDatabase.loadWithCoverage(), assert non-empty registry
    }

    public static void verify(IProject project, TestResult result) {
        // TODO step 6: additionally assert coverage data after test run
    }
}
