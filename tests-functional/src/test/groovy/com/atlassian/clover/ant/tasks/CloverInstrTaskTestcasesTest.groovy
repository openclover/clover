package com.atlassian.clover.ant.tasks

import clover.com.google.common.collect.Maps
import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.CoverageData
import com.atlassian.clover.api.CloverException
import com.atlassian.clover.registry.entities.TestCaseInfo
import com.atlassian.clover.testutils.IOHelper

class CloverInstrTaskTestcasesTest extends CloverBuildFileTestBase {

    private String initstring
    private Map<String, TestCaseInfo> testcases
    private String currentTestClass

    CloverInstrTaskTestcasesTest(String name) {
        super(name)
    }

    String getAntFileName() {
        return "clover-instr-testcases.xml"
    }

    void mainSetUp() throws Exception {
        super.mainSetUp()
        initstring = (new File(util.getWorkDir().getAbsolutePath(),getName()+"_coverage.db")).getAbsolutePath()
        getProject().setProperty("clover.initstring", initstring)
    }

    void mainTearDown() {
        testcases = null
        currentTestClass = null
        super.mainTearDown()
    }

    void testJUnitTests() throws CloverException {
        executeTarget("runJUnitTests")
        assertAntOutputContains("Tests run: 13")
        checkTestResults("JUnit4TestCase")
    }

    void testTestNGJDK15Tests() throws CloverException {
        executeTarget("runTestNGJDK15Tests")
        assertLogContains("Total tests run: 13, Failures: 5, Skips: 0")
        checkTestResults("TestNGJDK15TestCase")
    }

    private void checkTestResults(final String testclass) throws CloverException {
        gatherTestCases(testclass)
        assertTestSuccess("noExceptionEncountered")
        assertTestSuccess("expectedExceptionEncountered1")
        assertTestSuccess("expectedExceptionEncountered2")
        assertTestSuccess("expectedExceptionEncountered3")
        assertTestSuccess("expectedExceptionEncountered4")
        assertTestSuccess("undeclaredExpectedCheckedException")
        assertTestSuccess("expectedRuntimeException")
        assertTestSuccess("expectedError")

        assertTestFailure("expectedExceptionNotEncountered1",
                "Expected one of the following exceptions to be thrown from test method " +
                "expectedExceptionNotEncountered1: [MyException]")

        assertTestFailure("expectedExceptionNotEncountered2",
                "Expected one of the following exceptions to be thrown from test method " +
                "expectedExceptionNotEncountered2: [MyException]")

        assertTestFailure("unexpectedRuntimeException1",
                "Exception generated from test method: unexpectedRuntimeException1")

        assertTestFailure("unexpectedRuntimeException2",
                "Exception generated from test method: unexpectedRuntimeException2")

        assertTestFailure("unexpectedCheckedException",
                "Exception generated from test method: unexpectedCheckedException")
    }

    private void gatherTestCases(final String testClass) throws CloverException {
        testcases = Maps.newHashMap()
        currentTestClass = testClass
        final CloverDatabase cdb = new CloverDatabase(initstring)
        assertNotNull(cdb)

        final CoverageData cd = cdb.loadCoverageData()
        for (final TestCaseInfo tci : cd.getTests()) {
            testcases.put(tci.getQualifiedName(), tci)
        }
    }

    private TestCaseInfo getTestCaseInfo(final String test) {
        final TestCaseInfo tci = testcases.get(currentTestClass + "." + test)
        assertNotNull(tci)
        return tci
    }

    private void assertTestSuccess(final String test) {
        assertTrue(getTestCaseInfo(test).isSuccess())
    }

    private void assertTestFailure(final String test, final String failmsg) {
        final TestCaseInfo tci = getTestCaseInfo(test)
        assertTrue(tci.isFailure())
        assertEquals(failmsg, tci.getFailMessage())
    }

    void tearDown() throws Exception {

        getProject().executeTarget("tearDown")
        IOHelper.delete(util.getWorkDir())

        super.tearDown()
    }
}
