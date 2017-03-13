package com.atlassian.clover

import org.apache.tools.ant.util.JavaEnvUtils

/**
 * The purpose of this test is to
 * <li>make sure the code compiles under JDK1.5 or later</li>
 * <li>make sure that when that code is instrumented, it still compiles</li>
 */
class JavaSyntax15CompilationTest extends JavaSyntaxCompilationTestBase {

    void testCompilation_15() throws Exception {
        assertTrue("this test requires at least jdk1.5",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_1_5))

        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.5")
        compileSources(srcDir, JavaEnvUtils.JAVA_1_5)
    }

    /**
     * Test java 1.5 language features and clover handles them.
     *
     * @throws Exception
     */
    void testInstrumentationAndCompilation_15() throws Exception {
        assertTrue("this test requires at least jdk1.5",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_1_5))

        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.5")
        instrumentAndCompileSources(srcDir, JavaEnvUtils.JAVA_1_5)

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

