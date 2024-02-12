package org.openclover.functest.ant.tasks

import groovy.transform.CompileStatic
import org.apache.tools.ant.util.FileUtils
import org.hamcrest.CoreMatchers
import org.openclover.ant.tasks.HistoryPointTask
import org.openclover.buildutil.testutils.IOHelper

import java.text.DecimalFormat

import static org.hamcrest.MatcherAssert.assertThat
import static org.openclover.buildutil.testutils.AssertionUtils.assertStringContains

@CompileStatic
class CloverReportTaskTest extends CloverBuildFileTestBase {

    CloverReportTaskTest(String name) {
        super(name)
    }

    String getAntFileName() {
        return "clover-report.xml"
    }

    void testAllColumns() throws Exception {
        getProject().executeTarget("testAllColumns")
        // assert that this target was successful.
        assertNotNull(getProject().getProperty("success"))
        String realLog = getLog()
        String log = "Loading historical coverage data from"
        assertThat(realLog, CoreMatchers.not(CoreMatchers.containsString(log)))
        log = "Loading coverage database from"
        assertThat(realLog, CoreMatchers.containsString(log))
    }

    void testXmlReport() throws Exception {
        getProject().executeTarget("testXmlReport")
        assertNotNull(getProject().getProperty("testXmlReport.report"))
    }

    void testNoColumns() throws Exception {
        getProject().executeTarget("testNoColumns")
        assertNotNull(getProject().getProperty("success"))
    }

    void testSomeColumns() throws Exception {
        getProject().executeTarget("testSomeColumns")
        assertNotNull(getProject().getProperty("success"))
    }

    void testBarGraphColumns() throws Exception {
        getProject().executeTarget("testBarGraphColumns")
    }


    void testCharset() throws Exception {
        getProject().executeTarget("testCharset")
        assertNotNull(getProject().getProperty("success"))
    }

    void testBadColumns() throws Exception {
        getProject().executeTarget("testBadColumns")
    }

    void testHistoricalReport() throws Exception {
        final String dateFormat = "yyyy-MM-dd"

        setUpHistoryPoint("2005-09-04", dateFormat)

        util.setUpCoverageDb(0.6d)
        setUpHistoryPoint("2006-04-01", dateFormat)

        util.setUpCoverageDb(0.9d)
        setUpHistoryPoint("2006-08-02", dateFormat)

        getProject().executeTarget("testHistorical")

        String log = "Loading historical coverage data from"
        String realLog = getLog()
        assertThat(realLog, CoreMatchers.containsString(log))
        log = "Loading coverage database from"
        assertThat(realLog, CoreMatchers.not(CoreMatchers.containsString(log)))
    }

    void testHistoricalReportWithMultipleMovers() throws Exception {
        final String dateFormat = "yyyy-MM-dd"

        setUpHistoryPoint("2005-01-01", dateFormat)

        util.setUpCoverageDb(0.625d)

        setUpHistoryPoint("2006-01-01", dateFormat)

        util.setUpCoverageDb(0.875d)

        setUpHistoryPoint("2007-01-01", dateFormat)

        getProject().executeTarget("testHistoricalReportWithMultipleMovers")

        // remove newline chars and compress whitespace for easier string comparison
        String output = normalizeWhitespace(getProject().getProperty("testHistoricalReportWithMultipleMovers.report"))
        Properties moversProperties = new Properties()
        moversProperties.load(new FileInputStream(new File(util.getPathToSourceFile("expected-movers.properties"))))

        assertThat(output, CoreMatchers.containsString(
                "Table shows classes for which metric has changed (increased or decreased) above the threshold " +
                "(<b>+/-2</b>) over the last <b>2 years</b>. " +
                "Actual interval (based on timestamps of history points) is <b>2 years</b>. " +
                "Showing maximum <b>5</b> classes. " +
                "Metric used: <b>% TOTAL Coverage</b>, max value is <b>100</b>."))
        assertThat(output, CoreMatchers.containsString(moversProperties.getProperty("mover-1")))
        assertThat(output, CoreMatchers.containsString(moversProperties.getProperty("shaker")))
        assertThat(output, CoreMatchers.containsString(moversProperties.getProperty("loser-1")))

        assertThat(output, CoreMatchers.containsString(
                "Table shows classes for which metric has changed (increased or decreased) above the threshold " +
                "(<b>+/-21</b>) over the last <b>1 year</b>. " +
                "Actual interval (based on timestamps of history points) is <b>1 year</b>. " +
                "Showing maximum <b>1</b> classes. " +
                "Metric used: <b>% TOTAL Coverage</b>, max value is <b>100</b>."))
        assertThat(output, CoreMatchers.containsString(moversProperties.getProperty("mover-2")))
        String loser2Prop = moversProperties.getProperty("loser-2")
        loser2Prop = loser2Prop.replace("29.9", new DecimalFormat("###.0").format(29.9d))
        assertThat(output, CoreMatchers.containsString(loser2Prop))

        assertThat(output, CoreMatchers.containsString(
                "Table shows classes for which metric has changed (increased or decreased) above the threshold " +
                "(<b>+/-50</b>) over the last <b>1 year</b>. " +
                "Actual interval (based on timestamps of history points) is <b>1 year</b>. " +
                "Showing maximum <b>5</b> classes. " +
                "Metric used: <b>% TOTAL Coverage</b>, max value is <b>100</b>."))
        assertThat(output, CoreMatchers.containsString(
                "No changes in metric are outside the specified threshold (<b>+/-50</b>)"))
    }

    void testLinkedReports() throws Exception {
        final String dateFormat = "yyyy-MM-dd"
        // Note: we have to ensure that line endings from our test file are the same as produced by ant; therefore replace
        // all endings - "\r\n" or "\n" with system 'line.separator'
        final File reportLinksFile = new File(IOHelper.getProjectDir(),
                "tests-functional/src/test/resources/org/openclover/functest/ant/tasks/report-links.txt")
        assertTrue("The '" + reportLinksFile.getAbsolutePath() + "' is not a file",
                reportLinksFile.isFile())
        final String reportLinksValue = normalizeWhitespace(FileUtils.readFully(new FileReader(reportLinksFile)))

        setUpHistoryPoint("2006-01-01", dateFormat)

        util.setUpCoverageDb(0.1d)
        setUpHistoryPoint("2006-01-02", dateFormat)

        util.setUpCoverageDb(0.9d)
        setUpHistoryPoint("2006-01-03", dateFormat)

        getProject().executeTarget("testLinkedReports")
        assertStringContains(reportLinksValue,
                normalizeWhitespace(getProject().getProperty("report-contents")), false)
    }

    void testCloverHtmlReport() throws Exception {
        final String dateFormat = "yyyy-MM-dd"
        setUpHistoryPoint("2006-14-08", dateFormat)
        util.setUpCoverageDb(0.1d)
        setUpHistoryPoint("2006-15-08", dateFormat)
        int historyCount = util.getHistoryDir().list().length
        getProject().executeTarget("testCloverHtmlReport")
        assertNotNull(getProject().getProperty("success"))
        assertEquals("Number of history points", historyCount + 1, util.getHistoryDir().list().length)
        String log = "Loading historical coverage data from"
        String realLog = getLog()
        assertThat("expecting log to contain", realLog, CoreMatchers.containsString(log))
        log = "Loading coverage database from"
        assertThat("expecting log to contain", realLog, CoreMatchers.containsString(log))

    }

    void testCloverPdfReport() throws Exception {
        final String dateFormat = "yyyy-MM-dd"
        setUpHistoryPoint("2006-14-08", dateFormat)
        util.setUpCoverageDb(0.1d)
        setUpHistoryPoint("2006-15-08", dateFormat)
        int historyCount = util.getHistoryDir().list().length

        getProject().executeTarget("testCloverPdfReport")
        assertNotNull(getProject().getProperty("success"))
        assertEquals("Number of history points", historyCount + 1, util.getHistoryDir().list().length)
    }

    void testPdfReports() throws Exception {
        final String dateFormat = "yyyy-MM-dd"

        setUpHistoryPoint("2006-01-01", dateFormat)

        util.setUpCoverageDb(0.1d)
        setUpHistoryPoint("2006-01-02", dateFormat)

        util.setUpCoverageDb(0.9d)
        setUpHistoryPoint("2006-01-03", dateFormat)

        getProject().executeTarget("testPdfReports")
        assertNotNull(getProject().getProperty("success"))
    }

    void testDashboardPresent() throws Exception {
        getProject().executeTarget("testConditionalDashboard")
        assertNotNull(getProject().getProperty("treeMapInDashboard"))
        assertNotNull(getProject().getProperty("treeMapDashJsonInDashboard"))
        assertNotNull(getProject().getProperty("treeMapInJson"))
        assertNotNull(getProject().getProperty("treeMapInAllPkgs"))
    }

    void testDashboardAbsentOnSystemProperty() throws Exception {
        System.getProperties().setProperty("clover.skipTreeMap", "") // cleaned up in tearDown
        getProject().executeTarget("testConditionalDashboard")
        assertNull(getProject().getProperty("treeMapInDashboard"))
        assertNull(getProject().getProperty("treeMapDashJsonInDashboard"))
        assertNull(getProject().getProperty("treeMapInJson"))
        assertNull(getProject().getProperty("treeMapInAllPkgs"))
    }

    void testDashboardPresentOnSystemPropertySetToFalse() throws Exception {
        System.getProperties().setProperty("clover.skipTreeMap", "False") // cleaned up in tearDown
        getProject().executeTarget("testConditionalDashboard")
        assertNotNull(getProject().getProperty("treeMapInDashboard"))
        assertNotNull(getProject().getProperty("treeMapDashJsonInDashboard"))
        assertNotNull(getProject().getProperty("treeMapInJson"))
        assertNotNull(getProject().getProperty("treeMapInAllPkgs"))
    }

    void testReport() throws Exception {
        getProject().executeTarget("testReport")
        // only Classic report contains all-pkgs.html file
        assertNotNull(getProject().getProperty("allPkgsHtml"))
        // Historical Classic report shall NOT contain page header
        assertStringContains("<header class=\"aui-page-header\">",
                getProject().getProperty("historicalHtml"), true)
    }

    void setUpHistoryPoint(String date, String dateFormat) {
        HistoryPointTask hp = new HistoryPointTask()
        hp.setProject(project)
        hp.init()
        hp.setHistoryDir(util.getHistoryDir())
        hp.setDate(date)
        hp.setDateFormat(dateFormat)
        hp.setInitString(util.getInitString())
        hp.execute()
    }

    @Override
    void tearDown() throws Exception {
        super.tearDown()
        System.getProperties().remove("clover.skipTreeMap") //clean up after testDashboardAbsentOnSystemProperty
    }
}
