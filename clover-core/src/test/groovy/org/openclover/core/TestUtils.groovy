package org.openclover.core

import com.atlassian.clover.api.registry.SourceInfo
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.instr.InstrumentationSessionImpl
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.FixedSourceRegion
import com.atlassian.clover.registry.entities.FullMethodInfo
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import org.openclover.buildutil.testutils.IOHelper
import com.atlassian.clover.util.FileUtils
import org_openclover_runtime.Clover
import org_openclover_runtime.CoverageRecorder
import org.openclover.runtime.ErrorInfo

import static org.junit.Assert.assertTrue

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

    static File createEmptyDirFor(Class test, String methodName) throws IOException {
        final File projectDir = IOHelper.getProjectDir()
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

    static void runTestMethod(CoverageRecorder recorder, String className, int testId, FullMethodInfo testMethod,
                              FullMethodInfo[] coveredAppMethods, long start, long end) {
        runTestMethod(recorder, className, testId, testMethod, coveredAppMethods, start, end, null)
    }

    static void runTestMethod(CoverageRecorder recorder, String className, int testId, FullMethodInfo testMethod,
                              FullMethodInfo[] coveredAppMethods, long start, long end, ErrorInfo errorInfo) {
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

}
