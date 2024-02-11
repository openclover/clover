package org.openclover.core

import org.openclover.core.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

/**
 * The purpose of this test is to
 * <li>make sure the code compiles under JDK1.5 or later</li>
 * <li>make sure that when that code is instrumented, it still compiles</li>
 */
class JavaSyntax5CompilationTest extends JavaSyntaxCompilationTestBase {

    @Before
    void setUp() {
        setUpProject()
    }

    @Test
    void testCompilation_15() throws Exception {
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.5")
        compileSources(srcDir, JavaEnvUtils.JAVA_8)
    }

    /**
     * Test java 1.5 language features and clover handles them.
     *
     * @throws Exception
     */
    @Test
    void testInstrumentationAndCompilation_15() throws Exception {
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.5")
        instrumentAndCompileSources(srcDir, JavaEnvUtils.JAVA_8)

        String[] testCaseMainClasses = [
                "coverage.enums.EnumTests",
                "coverage.metadata.TestCases",
        ]
        executeMainClasses(testCaseMainClasses)

        // assert metrics
        assertMethodCoverage("coverage.enums.E1", 13)

        assertStatementCoverage("coverage.enums.E2", 8, 1)
        assertMethodCoverage("coverage.enums.E2", 14, 2)

        assertMethodCoverage("coverage.enums.E3", 7)

        //TODO remove this once 1.5 goes final?
//        assertMethodCoverage("coverage.metadata.Annot2", 8)
//        assertMethodCoverage("coverage.metadata.Annot2", 16)

        assertMethodCoverage("coverage.metadata.DeprecatedTest", 5)
    }

}

