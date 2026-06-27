package org.openclover.eclipse.functest.runner;

import org.eclipse.core.resources.IProject;

/** Checks build markers and presence of .clover/clover.db after a full build. Implemented in step 5. */
public class BuildVerifier {

    public static void verify(IProject project, TestResult result) {
        // TODO step 5: check IMarker.SEVERITY_ERROR, .clover/clover.db, bin/*.class
    }
}
