package com.atlassian.clover.reporters.console

import com.atlassian.clover.api.CloverException
import com.atlassian.clover.api.registry.ClassInfo
import com.atlassian.clover.util.Formatting
import com.atlassian.clover.TestUtils
import com.atlassian.clover.registry.metrics.ClassMetrics
import com.atlassian.clover.registry.metrics.HasMetricsTestFixture
import junit.framework.TestCase

class ConsoleReporterTest extends TestCase {


    void testPrintMetricsSummary() throws IOException, CloverException {
        // output buffer
        final StringWriter stringWriter = new StringWriter()
        PrintWriter writer = new PrintWriter(stringWriter)

        // create sample data
        HasMetricsTestFixture fixture = new HasMetricsTestFixture(getName())
        ClassInfo classInfo = fixture.newClass("Test", 0)
        ClassMetrics metrics = new ClassMetrics(classInfo)

        // print report
        ConsoleReporter.printMetricsSummary(writer, "title", metrics, null)

        // validate output
        final String formattedOutput =
                TestUtils.concatWithLineSeparator(true,
                        "title",
                        "Coverage:-",
                        "      Methods: 0/0 ( - )                                                                                                        ",
                        "   Statements: 0/0 ( - )                                                                                                        ",
                        "     Branches: 0/0 ( - )                                                                                                        ",
                        "        Total:  -                                                                                                               ",
                        "Complexity:-",
                        "   Avg Method: -",
                        "      Density: -",
                        "        Total: 0")

        assertEquals(formattedOutput, stringWriter.toString())
    }


    void testPrintMetricsSummaryWithUnitTests() throws IOException, CloverException {
        // set up console reporter with unit test summary enabled
        final ConsoleReporterConfig config = new ConsoleReporterConfig()
        config.setShowUnitTests(true)

        // output buffer
        final StringWriter stringWriter = new StringWriter()
        final PrintWriter printWriter = new PrintWriter(stringWriter)

        // create sample data
        ClassMetrics metrics = new ClassMetrics(null)
        metrics.setNumMethods(1)
        metrics.setNumCoveredMethods(1)
        metrics.setNumStatements(3)
        metrics.setNumCoveredStatements(2)
        metrics.setNumBranches(2)
        metrics.setNumCoveredBranches(2)
        metrics.setComplexity(8)
        metrics.setNumTests(7)
        metrics.setNumTestPasses(3)
        metrics.setNumTestFailures(2)
        metrics.setNumTestErrors(1)

        // print report
        ConsoleReporter.printMetricsSummary(printWriter, "title", metrics, config)

        // validate output
        final String fmtStatements = Formatting.getPercentStr(0.667f)
        final String fmtTotal = Formatting.getPercentStr(0.833f)
        final String fmtDensity = Formatting.format3d(2.6666667f)
        final String formattedOutput =
                TestUtils.concatWithLineSeparator(true,
                        "title",
                        "Coverage:-",
                        "      Methods: 1/1 (100%)                                                                                                       ",
                        "   Statements: 2/3 (" + fmtStatements + ")                                                                                                      ",
                        "     Branches: 2/2 (100%)                                                                                                       ",
                        "        Total: " + fmtTotal + "                                                                                                            ",
                        "Complexity:-",
                        "   Avg Method: 8",
                        "      Density: " + fmtDensity,
                        "        Total: 8",
                        "Tests:-",
                        "    Available: 7",
                        "     Executed: 6",
                        "       Passed: 3",
                        "       Failed: 2",
                        "       Errors: 1")

        assertEquals(formattedOutput, stringWriter.toString())
    }
}
