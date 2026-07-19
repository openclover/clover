package org.openclover.core.recorder

import org.openclover.core.CoverageDataSpec
import org.openclover.core.CoverageDataTestBase
import org.openclover.core.api.registry.TestCaseInfo
import org.openclover.core.io.tags.ObjectReader
import org.openclover.core.io.tags.TaggedInputReader
import org.openclover.core.io.tags.TaggedOutputWriter
import org.openclover.core.io.tags.Tags
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullTestCaseInfo
import org.openclover.runtime.recorder.PerTestRecorder
import org.openclover.runtime.util.CloverBitSet

/**
 * Round-trip tests for the tag-based persistence of {@link InMemPerTestCoverage}
 * (and the {@link FullTestCaseInfo} keys), i.e. the coverage sub-block of the
 * {@code clover.db} coverage segment.
 */
class InMemPerTestCoveragePersistenceTest extends CoverageDataTestBase {

    // mirrors CoverageSegment.TAGS (same numbering, continuing InstrSessionSegment's)
    private static final Tags TAGS = new Tags()
            .registerTag(InMemPerTestCoverage.name, Tags.NEXT_TAG + 16, InMemPerTestCoverage.&read as ObjectReader)
            .registerTag(FullTestCaseInfo.name, Tags.NEXT_TAG + 17, FullTestCaseInfo.&read as ObjectReader)

    protected CoverageDataSpec newCoverageDataSpec() {
        return new CoverageDataSpec(null, 0, false, true, false, true, PerTestCoverageStrategy.IN_MEMORY)
    }

    protected InMemPerTestCoverage newPerTestCoverage(Clover2Registry registry) {
        return new InMemPerTestCoverage(registry)
    }

    private static InMemPerTestCoverage roundTrip(InMemPerTestCoverage coverage) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream()
        new TaggedOutputWriter(new DataOutputStream(bos), TAGS).write(InMemPerTestCoverage, coverage)
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()))
        return new TaggedInputReader(din, TAGS).read(InMemPerTestCoverage)
    }

    void testRoundTripPreservesTestsAndHits() {
        int SLOT_COUNT = 64 * 3
        long start = System.currentTimeMillis()
        Clover2Registry reg = newPrefabReg(SLOT_COUNT)
        InMemPerTestCoverage coverage = new InMemPerTestCoverage(reg)

        CloverBitSet bits1 = new CloverBitSet(SLOT_COUNT)
        bits1.add(3)
        bits1.add(70)
        def t1 = newPerTestTranscript(reg, bits1, "Foo", "Foo.testOne", start, 0, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci1 = FullTestCaseInfo.Factory.getInstanceForSlice(t1)
        coverage.addCoverage(tci1, t1)

        CloverBitSet bits2 = new CloverBitSet(SLOT_COUNT)
        bits2.add(130)
        def t2 = newPerTestTranscript(reg, bits2, "Foo", "Foo.testTwo", start + 1, 1, 0, PerTestRecorder.NORMAL_EXIT, null)
        TestCaseInfo tci2 = FullTestCaseInfo.Factory.getInstanceForSlice(t2)
        coverage.addCoverage(tci2, t2)

        assertTrue(coverage.hasPerTestData())

        InMemPerTestCoverage read = roundTrip(coverage)

        assertTrue(read.hasPerTestData())
        assertEquals(coverage.getCoverageSize(), read.getCoverageSize())
        assertEquals(coverage.getTests(), read.getTests())

        for (TestCaseInfo tci : coverage.getTests()) {
            assertEquals("hits for " + tci, coverage.getHitsFor(tci), read.getHitsFor(tci))
            assertNotNull("getTestById for " + tci.getId(), read.getTestById(tci.getId()))
            assertEquals(tci, read.getTestById(tci.getId()))
        }

        // unique-coverage mask (initMasks) is derived identically after reload
        assertEquals(coverage.getUniqueHitsFor(tci1), read.getUniqueHitsFor(read.getTestById(tci1.getId())))
    }

    void testEmptyCoverageRoundTrips() {
        Clover2Registry reg = newPrefabReg(64)
        InMemPerTestCoverage read = roundTrip(new InMemPerTestCoverage(reg))
        assertFalse(read.hasPerTestData())
        assertTrue(read.getTests().isEmpty())
        assertEquals(64, read.getCoverageSize())
    }
}
