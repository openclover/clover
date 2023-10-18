package com.atlassian.clover

import com.atlassian.clover.util.JavaEnvUtils
import org.junit.Before
import org.junit.Test

/**
 * The purpose of this test is to
 * a) make sure the code compiles under JDK1.7 or later
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax7CompilationTest extends JavaSyntaxCompilationTestBase {

    /** Regular expression for: __CLR_hash_code.R.inc(index) */
    protected final String R_INC = "__CLR[a-zA-Z0-9_]+\\.R\\.inc\\([0-9]+\\);"

    @Before
    void setUp() {
        setUpProject()
    }

    /**
     * Test java 1.7 language features and how Clover handles them.
     *
     * @throws Exception
     */
    @Test
    void testInstrumentationAndCompilation_17() throws Exception {
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.7")
        resetAntOutput()
        instrumentAndCompileSources(srcDir, JavaEnvUtils.JAVA_1_7)

        // StringInSwitch
        assertFileMatches("StringInSwitch.java", ".*case.*Saturday.*__CLR.*", false)

        // TryWithResources
        assertFileMatches("TryWithResources.java",
                ".*__CLR.*java.util.zip.ZipFile zf = new java.util.zip.ZipFile\\(zipFileName\\).*", false)
        assertFileMatches("TryWithResources.java",
                ".*__CLR.*java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter\\(outputFilePath, charset\\).*",
                false)

        // TypeInference
        assertFileMatches("TypeInference.java", ".*__CLR.*Map<String, List<String>> myMap = new HashMap<>\\(\\).*", false)
    }

    /**
     * Test java 1.7 language features and how Clover handles them.
     *
     * @throws Exception
     */
    @Test
    void testInstrumentationAndCompilation_17_NonReifiableTypes() throws Exception {
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.7")

        // We must compile each file separately in order to verify presence (or absence) of compilation warnings
        // Note: we must use -Xlint or -Xlint:unchecked compiler option

        // NonReifiableTypesHeapPollution
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, "NonReifiableTypesHeapPollution.java", JavaEnvUtils.JAVA_7)
        assertAntOutputContains(".*\\[unchecked\\] Possible heap pollution from parameterized vararg type T.*", false)
        assertAntOutputContains(".*\\[unchecked\\] unchecked generic array creation for varargs parameter of type List<String>\\[\\].*", false)

        // NonReifiableTypesSafeVarargs
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, "NonReifiableTypesSafeVarargs.java", JavaEnvUtils.JAVA_7)
        assertAntOutputContains(".*\\[unchecked\\].*", true); // i.e. no warnings

        // NonReifiableTypesSuppressWarnings
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, "NonReifiableTypesSuppressWarnings.java", JavaEnvUtils.JAVA_7)
        assertAntOutputContains(".*\\[unchecked\\] unchecked generic array creation for varargs parameter of type List<String>\\[\\].*", false)
    }

    void testRecordIsNotReservedKeyword() {
        final File srcDir = new File(mTestcasesSrcDir, "javasyntax1.7")
        final String fileName = "RecordIsNotReservedKeyword.java"

        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_7)
        assertFileMatches(fileName, R_INC + "System.out.println(record)", false)
    }

}

