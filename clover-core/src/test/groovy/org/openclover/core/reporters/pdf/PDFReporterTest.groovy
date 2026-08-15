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

    private void testGenerateCurrentReport(boolean alwaysReport, int expectedReturnValue) throws IOException, CloverException {
        final File outFile = File.createTempFile(getName(), ".pdf", TestUtils.createEmptyDirFor(getClass(), getName()))
        outFile.delete()

        Current config = new Current()
        config.setSummary(true)
        config.setInitString(fixture.getInitStr())
        config.setAlwaysReport(alwaysReport)
        config.setFormat(Format.DEFAULT_PDF)
        config.setOutFile(outFile)
        assertEquals(expectedReturnValue, new PDFReporter(config).execute())
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
