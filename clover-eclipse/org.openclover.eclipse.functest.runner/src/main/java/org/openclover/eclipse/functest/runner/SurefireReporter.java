package org.openclover.eclipse.functest.runner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Writes per-project JUnit XML reports to target/surefire-reports/. */
public class SurefireReporter {

    private static final DateTimeFormatter ISO = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public static void write(List<TestResult> results, File reportsDir, String eclipseVersion) {
        reportsDir.mkdirs();
        for (TestResult r : results) {
            writeXml(r, reportsDir, eclipseVersion);
        }
    }

    private static void writeXml(TestResult r, File reportsDir, String eclipseVersion) {
        String safeName = eclipseVersion.replace(' ', '_');
        File out = new File(reportsDir, "TEST-eclipse-" + safeName + "-" + r.getProjectName() + ".xml");
        double timeSec = r.getDurationMs() / 1000.0;
        String timestamp = ISO.format(Instant.ofEpochMilli(
                System.currentTimeMillis() - r.getDurationMs()));

        List<String> failures = r.getFailures();
        int failCount  = failures.size();
        int skipCount  = r.isSkipped() ? 1 : 0;

        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(out), StandardCharsets.UTF_8))) {
            w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            w.printf("<testsuite name=\"eclipse-%s.%s\"%n", safeName, r.getProjectName());
            w.printf("           tests=\"1\" failures=\"%d\" errors=\"0\" skipped=\"%d\"%n", failCount, skipCount);
            w.printf("           time=\"%.3f\"%n", timeSec);
            w.printf("           timestamp=\"%s\">%n", timestamp);
            w.printf("    <testcase name=\"%s\"%n", r.getProjectName());
            w.printf("              classname=\"org.openclover.eclipse.functest\"%n");
            w.printf("              time=\"%.3f\">%n", timeSec);
            if (r.isSkipped()) {
                w.printf("        <skipped message=\"%s\"/>%n", xmlEscape(r.getSkipReason()));
            } else if (!failures.isEmpty()) {
                w.printf("        <failure message=\"%s\">%n",
                        xmlEscape(failures.get(0)));
                for (String msg : failures) {
                    w.println(xmlEscape(msg));
                }
                w.println("        </failure>");
            }
            w.println("    </testcase>");
            w.println("</testsuite>");
        } catch (Exception e) {
            System.err.println("[reporter] Could not write " + out + ": " + e.getMessage());
        }
    }

    private static String xmlEscape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
