package org.openclover.core.util

import junit.framework.TestCase
import org.openclover.buildutil.testutils.IOHelper
import org.openclover.core.CloverDatabase
import org.openclover.core.CloverTestFixture
import org.openclover.core.CoverageDataSpec
import org.openclover.core.optimization.Snapshot
import org.openclover.runtime.CloverNames
import org.openclover.runtime.api.CloverException

import java.lang.reflect.InvocationTargetException

import static org.openclover.core.util.Lists.newArrayList

class CloverUtilsTest extends TestCase {

    private File tmpDir = null
    private CloverTestFixture fixture = null

    CloverUtilsTest(String testName) {
        super(testName)
    }

    void setUp() throws Exception {
        tmpDir = IOHelper.createTmpDir("tmp")
        fixture = new CloverTestFixture(tmpDir)
    }

    void tearDown() throws Exception {
        IOHelper.delete(tmpDir)
    }

    void testScrubCoverageDataA() throws Exception {
        // create a coverage database.

        File coverageDb = createCoverage(true, true, false)
        String initString = coverageDb.getAbsolutePath()

        assertTrue(CloverUtils.scrubCoverageData(initString, false))
        assertTrue(coverageDb.exists())
        assertTrue(getCoverageRecordings(coverageDb).length == 0)

        assertTrue(CloverUtils.scrubCoverageData(initString, true))
        assertTrue(!coverageDb.exists())
    }

    void testScrubCoverageDataB() throws Exception {
        // create a coverage database.

        File coverageDb = createCoverage(true, false, false)
        String initString = coverageDb.getAbsolutePath()

        assertTrue(CloverUtils.scrubCoverageData(initString, false))
        assertTrue(coverageDb.exists())
        assertTrue(getCoverageRecordings(coverageDb).length == 0)

        assertTrue(CloverUtils.scrubCoverageData(initString, true))
        assertTrue(!coverageDb.exists())
    }

    void testScrubCoverageDataC() throws Exception {
        // create a coverage database.

        File coverageDb = createCoverage(false, true, false)
        String initString = coverageDb.getAbsolutePath()

        assertTrue(CloverUtils.scrubCoverageData(initString, false))
        assertTrue(!coverageDb.exists())
        assertTrue(getCoverageRecordings(coverageDb).length == 0)

        assertTrue(CloverUtils.scrubCoverageData(initString, true))
        assertTrue(!coverageDb.exists())
    }

    void testScrubCoverageDataD() throws Exception {
        // create a coverage database.

        File coverageDb = createCoverage(true, true, true)
        String initString = coverageDb.getAbsolutePath()

        assertTrue(CloverUtils.scrubCoverageData(initString, false, false, true))
        assertTrue(new File(Snapshot.fileNameForInitString(initString)).exists())
    }

    void testScrubCoverageDataE() throws Exception {
        // create a coverage database.

        File coverageDb = createCoverage(true, true, true)
        String initString = coverageDb.getAbsolutePath()

        assertTrue(CloverUtils.scrubCoverageData(initString, false, true, false))
        assertTrue(!new File(Snapshot.fileNameForInitString(initString)).exists())
    }

    void testScrubCoverageDataF() throws Exception {
        // create a coverage database.

        final File coverageDb = createCoverage(true, true, true)
        final String initString = coverageDb.getAbsolutePath()
        final File liveRecFile = new File(initString + CloverNames.LIVEREC_SUFFIX)
        liveRecFile.createNewFile()

        assertTrue(CloverUtils.scrubCoverageData(initString, false, false, false))
        assertTrue(!liveRecFile.exists())
    }

    void testTransformStackTrace() {
        String transformedTrace =
                CloverUtils.transformStackTrace("NullPointerException: a null objected was referenced.\n"+
                "at com.cenqua.samples.money.MoneyTest.testBlahblah(MoneyTest.java:99)\n" +
                "at com.cenqua.samples.money.MoneyTest." + CloverUtils.SYNTH_TESTNAME_PREF+ "123456789abcdefghijklmno(MoneyTest.java:87)\n" +
                "at com.cenqua.samples.money.MoneyTest.testMoneyBagHash(MoneyTest.java:84)\n" +
                "at com.cenqua.samples.money.MoneyBag.someOtherMethod(MoneyTest.java:99)\n" +
                "at com.cenqua.samples.money.MoneyTest." + CloverUtils.SYNTH_TESTNAME_PREF+ "123456789abcdefghijklmno(MoneyTest.java:Unkown Source)\n" +
                "at com.cenqua.samples.money.MoneyTest.testMoneyBagHash(MoneyTest.java:184)\n" +
                "at com.cenqua.samples.money.MoneyTest." + CloverUtils.SYNTH_TESTNAME_PREF + "123456789abcdefghijklmno(MoneyTest.java:287)",
        true)

        String expectedOutput ="NullPointerException: a null objected was referenced.\n"+
                "at com.cenqua.samples.money.MoneyTest.testBlahblah(MoneyTest.java:99)\n" +                
                "at com.cenqua.samples.money.MoneyTest.testMoneyBagHash(MoneyTest.java:87)\n" +
                "at com.cenqua.samples.money.MoneyBag.someOtherMethod(MoneyTest.java:99)\n" +
                "at com.cenqua.samples.money.MoneyTest.testMoneyBagHash(MoneyTest.java:Unkown Source)\n" +
                "at com.cenqua.samples.money.MoneyTest." + CloverUtils.SYNTH_TESTNAME_PREF + "123456789abcdefghijklmno(MoneyTest.java:287)"

        assertEquals(expectedOutput, transformedTrace)
           
    }

    void testTransformNoTrace() {
        String message = 'this is just a \n multiline error message with a\n few curly characters\n in it.*#^&$*&@(<>:'
        String transformed = CloverUtils.transformStackTrace(message, true)
        assertEquals(message, transformed)
    }

    void testTransformStackTraceWithUnkownSource() {
        String line1 = "at moneybags.MoneyTest." + CloverUtils.SYNTH_TESTNAME_PREF+ "ltfhf65l(MoneyTest.java:Unknown Source)"
        String transformed = CloverUtils.transformStackTrace(line1, true)
        String expected = "at moneybags.MoneyTest." + CloverUtils.SYNTH_TESTNAME_PREF+ "ltfhf65l(MoneyTest.java:Unknown Source)"
        assertEquals(expected, transformed)

        String line2 = "at moneybags.MoneyTest.realTestName(MoneyTest.java:Unknown Source)"

        String t2 = CloverUtils.transformStackTrace(line1 + "\n" + line2, true)
        assertEquals(line2, t2)
    }


    void testTransformTraceWithNoSyntheticMethods() {
        String cleanTrace = "NullPointerException: a null objected was referenced.\n" +
                "at com.foo.bar(Bar.java:45)\n"
        String frameworkTrace =
                "at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\n" +
                "at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n" +
                "at java.lang.reflect.Method.invoke(Method.java:585)\n" +
                "at junit.framework.TestCase.runTest(TestCase.java:154)\n" +
                "at junit.framework.TestCase.runBare(TestCase.java:127)\n" +
                'at junit.framework.TestResult$1.protect(TestResult.java:106)\n' +
                "at junit.framework.TestResult.runProtected(TestResult.java:124)\n" +
                "at junit.framework.TestResult.run(TestResult.java:109)\n" +
                "at junit.framework.TestCase.run(TestCase.java:118)\n" +
                "at junit.framework.TestSuite.runTest(TestSuite.java:208)\n" +
                "at junit.framework.TestSuite.run(TestSuite.java:203)\n" +
                "at org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.run(JUnitTestRunner.java:421)\n" +
                "at org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.launch(JUnitTestRunner.java:912)\n" +
                "at org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(JUnitTestRunner.java:743)"

        String transformed = CloverUtils.transformStackTrace(cleanTrace+frameworkTrace, false)
        assertEquals(cleanTrace+frameworkTrace, transformed)
        String transformedFiltered = CloverUtils.transformStackTrace(cleanTrace, true)
        assertEquals(cleanTrace.trim(), transformedFiltered.trim())
    }

    void testTransformTraceWithCinit() {
        String trace1 = 'at moneybags.MoneyBag$Mint.<init>(MoneyBag.java:21)\n' +
                "at moneybags.MoneyTest." + CloverUtils.SYNTH_TESTNAME_PREF+ "ltfhf65l(MoneyTest.java:136)\n" +
                "at moneybags.MoneyTest.testLinkificationInReport(MoneyTest.java:135)"

        String transformed = CloverUtils.transformStackTrace(trace1, false)
        String expected = 'at moneybags.MoneyBag$Mint.<init>(MoneyBag.java:21)\n' +
                          "at moneybags.MoneyTest.testLinkificationInReport(MoneyTest.java:136)"
        assertEquals(expected, transformed.trim())
    }


    void testTransformTraceWithNoPackage() {
        String trace1 = 'at MoneyBag$Mint.<init>(MoneyBag.java:21)\n' +
                "at MoneyTest." + CloverUtils.SYNTH_TESTNAME_PREF+ "ltfhf65l(MoneyTest.java:136)\n" +
                "at MoneyTest.testLinkificationInReport(MoneyTest.java:135)"

        String transformed = CloverUtils.transformStackTrace(trace1, true)
        String expected = 'at MoneyBag$Mint.<init>(MoneyBag.java:21)\n' +
                          "at MoneyTest.testLinkificationInReport(MoneyTest.java:136)"
        assertEquals(expected, transformed.trim())
    }

    void testTransformCorruptTrace() {
        String trace1 = "TestException: \n" +
                "\tat test.test.test.Test.test" + CloverUtils.SYNTH_TESTNAME_PREF + "li1dchdtc\n" +
                "\tat test.test.test.Test.test(CloverReportTaskSanityTest.java:91)\n" +
                "Caused by: ExceptionConverter: java.lang.RuntimeException: 1 annotations had invalid placement pages."
                
        String transformed = CloverUtils.transformStackTrace(trace1, true)
        assertEquals(trace1, transformed)

        String trace2 = "TestException: \n" +
                        "\tat test.test.test.Test.test" + CloverUtils.SYNTH_TESTNAME_PREF + "li1dchdtc(Test.java:90)\n" +
                        "\tat test.test.test.Test.test\n" +
                        "Caused by: ExceptionConverter: java.lang.RuntimeException: 1 annotations had invalid placement pages."

        String transformed2 = CloverUtils.transformStackTrace(trace2, true)
    }

//    void testTransformTraceWithĂśmlautĂź() {
//        String trace1 = "at MoneyBĂ¤g$Mint.<init>(MoneyBĂ¤g.java:21)\n" +
//                "at MoneyTeĂźt." + CloverUtils.SYNTH_TESTNAME_PREF + "ltfhf65l(MoneyTeĂźt.java:136)\n" +
//                "at MoneyTeĂźt.testLinkificationInReport(MoneyTeĂźt.java:135)"
//
//        String transformed = CloverUtils.transformStackTrace(trace1, true)
//        String expected = "at MoneyBĂ¤g$Mint.<init>(MoneyBĂ¤g.java:21)\n" +
//                          "at MoneyTeĂźt.testLinkificationInReport(MoneyTeĂźt.java:136)"
//
//        assertEquals(expected, transformed.toString().trim())
//    }


    void testTransformTraceWithNullInput() {
        assertNull(CloverUtils.transformStackTrace(null, false))
    }


    static class Parent {
        protected void method() { }
        protected String function(String arg1, Integer arg2) { return arg1 + arg2; } 
    }

    static class Child extends Parent {
    }

    void testInvokeVirtual() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        ReflectionUtils.invokeVirtualImplicit("method", new Child())
        ReflectionUtils.invokeVirtualImplicit("method", new Parent())

        try {
            ReflectionUtils.invokeVirtualImplicit("method", new Object(), (Object[])new Class[0])
            fail("Object.method() does not exist, and no exception was thrown.")
        } catch (NoSuchMethodException e) {
        }
        
        assertEquals("arg11", ReflectionUtils.invokeVirtualImplicit("function", new Child(), "arg1", 1))
        assertEquals("arg11", ReflectionUtils.invokeVirtualImplicit("function", new Parent(), "arg1", 1))
    }
    

    private File createCoverage(boolean createDb, boolean createRecordings, boolean createSnapshot) throws IOException, CloverException {

        List<CloverTestFixture.Clazz> classList = newArrayList()
        CloverTestFixture.Clazz clazz = new CloverTestFixture.Clazz(tmpDir, "com.cenqua", "Test",new CloverTestFixture.Coverage(0.90f, 0.80f, 0.85f))
        classList.add(clazz)


        String initString = fixture.createCoverageDB()
        fixture.register(initString, classList)
        if (createRecordings) {
            fixture.write(initString, classList)
        }

        File coverageDatabase = new File(initString)
        if (!createDb) {
            coverageDatabase.delete()
        }

        if (createSnapshot) {
            Snapshot.generateFor(
                CloverDatabase.loadWithCoverage(initString, new CoverageDataSpec())).store()
        }

        assertEquals(createDb, coverageDatabase.exists())
        assertEquals(createRecordings, getCoverageRecordings(coverageDatabase).length > 0)
        assertEquals(createSnapshot, new File(Snapshot.fileNameForInitString(initString)).exists())

        return coverageDatabase
    }

    private File[] getCoverageRecordings(File coverageDb) {
        final String coverageDbName = coverageDb.getName()
        final File[] ls = coverageDb.getParentFile().listFiles(new FilenameFilter() {
            boolean accept(File dir, String name) {
                return name.startsWith(coverageDbName) && name.compareTo(coverageDbName) != 0
            }
        })
        return ls
    }
}
