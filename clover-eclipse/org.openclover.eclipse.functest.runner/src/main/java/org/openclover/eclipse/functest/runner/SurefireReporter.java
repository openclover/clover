package org.openclover.eclipse.functest.runner;

import java.io.File;
import java.util.List;

/** Writes per-project JUnit XML reports to target/surefire-reports/. Implemented in step 5. */
public class SurefireReporter {

    public static void write(List<TestResult> results, File reportsDir, String eclipseVersion) {
        // TODO step 5: write TEST-eclipse-${eclipseVersion}-${projectName}.xml per project
        for (TestResult r : results) {
            String status = r.hasFailed() ? "FAIL" : "PASS";
            System.out.printf("[runner] %-45s %s  (%.1fs)%n",
                    r.getProjectName(), status, r.getDurationMs() / 1000.0);
        }
    }
}
