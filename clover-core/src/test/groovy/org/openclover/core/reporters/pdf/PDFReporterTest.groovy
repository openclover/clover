package org.openclover.core.reporters.pdf

import junit.framework.TestCase
import org.openclover.core.CloverStartup
import org.openclover.core.TestUtils
import org.openclover.core.registry.metrics.HasMetricsTestFixture
import org.openclover.core.reporters.Current
import org.openclover.core.reporters.Format
import org.openclover.core.reporters.Historical
import org.openclover.runtime.Logger
import org.openclover.runtime.api.CloverException
import org.openclover.runtime.api.registry.CloverRegistryException

class PDFReporterTest extends TestCase {
    HasMetricsTestFixture fixture

    void setUp() throws IOException, CloverRegistryException {
        fixture = new HasMetricsTestFixture(PDFReporterTest.class.getName())
        fixture.createSampleRegistry()
    }

    void testGenerateCurrentReportWithoutData() throws CloverException, IOException {
        // with alwaysReport the empty report is still written, without it nothing is generated
        testGenerateCurrentReport(true, 0)
        testGenerateCurrentReport(false, 1)
    }

    private File testGenerateCurrentReport(boolean alwaysReport, int expectedReturnValue) throws IOException, CloverException {
        final File outFile = File.createTempFile(getName(), ".pdf", TestUtils.createEmptyDirFor(getClass(), getName()))
        outFile.delete()

        assertEquals(expectedReturnValue, runReport(outFile, alwaysReport))
        return outFile
    }

    private int runReport(File outFile, boolean alwaysReport) throws CloverException {
        Current config = new Current()
        config.setSummary(true)
        config.setInitString(fixture.getInitStr())
        config.setAlwaysReport(alwaysReport)
        config.setFormat(Format.DEFAULT_PDF)
        config.setOutFile(outFile)
        return new PDFReporter(config).execute()
    }

    /**
     * The report is rendered beside its destination and moved into place only once it is complete,
     * so a run that produces nothing leaves whatever was already there untouched.
     */
    void testAFailedRunLeavesTheExistingReportAlone() throws IOException, CloverException {
        final File outFile = new File(TestUtils.createEmptyDirFor(getClass(), getName()), "coverage.pdf")
        outFile.text = "yesterday's report"

        assertEquals("no coverage, so nothing should be reported", 1, runReport(outFile, false))

        assertTrue("the previous report must survive", outFile.exists())
        assertEquals("yesterday's report", outFile.text)
        assertFalse("the work file must not be left behind",
                new File(outFile.parentFile, outFile.name + ".tmp").exists())
    }

    void testASuccessfulRunReplacesTheExistingReport() throws IOException, CloverException {
        final File outFile = new File(TestUtils.createEmptyDirFor(getClass(), getName()), "coverage.pdf")
        outFile.text = "yesterday's report"

        assertEquals(0, runReport(outFile, true))

        assertTrue("a PDF should have replaced the old file",
                outFile.bytes.length > 0 && new String(outFile.bytes, 0, 5, "ISO-8859-1") == "%PDF-")
        assertFalse("the work file must not be left behind",
                new File(outFile.parentFile, outFile.name + ".tmp").exists())
    }

    void testGenerateHistoryReportWithoutData() throws IOException, CloverException {
        Historical config = new Historical()
        config.setAlwaysReport(false)
        config.setFormat(Format.DEFAULT_PDF)
        config.processAndValidate()
        File tmpFile = File.createTempFile(getName(), ".pdf", TestUtils.createEmptyDirFor(getClass(), getName()))
        config.setOutFile(tmpFile)
        config.setHistoryDir(new File("."))
        assertEquals(1, new PDFReporter(config).execute())
    }
}
