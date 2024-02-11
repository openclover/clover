package org.openclover.core

import com.atlassian.clover.CoverageData
import com.atlassian.clover.CoverageDataCollator
import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.ProgressListener
import com.atlassian.clover.instr.InstrumentationSessionImpl
import com.atlassian.clover.recorder.PerTestCoverageStrategy
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.entities.FullMethodInfo
import com.atlassian.clover.registry.metrics.HasMetricsFilter
import com.atlassian.clover.util.SimpleCoverageRange
import org_openclover_runtime.CoverageRecorder
import junit.framework.TestCase

import java.lang.reflect.Field

class CoverageDataCollatorTest extends TestCase {
    void testCoverageDeletionForOldCoverage() throws Exception {
        testMaybeDeleteOldCoverage(true)
    }

    void testNoCoverageDeletionForOldCoverage() throws Exception {
        testMaybeDeleteOldCoverage(false)
    }

    private void testMaybeDeleteOldCoverage(boolean delete) throws Exception {
        final File recDir = TestUtils.createEmptyDirFor(getClass(), getName())
        final File regFile = new File(recDir, getName())

        final CoverageDataSpec spec =
            new CoverageDataSpec(
                HasMetricsFilter.ACCEPT_NONE, 0, delete, false,
                false, true, PerTestCoverageStrategy.IN_MEMORY)

        final Clover2Registry reg = new Clover2Registry(regFile, getName())

        InstrumentationSessionImpl session = (InstrumentationSessionImpl) reg.startInstr()
        final FullMethodInfo bar_it = TestUtils.addClassWithSingleMethod(session, new ContextSet(), "com.foo", System.currentTimeMillis(), 0l, "Bar", "void it()", false)
        final FullMethodInfo barTest_testIt = TestUtils.addClassWithSingleMethod(session, new ContextSet(), "com.foo", System.currentTimeMillis(), 0l, "BarTest", "void testIt()", true)
        session.finishAndApply()
        reg.saveAndOverwriteFile()

        elapse200ms()

        final CoverageRecorder recorder = TestUtils.newRecorder(reg)
        TestUtils.runTestMethod(recorder, "FooTest", 0, barTest_testIt, [ bar_it ] as FullMethodInfo[])

        elapse200ms()

        session = (InstrumentationSessionImpl) reg.startInstr()
        TestUtils.addClassWithSingleMethod(session, new ContextSet(), "com.foo", System.currentTimeMillis(), 0l, "Baz", "void it()", false)
        session.finishAndApply()
        reg.saveAndOverwriteFile()

        final CoverageDataCollator collator = new CoverageDataCollator(reg)
        final CoverageData data = collator.loadCoverageData(spec, ProgressListener.NOOP_LISTENER)

        assertTrue(data.isEmpty())
        assertEquals(0, data.getHitCount(0))
        assertEquals(0, data.getTestsCovering(new SimpleCoverageRange(0, 1)).size())

        final String[] filesInRegDir = recDir.list()
        for (String fileName : filesInRegDir) {
            final boolean isRecording = !fileName.endsWith(regFile.getName()) &&
                        fileName.contains(regFile.getName()) &&
                        !fileName.contains(regFile.getName() + ".")
            if (isRecording && delete) {
                fail("Recording file was not deleted: " + fileName)
            }
        }
    }

    void testNoDeletionForCurrentCoverage() throws Exception {
        final File recDir = TestUtils.createEmptyDirFor(getClass(), getName())
        final File regFile = new File(recDir, getName())

        final CoverageDataSpec spec =
            new CoverageDataSpec(
                HasMetricsFilter.ACCEPT_NONE, 0, true, false,
                false, true, PerTestCoverageStrategy.IN_MEMORY)


        final Clover2Registry reg = new Clover2Registry(regFile, getName())

        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) reg.startInstr()
        final FullMethodInfo bar_it = TestUtils.addClassWithSingleMethod(session, new ContextSet(), "com.foo", System.currentTimeMillis(), 0l, "Bar", "void it()", false)
        final FullMethodInfo barTest_testIt = TestUtils.addClassWithSingleMethod(session, new ContextSet(), "com.foo", System.currentTimeMillis(), 0l, "BarTest", "void testIt()", true)
        session.finishAndApply()
        reg.saveAndOverwriteFile()

        elapse200ms()

        final CoverageRecorder recorder = TestUtils.newRecorder(reg)
        TestUtils.runTestMethod(recorder, "FooTest", 0, barTest_testIt, [ bar_it ] as FullMethodInfo[])

        elapse200ms()

        final CoverageDataCollator collator = new CoverageDataCollator(reg)
        final CoverageData data = collator.loadCoverageData(spec, ProgressListener.NOOP_LISTENER)

        assertTrue(!data.isEmpty())
        assertEquals(1, data.getHitCount(0))
        assertEquals(1, data.getTestsCovering(new SimpleCoverageRange(0, 1)).size())

        boolean coverageFilesFound = false
        final String[] filesInRegDir = recDir.list()
        for (String fileName : filesInRegDir) {
            coverageFilesFound |= !fileName.endsWith(regFile.getName()) && fileName.contains(regFile.getName())
        }

        if (!coverageFilesFound) {
            fail("Recording file was deleted.")
        }
    }

    void testOverflowCorrection() throws Exception {
        final File recDir = TestUtils.createEmptyDirFor(getClass(), getName())
        final File regFile = new File(recDir, getName())

        final CoverageDataSpec spec =
            new CoverageDataSpec(
                HasMetricsFilter.ACCEPT_NONE, 0, false, false,
                false, true, PerTestCoverageStrategy.IN_MEMORY)

        final Clover2Registry reg = new Clover2Registry(regFile, getName())

        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) reg.startInstr()
        TestUtils.addClassWithSingleMethod(session, new ContextSet(), "com.foo", System.currentTimeMillis(), 0l, "Bar", "void it()", false)
        TestUtils.addClassWithSingleMethod(session, new ContextSet(), "com.foo", System.currentTimeMillis(), 0l, "Baz", "void it()", false)
        TestUtils.addClassWithSingleMethod(session, new ContextSet(), "com.foo", System.currentTimeMillis(), 0l, "Bat", "void it()", false)
        session.finishAndApply()
        reg.saveAndOverwriteFile()

        elapse200ms()

        final CoverageRecorder recorder = TestUtils.newRecorder(reg)

        final Field elements = recorder.getClass().getDeclaredField("elements")
        elements.setAccessible(true)
        ((int[])elements.get(recorder))[0] = Integer.MIN_VALUE
        ((int[])elements.get(recorder))[1] = -1
        ((int[])elements.get(recorder))[2] = 0
        recorder.forceFlush()

        elapse200ms()

        final CoverageDataCollator collator = new CoverageDataCollator(reg)
        final CoverageData data = collator.loadCoverageData(spec, ProgressListener.NOOP_LISTENER)

        assertTrue(!data.isEmpty())
        assertEquals(Integer.MAX_VALUE, data.getHitCount(0))
        assertEquals(Integer.MAX_VALUE, data.getHitCount(1))
        assertEquals(0, data.getHitCount(2))
    }

    /**
     * Our code expects at least 1ms will have elapsed between instrumentation and execution
     * of instrumented code. Not an unreasonable expectation. But it screws with tests because
     * they are fast! We make it 200ms because the max default granularity for
     * Windows is 100ms and we want an increment of at least 100ms.
     */
    private void elapse200ms() throws InterruptedException {
        Thread.sleep(200)
    }
}
