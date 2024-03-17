package org.openclover.core

import junit.framework.TestCase
import org.openclover.core.api.registry.ContextSet
import org.openclover.core.api.registry.TestCaseInfo
import org.openclover.core.context.ContextSetImpl
import org.openclover.core.instr.InstrumentationSessionImpl
import org.openclover.core.recorder.PerTestCoverage
import org.openclover.core.recorder.PerTestRecordingTranscript
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.api.registry.CoverageDataProvider
import org.openclover.core.registry.FixedSourceRegion
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.MethodSignature
import org.openclover.core.registry.entities.Modifiers
import org.openclover.core.registry.entities.FullTestCaseInfo
import org.openclover.core.util.SimpleCoverageRange
import org.openclover.runtime.ErrorInfo
import org.openclover.runtime.RuntimeType
import org.openclover.runtime.api.CloverException
import org.openclover.runtime.recorder.FileBasedPerTestRecording
import org.openclover.runtime.recorder.PerTestRecorder
import org.openclover.runtime.util.CloverBitSet

abstract class CoverageDataTestBase extends TestCase {
    private File tempDir
    private int recorderId

    @Override
    protected void setUp() throws Exception {
        tempDir = TestUtils.createEmptyDirFor(getClass(), getName())
        recorderId = 0
    }

    @Override
    void tearDown() {
        FullTestCaseInfo.Factory.reset()
    }

    void testGetForSliceWithDifferentRuntimeTestName() throws IOException, CloverException {
        int SLOT_COUNT = 64 * 3
        long start = System.currentTimeMillis()

        Clover2Registry reg = newPrefabReg(SLOT_COUNT)

        PerTestRecordingTranscript recordingTranscript1 =
                newPerTestTranscript(
                        reg, new CloverBitSet(), "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.NORMAL_EXIT, null)
        recordingTranscript1.runtimeTestName = "runtimeTest1"
        PerTestRecordingTranscript recordingTranscript2 =
                newPerTestTranscript(
                        reg, new CloverBitSet(), "Foo", "Foo.testMethod", start, 1, 0, PerTestRecorder.NORMAL_EXIT, null)
        recordingTranscript2.runtimeTestName = "runtimeTest2"

        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(recordingTranscript1)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(recordingTranscript2)

        assertFalse("TestCaseInfo must not be equals to each other", tci1.equals(tci2))
    }

    void testGetTestById() throws IOException, CloverException {
        int SLOT_COUNT = 64 * 3
        long start = System.currentTimeMillis()

        Clover2Registry reg = newPrefabReg(SLOT_COUNT)
        CoverageData data = new CoverageData(reg, newCoverageDataSpec())

        PerTestRecordingTranscript recordingTranscript1 =
            newPerTestTranscript(
                reg, new CloverBitSet(), "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(recordingTranscript1)
        data.addCoverage(tci1, recordingTranscript1)

        PerTestRecordingTranscript recordingTranscript2 =
            newPerTestTranscript(
                reg, new CloverBitSet(), "Foo", "Foo.testMethod2", start + 2, 1, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(recordingTranscript2)
        data.addCoverage(tci2, recordingTranscript2)

        assertSame(tci1, data.getTestById(tci1.getId()))
        assertSame(tci2, data.getTestById(tci2.getId()))
        assertNull(data.getTestById(-1))
        assertNull(data.getTestById(Integer.MAX_VALUE))
    }

    void testUniqueTestCoverage() throws IOException, CloverException {
        int SLOT_COUNT = 64 * 3
        long start = System.currentTimeMillis()

        Clover2Registry reg = newPrefabReg(SLOT_COUNT)
        CoverageData data = new CoverageData(reg, newCoverageDataSpec())

        // Foo.testMethod: [0..63] => 1 hit, [64..127] => 1 hit, [128..191] => 0 hit
        CloverBitSet perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 0; i < 64; i++) {
            perTestCoverage.add(i)
        }
        for (int i = 1 * 64; i < 2 * 64; i++) {
            perTestCoverage.add(i)
        }

        PerTestRecordingTranscript recordingTranscript1 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(recordingTranscript1)
        data.addCoverage(tci1, recordingTranscript1)

        // Foo.testMethod2: [0..63] => 0 hit, [64..127] => 1 hit, [128..191] => 1 hit
        perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 1 * 64; i < 2 * 64; i++) {
            perTestCoverage.add(i)
        }
        for (int i = 2 * 64; i < 3 * 64; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript recordingTranscript2 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod2", start + 2, 1, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(recordingTranscript2)
        data.addCoverage(tci2, recordingTranscript2)

        //[0..63] => (Foo.testMethod)
        //[64..127] => (Foo.testMethod, Foo.testMethod2)
        //[128..191] => (Foo.testMethod2)
        CoverageDataProvider tci1Coverage = new BitSetCoverageProvider(data.getUniqueHitsFor(tci1), data)
        for (int i = 0; i < 64; i++) {
            assertEquals(String.format("index %d:", i), 1, tci1Coverage.getHitCount(i))
        }
        for (int i = 1 * 64; i < 2 * 64; i++) {
            assertEquals(String.format("index %d:", i), 0, tci1Coverage.getHitCount(i))
        }
        for (int i = 2 * 64; i < 3 * 64; i++) {
            assertEquals(String.format("index %d:", i), 0, tci1Coverage.getHitCount(i))
        }

        CoverageDataProvider tci2Coverage = new BitSetCoverageProvider(data.getUniqueHitsFor(tci2), data)
        for (int i = 0; i < 64; i++) {
            assertEquals(String.format("index %d:", i), 0, tci2Coverage.getHitCount(i))
        }
        for (int i = 1 * 64; i < 2 * 64; i++) {
            assertEquals(String.format("index %d:", i), 0, tci2Coverage.getHitCount(i))
        }
        for (int i = 2 * 64; i < 3 * 64; i++) {
            assertEquals(String.format("index %d:", i), 1, tci2Coverage.getHitCount(i))
        }
    }

    void testPassOnlyTestCoverage() throws IOException, CloverException {
        int SLOT_COUNT = 64 * 3
        long start = System.currentTimeMillis()

        //[0..63] [64..127] [128..191]
        //   2        2          2
        int[] coverage = new int[SLOT_COUNT]
        for (int i = 0; i < coverage.length; i++) {
            coverage[i] = 2
        }

        Clover2Registry reg = newPrefabReg(SLOT_COUNT)
        CoverageData data = new CoverageData(0, coverage, newPerTestCoverage(reg))

        //FAILING TEST
        //[0..63] [64..127] [128..191]
        //   +        +          0
        CloverBitSet perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 0; i < 64; i++) {
            perTestCoverage.add(i)
        }
        for (int i = 1 * 64; i < 2 * 64; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript failedRecording =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.ABNORMAL_EXIT, new ErrorInfo("message", "stack trace"))
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(failedRecording)
        data.addCoverage(tci1, failedRecording)

        //PASSING TEST
        //[0..63] [64..127] [128..191]
        //   +        0          +
        perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 1 * 64; i < 2 * 64; i++) {
            perTestCoverage.add(i)
        }
        for (int i = 2 * 64; i < 3 * 64; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript passedRecording =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod2", start + 2, 1, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(passedRecording)
        data.addCoverage(tci2, passedRecording)

        CoverageDataProvider passOnlyCoverage = new BitSetCoverageProvider(data.getPassOnlyHits(), data)

        for (int i = 0; i < 64; i++) {
            assertEquals(String.format("index %d:", i), 0, passOnlyCoverage.getHitCount(i))
        }
        for (int i = 1 * 64; i < 2 * 64; i++) {
            assertEquals(String.format("index %d:", i), 1, passOnlyCoverage.getHitCount(i))
        }
        for (int i = 2 * 64; i < 3 * 64; i++) {
            assertEquals(String.format("index %d:", i), 1, passOnlyCoverage.getHitCount(i))
        }
    }

    void testAllCoverageWhenAllPassing() throws IOException, CloverException {
        int SLOT_COUNT = 64 * 2
        long start = System.currentTimeMillis()

        //[0..63] [64..127]
        //   2        2
        int[] coverage = new int[SLOT_COUNT]
        for (int i = 0; i < coverage.length; i++) {
            coverage[i] = 2
        }

        Clover2Registry reg = newPrefabReg(SLOT_COUNT)
        CoverageData data = new CoverageData(0, coverage, newPerTestCoverage(reg))

        //[0..63] [64..127]
        //   +        0
        CloverBitSet perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 0; i < 64; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript passedRecording1 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.NORMAL_EXIT, new ErrorInfo("message", "stack trace"))
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(passedRecording1)
        data.addCoverage(tci1, passedRecording1)

        //[0..63] [64..127]
        //   0        +
        perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 1 * 64; i < SLOT_COUNT; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript passedRecording2 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod2", start + 2, 1, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(passedRecording2)
        data.addCoverage(tci2, passedRecording2)

        CoverageDataProvider allCoverage = new BitSetCoverageProvider(data.getAllHits(), data)

        for (int i = 0; i < SLOT_COUNT; i++) {
            assertEquals(String.format("index %d:", i), 1, allCoverage.getHitCount(i))
        }
    }

    void testAllCoverageWhenSomeFailing() throws IOException, CloverException {
        int SLOT_COUNT = 64 * 2
        long start = System.currentTimeMillis()

        //[0..63] [64..127]
        //   2        2
        int[] coverage = new int[SLOT_COUNT]
        for (int i = 0; i < coverage.length; i++) {
            coverage[i] = 2
        }

        Clover2Registry reg = newPrefabReg(SLOT_COUNT)
        CoverageData data = new CoverageData(0, coverage, newPerTestCoverage(reg))

        //[0..63] [64..127]
        //   +        0
        CloverBitSet perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 0; i < 64; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript failedRecording =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.ABNORMAL_EXIT, new ErrorInfo("message", "stack trace"))
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(failedRecording)
        data.addCoverage(tci1, failedRecording)

        //[0..63] [64..127]
        //   0        +
        perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 1 * 64; i < SLOT_COUNT; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript passedRecording =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod2", start + 2, 1, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(passedRecording)
        data.addCoverage(tci2, passedRecording)

        CoverageDataProvider allCoverage = new BitSetCoverageProvider(data.getAllHits(), data)

        for (int i = 0; i < SLOT_COUNT; i++) {
            assertEquals(String.format("index %d:", i), 1, allCoverage.getHitCount(i))
        }
    }

    void testAllCoverageWhenAllFailing() throws IOException, CloverException {
        int SLOT_COUNT = 64 * 2
        long start = System.currentTimeMillis()

        //[0..63] [64..127]
        //   2        2
        int[] coverage = new int[SLOT_COUNT]
        for (int i = 0; i < coverage.length; i++) {
            coverage[i] = 2
        }

        Clover2Registry reg = newPrefabReg(SLOT_COUNT)
        CoverageData data = new CoverageData(0, coverage, newPerTestCoverage(reg))

        //[0..63] [64..127]
        //   +        0
        CloverBitSet perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 0; i < 64; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript failedRecording1 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.ABNORMAL_EXIT, new ErrorInfo("message", "stack trace"))
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(failedRecording1)
        data.addCoverage(tci1, failedRecording1)

        //[0..63] [64..127]
        //   0        +
        perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 1 * 64; i < SLOT_COUNT; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript failedRecording2 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod2", start + 2, 1, 0, PerTestRecorder.ABNORMAL_EXIT, null)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(failedRecording2)
        data.addCoverage(tci2, failedRecording2)

        CoverageDataProvider allCoverage = new BitSetCoverageProvider(data.getAllHits(), data)

        for (int i = 0; i < SLOT_COUNT; i++) {
            assertEquals(String.format("index %d:", i), 1, allCoverage.getHitCount(i))
        }
    }

    void testMapCoverage() throws IOException, CloverException, CloverException {
        int SLOT_COUNT = 64 * 2
        long start = System.currentTimeMillis()

        //[0..63] [64..127]
        //   2        2
        int[] coverage = new int[SLOT_COUNT]
        for (int i = 0; i < coverage.length; i++) {
            coverage[i] = 2
        }

        //Ensure the FileInfos are laid out correctly for this test - SamplingPerTestCoverage needs this info to sample
        Clover2Registry reg = newEmptyReg()
        addFileClassMethodStatements(reg, "Doo", "void foo()", 64)
        addFileClassMethodStatements(reg, "Boo", "void foo()", 64)


        CoverageData data = new CoverageData(0, coverage, newPerTestCoverage(reg))

        //[0..63] [64..127]
        //   +        0
        CloverBitSet perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 0; i < 64; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript passedRecording1 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(passedRecording1)
        data.addCoverage(tci1, passedRecording1)

        //[0..63] [64..127]
        //   0        +
        perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 1 * 64; i < SLOT_COUNT; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript passedRecording2 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod2", start + 2, 1, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(passedRecording2)
        data.addCoverage(tci2, passedRecording2)

        Map<TestCaseInfo, BitSet> testsAndCoverage = data.mapTestsAndCoverageForFile(newFileInfo(0, 64))
        assertEquals(1, testsAndCoverage.size())
        assertTrue(testsAndCoverage.keySet().contains(tci1))
        CoverageDataProvider testCoverage =
            new BitSetCoverageProvider(
                (BitSet)testsAndCoverage.values().toArray()[0],
                data)
        for (int i = 0; i < 64; i++) {
            assertEquals(String.format("index %d:", i), 1, testCoverage.getHitCount(i))
        }
        for (int i = 64; i < 2 * 64; i++) {
            assertEquals(String.format("index %d:", i), 0, testCoverage.getHitCount(i))
        }

        testsAndCoverage = data.mapTestsAndCoverageForFile(newFileInfo(64, 64))
        assertEquals(1, testsAndCoverage.size())
        assertTrue(testsAndCoverage.keySet().contains(tci2))
        testCoverage = new BitSetCoverageProvider(testsAndCoverage.get(tci2), data)
        for (int i = 0; i < 64; i++) {
            assertEquals(String.format("index %d:", i), 0, testCoverage.getHitCount(i))
        }
        for (int i = 64; i < 2 * 64; i++) {
            assertEquals(String.format("index %d:", i), 1, testCoverage.getHitCount(i))
        }
    }

    void testGetTestsCoveringRange() throws IOException, CloverException, CloverException {
        int SLOT_COUNT = 64 * 2
        long start = System.currentTimeMillis()

        //[0..63] [64..127]
        //   2        2
        int[] coverage = new int[SLOT_COUNT]
        for (int i = 0; i < coverage.length; i++) {
            coverage[i] = 2
        }

        //Ensure the FileInfos are laid out correctly for this test - SamplingPerTestCoverage needs this info to sample
        Clover2Registry reg = newEmptyReg()
        addFileClassMethodStatements(reg, "Doo", "void foo()", 64)
        addFileClassMethodStatements(reg, "Boo", "void foo()", 64)


        CoverageData data = new CoverageData(0, coverage, newPerTestCoverage(reg))

        //[0..63] [64..127]
        //   +        0
        CloverBitSet perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 0; i < 64; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript passedRecording1 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(passedRecording1)
        data.addCoverage(tci1, passedRecording1)

        //[0..63] [64..127]
        //   0        +
        perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 1 * 64; i < SLOT_COUNT; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript passedRecording2 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod2", start + 2, 1, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(passedRecording2)
        data.addCoverage(tci2, passedRecording2)

        Set<TestCaseInfo> tests = data.getTestsCovering(new SimpleCoverageRange(0, 64))
        assertEquals(1, tests.size())
        assertTrue(tests.contains(tci1))

        tests = data.getTestsCovering(new SimpleCoverageRange(33, 64))
        assertEquals(2, tests.size())
        assertTrue(tests.contains(tci1))
        assertTrue(tests.contains(tci2))

        tests = data.getTestsCovering(new SimpleCoverageRange(64, 64))
        assertEquals(1, tests.size())
        assertTrue(tests.contains(tci2))

        tests = data.getTestsCovering(new SimpleCoverageRange(128, 64))
        assertTrue(tests.isEmpty())
    }

    void testGetHitsForTCI() throws IOException, CloverException, CloverException {
        int SLOT_COUNT = 64 * 2
        long start = System.currentTimeMillis()

        //[0..63] [64..127]
        //   2        2
        int[] coverage = new int[SLOT_COUNT]
        for (int i = 0; i < coverage.length; i++) {
            coverage[i] = 2
        }

        //Ensure the FileInfos are laid out correctly for this test - SamplingPerTestCoverage needs this info to sample
        Clover2Registry reg = newPrefabReg(SLOT_COUNT)

        CoverageData data = new CoverageData(0, coverage, newPerTestCoverage(reg))

        //[0..63] [64..127]
        //   +        0
        CloverBitSet perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 0; i < 64; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript passedRecording1 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(passedRecording1)
        data.addCoverage(tci1, passedRecording1)

        //[0..63] [64..127]
        //   0        +
        perTestCoverage = new CloverBitSet(SLOT_COUNT)
        for (int i = 1 * 64; i < SLOT_COUNT; i++) {
            perTestCoverage.add(i)
        }
        PerTestRecordingTranscript passedRecording2 =
            newPerTestTranscript(
                reg, perTestCoverage, "Foo", "Foo.testMethod2", start + 2, 1, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(passedRecording2)
        data.addCoverage(tci2, passedRecording2)

        CoverageDataProvider testCoverage = new BitSetCoverageProvider(data.getHitsFor(tci1), data)
        for (int i = 0; i < 64; i++) {
            assertEquals(String.format("index %d:", i), 1, testCoverage.getHitCount(i))
        }
        for (int i = 64; i < 2 * 64; i++) {
            assertEquals(String.format("index %d:", i), 0, testCoverage.getHitCount(i))
        }

        testCoverage = new BitSetCoverageProvider(data.getHitsFor(tci2), data)
        for (int i = 0; i < 64; i++) {
            assertEquals(String.format("index %d:", i), 0, testCoverage.getHitCount(i))
        }
        for (int i = 64; i < 2 * 64; i++) {
            assertEquals(String.format("index %d:", i), 1, testCoverage.getHitCount(i))
        }
    }

    void testHasPerTestCoverage() throws IOException, CloverException {
        int SLOT_COUNT = 64 * 3
        long start = System.currentTimeMillis()

        Clover2Registry reg = newPrefabReg(SLOT_COUNT)
        CoverageData data = new CoverageData(reg, newCoverageDataSpec())
        assertFalse(data.hasPerTestData())

        PerTestRecordingTranscript recordingTranscript1 =
            newPerTestTranscript(
                reg, new CloverBitSet(), "Foo", "Foo.testMethod", start, 0, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(recordingTranscript1)
        data.addCoverage(tci1, recordingTranscript1)

        assertTrue(data.hasPerTestData());         
    }
    private void addFileClassMethodStatements(Clover2Registry reg, String className, String methodSignature, int elementCount) throws CloverException, IOException {
        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) reg.startInstr()
        session.enterFile("", File.createTempFile("clover", "java"), 0, 0, 0, 0, 0)
        session.enterClass(className, new FixedSourceRegion(0, 0), new Modifiers(), false, false, false)
        session.enterMethod(new ContextSetImpl(), new FixedSourceRegion(0, 0), new MethodSignature(methodSignature), false)
        for (int i = 0; i < elementCount - 1; i++) {
            session.addStatement(new ContextSetImpl(), new FixedSourceRegion(0, 0), 1)
        }
        session.exitMethod(0, 0)
        session.exitClass(0, 0)
        session.exitFile()
        session.finishAndApply()
    }

    private FullFileInfo newFileInfo(int start, int length) throws IOException, CloverException {
        FullFileInfo file = new FullFileInfo(null, File.createTempFile("clover", "java"), null, start, 0, 0, 0, 0, 0, 0)
        file.setDataLength(length)
        return file
    }

    protected abstract CoverageDataSpec newCoverageDataSpec()

    protected abstract PerTestCoverage newPerTestCoverage(Clover2Registry registry)


    private PerTestRecordingTranscript newPerTestTranscript(Clover2Registry registry, CloverBitSet perTestCoverage, String className, String testName, long start, int test, int sliceId, int exitStatus, ErrorInfo ei) throws IOException, CloverException {
        final RuntimeType type = new RuntimeType(className)

        // Save to disk, in case needed by a PerTestCoverage instance
        final FileBasedPerTestRecording recording = new FileBasedPerTestRecording(
                registry.getInitstring(), 0, recorderId++,
                perTestCoverage, testName, testName + "AtRuntime",
                start, start + 1, 0.001, type, sliceId, test, exitStatus, ei)
        recording.transcribe()

        // "Here's one I prepared earlier..."
        PerTestRecordingTranscript recordingTranscript1 = new PerTestRecordingTranscript(
                perTestCoverage, recording.getFile(), 0, testName, testName + "AtRuntime",
                start, start + 1, 0.001, type, test, sliceId, exitStatus, ei)
        return recordingTranscript1
    }

    private File newTempFile(String suffix) throws IOException, CloverException {
        File file = File.createTempFile("coverage", suffix, tempDir)
        file.deleteOnExit()
        return file
    }

    private Clover2Registry newEmptyReg() throws IOException, CloverException {
        return new Clover2Registry(newTempFile(".db"), "Registry")
    }

    private Clover2Registry newPrefabReg(int slotCount) throws IOException, CloverException {
        final Clover2Registry reg = new Clover2Registry(newTempFile(".db"), "Registry")
        final ContextSet context = new ContextSetImpl()

        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) reg.startInstr()
        session.enterFile("com.foo", new File("Foo.java"), 0, 0, 0, 0, 0)
        session.enterClass("Foo", new FixedSourceRegion(0, 0), new Modifiers(), false, false, false)
        session.enterMethod(context, new FixedSourceRegion(0, 0), new MethodSignature("void bar()"), false)
        for(int i = 0; i < slotCount - 1; i++) {
            session.addStatement(context, new FixedSourceRegion(0, 0), 1)
        }
        session.exitMethod(0, 0)
        session.exitClass(0, 0)
        session.exitFile()
        session.finishAndApply()
        return reg
    }
}
