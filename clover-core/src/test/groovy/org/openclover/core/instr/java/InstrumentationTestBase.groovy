package org.openclover.core.instr.java

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import org.openclover.core.cfg.instr.InstrumentationLevel
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig
import org.openclover.core.cfg.instr.java.LambdaInstrumentation
import org.openclover.core.cfg.instr.java.SourceLevel
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.metrics.ProjectMetrics
import org.openclover.core.util.FileUtils
import org.openclover.runtime.CloverNames
import org_openclover_runtime.CoverageRecorder
import org_openclover_runtime.TestNameSniffer

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertEquals

class InstrumentationTestBase {
    protected File workingDir

    protected String snifferField = "public static final " + TestNameSniffer.class.getName() + " SNIFFER=" +
            TestNameSniffer.class.getName() + ".NULL_INSTANCE;"

    @Rule
    public TestName name = new TestName()

    @Before
    void setUp()
            throws Exception {
        workingDir = File.createTempFile(getClass().getName() + "." + name, ".tmp")
        workingDir.delete()
        workingDir.mkdir()
    }

    @After
    void tearDown()
            throws Exception {
        FileUtils.deltree(workingDir)
    }

    protected void checkStatement(String test, String expected, int instrumentationlevel) throws Exception {
        JavaInstrumentationConfig config = getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false)
        config.setInstrLevel(instrumentationlevel)
        checkInstrumentation([
            ["class B { private void a(int arg) {" + test + "}}",
             "class B {" + snifferField + " private void a(int arg) {RECORDER.inc(0);" + expected + "}}"]
        ] as String[][], config)
    }

    protected void checkStatement(String test, String expected) throws Exception {
        checkStatement(test, expected, InstrumentationLevel.STATEMENT.ordinal())
    }

    // array of {input, expected output} - to prevent the prefix of the string being compared,
    // mark the start point for comparison with ST_POINT
    protected void checkInstrumentation(String[][] testcases, boolean testRewriting) throws Exception {
        checkInstrumentation("", testcases, testRewriting)
    }

    protected void checkInstrumentation(String[][] testcases) throws Exception {
        checkInstrumentation("", testcases, true)
    }

    // check array of {input, expected output} - replacing the recorder member with recorderStr.
    protected void checkInstrumentation(String recorderStr, String[][] testcases, boolean testRewriting) throws Exception {
        for (String[] testcase : testcases) {
            String instr = getInstrumentedVersion(testcase[0], testRewriting)
            checkStringSuffix(recorderStr, CloverTokenStreamFilter.MARKER + testcase[1], instr)
        }
    }

    // check array of {input, expected output} - replacing the recorder member with recorderStr.
    protected void checkInstrumentation(String[][] testcases, JavaInstrumentationConfig config) throws Exception {
        for (String[] testcase : testcases) {
            String instr = getInstrumentedVersion(testcase[0], config)
            checkStringSuffix("", CloverTokenStreamFilter.MARKER + testcase[1], instr)
        }
    }

    private static final String SNIFFER_REGEX = CloverNames.CLOVER_PREFIX + "[_0-9]+_TEST_NAME_SNIFFER"
    private static final String RECORDER_REGEX = CloverNames.CLOVER_PREFIX + "[_A-Za-z0-9]+"
    private static final String CLR_REGEX = CloverNames.CLOVER_PREFIX
    private static final String RECORDER_INNER_MEMBER_REGEX = "public static " + CoverageRecorder.class.getName() + " " + RECORDER_REGEX + "=[^;]+;"

    private static void checkStringSuffix(String recorder, String s1, String s2) {
        String t2 = s2.replaceAll(SNIFFER_REGEX, "SNIFFER")
                      .replaceAll(RECORDER_INNER_MEMBER_REGEX, recorder)
                      .replaceAll(RECORDER_REGEX, "RECORDER")
                      .replaceAll(CLR_REGEX, "CLR")
        assertThat(t2, equalTo(s1))
    }

    protected String getInstrumentedVersion(String input, boolean testRewriting) throws Exception {
        File coverageDbFile = newDbTempFile()
        coverageDbFile.delete()
        return getInstrumentedVersion(coverageDbFile.getAbsolutePath(), false, input, testRewriting)
    }

    protected File newDbTempFile() throws IOException {
        File tempFile = File.createTempFile(getClass().getName() + "." + name, ".tmp", workingDir)
        tempFile.delete()
        return tempFile
    }

    protected String getInstrumentedVersion(String initString, boolean relativeIS, String input) throws Exception {
        return getInstrumentedVersion(initString, relativeIS, input, true)
    }

    protected String getInstrumentedVersion(String initString, boolean relativeIS, String input, boolean testRewriting) throws Exception {
        JavaInstrumentationConfig cfg = getInstrConfig(initString, relativeIS, testRewriting, false)
        return getInstrumentedVersion(input, cfg)
    }

    protected String getInstrumentedVersion(final String sourceCode, final JavaInstrumentationConfig cfg) throws Exception {
        final File tempFile = newDbTempFile()
        final StringWriter out = new StringWriter()
        final InstrumentationSource input = new StringInstrumentationSource(tempFile, sourceCode)

        performInstrumentation(cfg, input, out)
        tempFile.delete()

        return out.toString()
    }

    protected JavaInstrumentationConfig getInstrConfig(String initString, boolean relativeIS, boolean testRewriting, boolean classInstrStrategy) {
        JavaInstrumentationConfig cfg = new JavaInstrumentationConfig()
        cfg.setDefaultBaseDir(workingDir)
        if (initString != null) {
            cfg.setInitstring(initString)
        }
        cfg.setRelative(relativeIS)
        cfg.setProjectName(name.toString())
        cfg.setSourceLevel(SourceLevel.JAVA_8)
        cfg.setReportInitErrors(false)
        cfg.setRecordTestResults(testRewriting)
        cfg.setEncoding("ISO-88591")
        cfg.setInstrumentLambda(LambdaInstrumentation.ALL)
        return cfg
    }

    protected Clover2Registry checkMetrics(JavaInstrumentationConfig instrConfig, String src, int numClasses, int numMethods, int numStatements, int numBranches, int totalComplexity) throws Exception {
        final InstrumentationSource input = new StringInstrumentationSource(newDbTempFile(), src)
        final Clover2Registry registry = performInstrumentation(instrConfig,
                input, new StringWriter())
        final ProjectMetrics pm = (ProjectMetrics) registry.getProject().getMetrics()

        assertEquals("num classes",numClasses, pm.getNumClasses())
        assertEquals("num methods",numMethods, pm.getNumMethods())
        assertEquals("num statements", numStatements, pm.getNumStatements())
        assertEquals("num branches", numBranches, pm.getNumBranches())
        assertEquals("total complexity", totalComplexity, pm.getComplexity())

        return registry
    }

    /**
     * convenience method that instruments but discards the instrumentation output
     */
    protected Clover2Registry performInstrumentation(final String sourceCode) throws Exception {
        final InstrumentationSource input = new StringInstrumentationSource(newDbTempFile(), sourceCode)
        return performInstrumentation(getInstrConfig(null, false, true, false), input, new StringWriter())
    }

    protected static Clover2Registry performInstrumentation(final JavaInstrumentationConfig cfg, final InstrumentationSource input,
                                                            final Writer out) throws Exception {
        final Instrumenter instr = new Instrumenter(cfg)
        instr.startInstrumentation()
        instr.instrument(input, out, null)
        return instr.endInstrumentation()
    }
}
