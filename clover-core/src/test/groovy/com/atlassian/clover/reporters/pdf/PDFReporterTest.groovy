package com.atlassian.clover.reporters.pdf

import org.openclover.runtime.api.CloverException
import com.atlassian.clover.CloverStartup
import org.openclover.runtime.Logger
import com.atlassian.clover.TestUtils
import com.atlassian.clover.registry.metrics.HasMetricsTestFixture
import com.atlassian.clover.reporters.Current
import com.atlassian.clover.reporters.Format
import com.atlassian.clover.reporters.Historical
import org.openclover.runtime.api.registry.CloverRegistryException
import junit.framework.TestCase

class PDFReporterTest extends TestCase {
    HasMetricsTestFixture fixture

    void setUp() throws IOException, CloverRegistryException {
        CloverStartup.loadLicense(Logger.getInstance())
        fixture = new HasMetricsTestFixture(PDFReporterTest.class.getName())
        fixture.createSampleRegistry()
    }

    void testGenerateCurrentReportWithoutData() throws CloverException, IOException {
        testGenerateCurrentReport(true, 1)
        testGenerateCurrentReport(false, 0)
    }

    private void testGenerateCurrentReport(boolean alwaysReport, int expectedReturnValue) throws IOException, CloverException {
        final File outFile = File.createTempFile(getName(), ".pdf", TestUtils.createEmptyDirFor(getClass(), getName()))
        outFile.delete()

        Current config = new Current()
        config.setSummary(true)
        config.setInitString(fixture.getInitStr())
        config.setAlwaysReport(alwaysReport)
        config.setFormat(Format.DEFAULT_PDF)
        config.setOutFile(outFile)
        assertFalse(new PDFReporter(config).execute() == expectedReturnValue)
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
