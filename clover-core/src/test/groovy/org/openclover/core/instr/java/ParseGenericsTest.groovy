package org.openclover.core.instr.java

import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig
import org.openclover.core.cfg.instr.java.SourceLevel
import org.openclover.buildutil.testutils.IOHelper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.junit.Assert.assertTrue

class ParseGenericsTest {
    public static final String CLOVER_CORE_TEST_INSTR_DIR = "clover-core/src/test/resources/com/atlassian/clover/instr/java/"
    private File mProjDir
    private File mTestcasesSrcDir

    @Rule
    public TestName testName = new TestName()

    @Before
    void setUp() throws Exception {
        mProjDir = IOHelper.getProjectDir()
        mTestcasesSrcDir = new File(mProjDir, CLOVER_CORE_TEST_INSTR_DIR)
        assertTrue(mTestcasesSrcDir.isDirectory())
    }

    @Test
    void testGenericParsing() throws Exception {
        checkParsing([
            // generic class
            "class B<TA,TB> {}",
            "class B<TA, TB extends java.util.List<TA>> {}"
        ] as String[])
    }

    @Test
    void testFileTestcase() throws Exception {
        assertSourceOkay("GenericsTestcase.java.txt")
        assertSourceOkay("GenericsTestcase-2_2.java.txt")
    }

    static class TestInstrumentationSource implements InstrumentationSource {
        private File sourceFile
        private File mTestcasesSrcDir
        private String srcName

        TestInstrumentationSource(File sourceFile, File mTestcasesSrcDir, String srcName) {
            this.sourceFile = sourceFile
            this.mTestcasesSrcDir = mTestcasesSrcDir
            this.srcName = srcName
        }

        File getSourceFileLocation() {
            return sourceFile
        }

        Reader createReader() throws IOException {
            return new InputStreamReader(new FileInputStream(new File(mTestcasesSrcDir, srcName)))
        }
    }

    private void assertSourceOkay(final String srcName) throws Exception {
        // generate temporary location for a database
        final File coverageDbFile = File.createTempFile(testName.methodName, ".tmp")
        coverageDbFile.delete()
        String initString = coverageDbFile.getAbsolutePath()
        System.err.println("mTestcasesSrcDir.getAbsolutePath() = " + mTestcasesSrcDir.getAbsolutePath())

        // create fake temporary source file
        final File sourceFile = File.createTempFile(testName.methodName, "tmp")

        // parse and instrument source file
        final InstrumentationSource source = new TestInstrumentationSource(sourceFile, mTestcasesSrcDir, srcName)
        parseFile(initString, source)

        // delete fake file
        sourceFile.delete()

        // delete database created during instrumentation
        coverageDbFile.delete()
    }

    // array of {input, expected output} - to prevent the prefix of the string being compared,
    // mark the start point for comparison with ST_POINT
    private void checkParsing(final String[] testcases) throws Exception
    {
        File coverageDbFile = File.createTempFile(testName.methodName, ".tmp")
        coverageDbFile.delete()
        String initString = coverageDbFile.getAbsolutePath()

        for (final String sourceContent : testcases) {
            final File sourceFile = File.createTempFile(testName.methodName, "tmp")
            final InstrumentationSource source = new StringInstrumentationSource(sourceFile, sourceContent)
            parseFile(initString, source)
            sourceFile.delete()
        }

        coverageDbFile.delete()
    }

    private void parseFile(String initString, final InstrumentationSource ins) throws Exception {
        final JavaInstrumentationConfig cfg = new JavaInstrumentationConfig()
        cfg.setInitstring(initString)
        cfg.setProjectName(testName.methodName)
        cfg.setSourceLevel(SourceLevel.JAVA_8)
        final StringWriter out = new StringWriter()
        final Instrumenter instr = new Instrumenter(cfg)
        instr.startInstrumentation()
        instr.instrument(ins, out, null)
        instr.endInstrumentation()
    }

}
