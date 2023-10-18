package com.atlassian.clover.ant.tasks

import com.atlassian.clover.ant.testutils.CloverTestFixture
import com.atlassian.clover.cfg.Percentage
import com.atlassian.clover.registry.metrics.BlockMetrics
import groovy.transform.CompileStatic
import org.apache.tools.ant.Project

import java.text.DecimalFormat

import static org.openclover.util.Lists.newArrayList

@CompileStatic
class CloverPassTaskTest extends CloverBuildFileTestBase {
    private CloverTestFixture fixture

    CloverPassTaskTest(String aTestName) {
        super(aTestName)
    }

    String getAntFileName() {
        return "clover-report.xml"
    }

    void setUp() throws Exception {
        super.setUp()
        fixture = new CloverTestFixture(util.getWorkDir())
    }

    private String createDatabase(float overall, int elements) throws Exception {
        return createDatabase(overall,overall,overall, elements)
    }

    private String createDatabase(float stmt, float cond, float method, int elements) throws Exception {
        List<CloverTestFixture.Clazz> classList = newArrayList()

        classList.add(new CloverTestFixture.Clazz(util.getWorkDir(), "com.cenqua", "Test",
                new CloverTestFixture.Coverage(stmt, cond, method, elements)))

        String initString = fixture.createCoverageDB()
        fixture.register(initString, classList)
        fixture.write(initString, classList)
        return initString
    }

    private static final String FAIL_PROP = "clover.checkfail"

    void testTotalCoverageWith85() throws Exception {
        final String valueString = "85%"
        final DecimalFormat pcFormat = new DecimalFormat("###%")
        final String initstring = createDatabase(0.8499f, 10000)
        final String searchedLogEntry = "recorded coverage = " + pcFormat.format(0.8500) + "; target coverage = " + pcFormat.format(0.8500)

        Project project = executePassTask(valueString, initstring)

        assertFullLogContains(searchedLogEntry)
        assertEquals("",project.getProperty(FAIL_PROP))
    }


    void testTotalCoverageWith85_0() throws Exception {
        final String valueString = "85.0%"
        final DecimalFormat pcFormat = new DecimalFormat("###.0%")
        final String initstring = createDatabase(0.8499f, 10000)
        final String searchedLogEntry = "recorded coverage = " + pcFormat.format(0.8500) + "; target coverage = " + pcFormat.format(0.8500)

        Project project = executePassTask(valueString, initstring)

        assertFullLogContains(searchedLogEntry)
        assertEquals("",project.getProperty(FAIL_PROP))
    }

    void testTotalCoverageWith85_00() throws Exception {
        final String valueString = "85.00%"
        final String initstring = createDatabase(0.8499f, 10000)
        final DecimalFormat pcFormat = new DecimalFormat("###.00%")
        final String searchedLogEntry = "Total coverage of " + pcFormat.format(0.8499) + " did not meet target of " + pcFormat.format(0.8500)

        Project project = executePassTask(valueString, initstring)

        assertEquals(searchedLogEntry, project.getProperty(FAIL_PROP))
    }

    /**
     * Tests how "Total coverage" check behaves if coverage value is undefined (it's a case case when
     * project has no source code). It should always return true, no matter what expected coverage is.
     * @throws Exception
     */
    void testTotalCoverageWithUndefinedVs0() throws Exception {
        // undefined values for all metrics, 0 elements, run check with totalCoverage="0%"
        final String initString = createDatabase(BlockMetrics.VALUE_UNDEFINED, 0)
        executePassTask("0.00%", initString)

        // check against expected 0% coverage
        assertFullLogContains("Recorded coverage = -1.0 means undefined value, so cannot compare against target coverage = 0")
        assertFullLogContains("Coverage check PASSED")
    }

    /**
     * Tests how "Total coverage" check behaves if coverage value is undefined (it's a case case when
     * project has no source code). It should always return true, no matter what expected coverage is.
     * @throws Exception
     */
    void testTotalCoverageWithUndefinedVs100() throws Exception {
        // undefined values for all metrics, 0 elements, run check with totalCoverage="100%"
        final String initString = createDatabase(BlockMetrics.VALUE_UNDEFINED, 0)
        executePassTask("100.00%", initString)

        // check against expected 100% coverage
        assertFullLogContains("Recorded coverage = -1.0 means undefined value, so cannot compare against target coverage = 100")
        assertFullLogContains("Coverage check PASSED")
    }

    private Project executePassTask(String valueString, String initString) {
        Project project = getProject()
        project.setProperty(FAIL_PROP, "")
        project.setBasedir(util.getWorkDir().getAbsolutePath())

        CloverPassTask task = new CloverPassTask()
        task.setProject(project)
        task.init()
        task.setFailureProperty(FAIL_PROP)
        task.setProject(project)
        task.setTarget(new Percentage(valueString))
        task.setInitString(initString)
        task.setDebug(true)
        task.execute()
        return project
    }

    void setUpHistoryPoint(String date, String dateFormat, Project p) {
        HistoryPointTask hp = new HistoryPointTask()
        hp.setProject(p)
        hp.init()
        hp.setHistoryDir(util.getHistoryDir())
        hp.setDate(date)
        hp.setDateFormat(dateFormat)
        hp.setInitString(util.getInitString())
        hp.execute()
    }

    void testThreshold() throws Exception {
        final String dateFormat = "yyyy-MM-dd"

        util.setUpCoverageDb(1.0f)
         setUpHistoryPoint("2008-03-02", dateFormat, project)

        util.setUpCoverageDb(0.5f)
        getProject().executeTarget("clover.check.threshold")

        String realLog = getLog()
        String log = "Coverage check PASSED"
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
    }

    void testNewPackage() throws Exception {
        final String dateFormat = "yyyy-MM-dd"

        CloverTestFixture fixture = new CloverTestFixture(util.getWorkDir())
        List classList = util.createClassList(0.5f, util.getWorkDir())
        classList.add(new CloverTestFixture.Clazz(util.getWorkDir(), "com.cenqua.new", "New",
                new CloverTestFixture.Coverage(0.90f, 0.80f, 0.85f)))

        String initString = util.getInitString()
        fixture.register(initString, classList)
        fixture.write(initString, classList)

        util.setUpCoverageDb(0.0f)
       setUpHistoryPoint("2008-03-01", dateFormat, project)


        getProject().executeTarget("clover.check")

        String realLog = getLog()
        String log = "Coverage check PASSED"
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
    }

    void testNewClass() throws Exception {
        final String dateFormat = "yyy-MM-dd"
        final DecimalFormat coverageValue = new DecimalFormat("###.0%")

        CloverTestFixture fixture = new CloverTestFixture(util.getWorkDir())
        List<CloverTestFixture.Clazz> classList = util.createClassList(0.5f, util.getWorkDir())
        classList.add(new CloverTestFixture.Clazz(util.getWorkDir(), "com.cenqua", "NewClass",
                new CloverTestFixture.Coverage(0.10f, 0.05f, 0.03f)))

        String initString = util.getInitString()
        fixture.register(initString, classList)
        fixture.write(initString, classList)

        util.setUpCoverageDb(0.0f)

        setUpHistoryPoint("2008-03-01", dateFormat, project)

        getProject().executeTarget("clover.check")

        String realLog = getLog()
        String log = "Coverage check FAILED"
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
        log = "  " + coverageValue.format(0.058) + " com.cenqua.NewClass (Added)"
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
    }

    void testHistoryPoint() throws Exception {
        final String dateFormat = "yyyy-MM-dd"


        util.setUpCoverageDb(0.0f)
        setUpHistoryPoint("2008-03-01", dateFormat, project)
        util.setUpCoverageDb(1.0f)
         setUpHistoryPoint("2008-03-02", dateFormat, project)
        util.setUpCoverageDb(0.1f)

        final GregorianCalendar c = new GregorianCalendar()
        setUpHistoryPoint((c.get(Calendar.YEAR) + 1) + "-01-01", dateFormat, project)  //next year
        util.setUpCoverageDb(0.5f)

        getProject().executeTarget("clover.check.target")

        String realLog = getLog()
        String log = "Read 3 history points."
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
        log = "Coverage check FAILED"
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
        log = "Total coverage of " + new DecimalFormat("###.0%").format(0.616) + " did not meet target of 100%"
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
        log = "Total coverage of " + new DecimalFormat("###.00%").format(0.6163) + " did not meet last history point target of " + new DecimalFormat("###.00%").format(0.6662)
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
        log = "-50 to 50% com.cenqua.mover.Mover"
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))

    }

    void testNoHistoryPoints() throws Exception {
        getProject().executeTarget("clover.check")

        String realLog = getLog()
        String log = "Read 0 history points."
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))

        log = "Coverage check PASSED"
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
    }

    void testNoPreviousHistoryPoints() throws Exception {
        final String dateFormat = "yyyy-MM-dd"
        final GregorianCalendar c = new GregorianCalendar()

        util.setUpCoverageDb(0.0f)
        setUpHistoryPoint((c.get(Calendar.YEAR) + 1) + "-01-01", dateFormat, project)
        util.setUpCoverageDb(1.0f)
        setUpHistoryPoint((c.get(Calendar.YEAR) + 1) + "-01-02", dateFormat, project)
        util.setUpCoverageDb(0.1f)
        setUpHistoryPoint((c.get(Calendar.YEAR) + 1) + "-01-03", dateFormat, project)
        util.setUpCoverageDb(0.5f)

        getProject().executeTarget("clover.check")

        String realLog = getLog()
        String log = "Read 3 history points."
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))

        log = "Coverage check PASSED"
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
    }

}
