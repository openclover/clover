package com.atlassian.clover

import com.atlassian.clover.api.registry.SourceInfo
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.instr.InstrumentationSessionImpl
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.FixedSourceRegion
import com.atlassian.clover.registry.entities.FullMethodInfo
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import com.atlassian.clover.util.FileUtils
import com_atlassian_clover.Clover
import com_atlassian_clover.CoverageRecorder

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class TestUtils {

    private static long checksum = 0L

    static long newChecksum() {
        checksum++
    }

    /**
     * Concatenates a list of lines passed as separate Strings into a single String, putting a line separator
     * (as indicated by system property 'line.separator') between them.
     *
     * @param lastLineWithSeparator  true if newline character shall be put after the last line as well
     * @param lines                  list of lines to bo concatenated
     * @return String
     */
    static String concatWithLineSeparator(boolean lastLineWithSeparator, String... lines) {
        String separator = System.getProperty("line.separator", "\n")
        StringBuilder out = new StringBuilder()
        for (int i = 0; i < lines.length; i++) {
            out.append(lines[i])
            if ( (i < lines.length - 1) || lastLineWithSeparator ){
                out.append(separator)
            }
        }

        out.toString()
    }

    static File createEmptyDirFor(Class test) throws IOException {
        createEmptyDirFor(test, null)
    }

    /**
     * Read the 'project.dir' system property, assert that it's not null and points to a workspace directory.
     */
    static File getProjectDirFromProperty() {
        final String PROJECT_DIR = "project.dir"
        final String projectDir = System.getProperty(PROJECT_DIR)
        assertNotNull("The '" + PROJECT_DIR + "' property is not set. It must point to the Clover's workspace root",
                projectDir)
        assertTrue("The location pointed by '" + PROJECT_DIR + "' is not a directory",
                new File(projectDir).isDirectory())
        assertTrue("The location pointed by '" + PROJECT_DIR + "' does not seem to be a Clover workspace directory",
                new File(projectDir, "common.xml").isFile())

        new File(projectDir)
    }

    static File createEmptyDirFor(Class test, String methodName) throws IOException {
        final File projectDir = getProjectDirFromProperty()
        final String testTmpDir = FileUtils.getPlatformSpecificPath(projectDir.getAbsolutePath() + "/clover-core/target/testrun/tmp/")
        final File tempDir = FileUtils.createEmptyDir(
                new File(testTmpDir),
                test.getName() + (methodName != null ? "_" + methodName : ""))
        assertTrue(tempDir.isDirectory())

        tempDir
    }

    static FullMethodInfo addClassWithSingleMethod(InstrumentationSessionImpl session, ContextSet context, String pkg, long timestamp, long fileSize, String clazzName, String methodName, boolean isTest) {
        SourceInfo region = new FixedSourceRegion(0, 0)
        session.enterFile(pkg, new File(pkg.replace('.', '/' ) + "/" + clazzName + ".java"), 0, 0, timestamp, fileSize, newChecksum())
        session.enterClass(clazzName, region, new Modifiers(), false, false, false)
        FullMethodInfo method = session.enterMethod(context, region, new MethodSignature(methodName), isTest)
        session.addStatement(context, region, 0)
        session.exitMethod(0, 0)
        session.exitClass(0, 0)
        session.exitFile()
        method
    }

    static void runTestMethod(CoverageRecorder recorder, String className, int testId, FullMethodInfo testMethod, FullMethodInfo[] coveredAppMethods, long start, long end) {
        runTestMethod(recorder, className, testId, testMethod, coveredAppMethods, start, end, null)
    }

    static void runTestMethod(CoverageRecorder recorder, String className, int testId, FullMethodInfo testMethod, FullMethodInfo[] coveredAppMethods, long start, long end, ErrorInfo errorInfo) {
        recorder.sliceStart(className, start, testMethod.getDataIndex(), testId)
        //Recorder test method invocation + coverage of its only statement
        recorder.inc(testMethod.getDataIndex())
        recorder.inc(testMethod.getDataIndex() + 1)
        for (FullMethodInfo coveredAppMethod : coveredAppMethods) {
            //Recorder method invocation + coverage of its only statement
            recorder.inc(coveredAppMethod.getDataIndex())
            recorder.inc(coveredAppMethod.getDataIndex() + 1)
        }
        recorder.sliceEnd(className, testMethod.getSimpleName(), testMethod.getSimpleName() + "@runtime",
                end, testMethod.getDataIndex(), 0, errorInfo == null ? 1 : 0, errorInfo)
        recorder.forceFlush()
    }

    static void runTestMethod(CoverageRecorder recorder, String className, int testID, FullMethodInfo testMethod, FullMethodInfo[] coveredAppMethods) {
        long start = System.currentTimeMillis()
        runTestMethod(recorder, className, testID, testMethod, coveredAppMethods, start, start + 100)
    }

    static CoverageRecorder newRecorder(Clover2Registry registry) {
        Clover.getRecorder(registry.getRegistryFile().getAbsolutePath(),
                registry.getVersion(), 0, registry.getProject().getDataLength(), null, null)
    }

    /**
     * Read file contents into a String.
     * @param inputFile file to be read
     * @return String - file content
     */
    private static String readFile(final File inputFile) {
        final int BUF_SIZE = 8000
        final char[] buffer = new char[BUF_SIZE]
        final StringBuilder out = new StringBuilder()

        try {
            int charsRead
            final Reader fileReader = new BufferedReader(new FileReader(inputFile))

            while ( (charsRead = fileReader.read(buffer, 0, BUF_SIZE)) != -1 ) {
                out.append(buffer, 0, charsRead)
            }
            fileReader.close()
        } catch (IOException ex) {
            fail(ex.toString())
        }

        out.toString()
    }

    /**
     * Check whether string contains substring.
     * @param expectedSubstring substring we look for
     * @param actualString      string to be searched
     * @param negate            <code>false</code>=fail if not found, <code>true</code>=fail if found
     */
    static void assertStringContains(String expectedSubstring, String actualString, boolean negate) {
        if (!negate) {
            assertTrue("A substring \n'" + expectedSubstring + "'\n was not found in \n'" + actualString + "'",
                    actualString.contains(expectedSubstring))
        } else {
            assertFalse("A substring \n'" + expectedSubstring + "'\n was found in \n'" + actualString + "'.",
                    actualString.contains(expectedSubstring))
        }
    }

    /**
     * Check whether string matches regular expression.
     * @param regExp         regular expression we look for
     * @param actualString   string to be searched
     * @param negate         <code>false</code>=fail if not found, <code>true</code>=fail if found
     */
    static void assertStringMatches(String regExp, String actualString, boolean negate) {
        final Pattern pattern = Pattern.compile(regExp)
        final Matcher matcher = pattern.matcher(actualString)
        if (!negate) {
            assertTrue("A pattern \n'" + regExp + "'\n was not found in \n'" + actualString + "'.",
                    matcher.find())
        } else {
            assertFalse("A pattern \n'" + regExp + "'\n was found in \n'" + actualString + "'.",
                    matcher.find())
        }
    }

    /**
     * Check whether file content contains substring.
     * @param substring         substring we look for
     * @param actualFile        file to be searched
     * @param negate            <code>false</code>=fail if not found, <code>true</code>=fail if found
     */
    static void assertFileContains(String substring, File actualFile, boolean negate) {
        final String fileContent = readFile(actualFile)
        if (!negate) {
            assertTrue("A substring '" + substring + "' was not found in file '" + actualFile.getAbsolutePath() + "'.",
                    fileContent.contains(substring))
        } else {
            assertFalse("A substring '" + substring + "' was found in file '" + actualFile.getAbsolutePath() + "'.",
                    fileContent.contains(substring))
        }
    }

    /**
     * Check whether file content matches regular expression.
     * @param regExp         regular expression we look for
     * @param actualFile     file to be searched
     * @param negate         <code>false</code>=fail if not found, <code>true</code>=fail if found
     */
    static void assertFileMatches(String regExp, File actualFile, boolean negate) {
        final String fileContent = readFile(actualFile)
        final Pattern pattern = Pattern.compile(regExp)
        final Matcher matcher = pattern.matcher(fileContent)
        if (!negate) {
            assertTrue("A pattern '" + regExp + "' was not found in file '" + actualFile.getAbsolutePath() + "'.",
                    matcher.find())
        } else {
            assertFalse("A pattern '" + regExp + "' was found in file '" + actualFile.getAbsolutePath() + "'.",
                    matcher.find())
        }
    }
}
