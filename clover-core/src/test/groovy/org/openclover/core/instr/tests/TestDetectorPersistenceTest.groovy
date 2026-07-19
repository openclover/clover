package org.openclover.core.instr.tests

import org.junit.Test
import org.openclover.core.io.tags.ObjectReader
import org.openclover.core.io.tags.TaggedInputReader
import org.openclover.core.io.tags.TaggedOutputWriter
import org.openclover.core.io.tags.Tags

import java.util.regex.Pattern

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

/**
 * Round-trip tests for every {@link org.openclover.core.io.tags.TaggedPersistent}
 * type in the resolved test-detector graph that is persisted inside an
 * instrumentation config (the shape produced by the Ant/Groovy flow).
 */
class TestDetectorPersistenceTest {

    private static final Tags TAGS = new Tags()
            .registerTag(NoTestDetector.name, Tags.NEXT_TAG + 51, NoTestDetector.&read as ObjectReader)
            .registerTag(DefaultTestDetector.name, Tags.NEXT_TAG + 52, DefaultTestDetector.&read as ObjectReader)
            .registerTag(TestSpec.name, Tags.NEXT_TAG + 53, TestSpec.&read as ObjectReader)
            .registerTag(AggregateTestDetector.name, Tags.NEXT_TAG + 54, AggregateTestDetector.&read as ObjectReader)
            .registerTag(FileMappedTestDetector.name, Tags.NEXT_TAG + 55, FileMappedTestDetector.&read as ObjectReader)
            .registerTag(SimpleTestSourceMatcher.name, Tags.NEXT_TAG + 56, SimpleTestSourceMatcher.&read as ObjectReader)

    private static <T> T roundTrip(Class<T> type, T value) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream()
        new TaggedOutputWriter(new DataOutputStream(bos), TAGS).write(type, value)
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()))
        return new TaggedInputReader(din, TAGS).read(type)
    }

    @Test
    void testNoTestDetector() {
        assertTrue(roundTrip(NoTestDetector, new NoTestDetector()) instanceof NoTestDetector)
    }

    @Test
    void testDefaultTestDetector() {
        assertTrue(roundTrip(DefaultTestDetector, new DefaultTestDetector()) instanceof DefaultTestDetector)
    }

    @Test
    void testTestSpecAllPatterns() {
        TestSpec spec = new TestSpec()
        spec.setPkgPattern(Pattern.compile("com\\.foo.*"))
        spec.setClassAnnotationPattern(Pattern.compile("@RunWith"))
        spec.setClassPattern(Pattern.compile(".*Test"))
        spec.setSuperPattern(Pattern.compile("TestCase"))
        spec.setClassTagPattern(Pattern.compile("@mytest"))
        spec.setMethodAnnotationPattern(Pattern.compile("@Test"))
        spec.setMethodPattern(Pattern.compile("test.*"))
        spec.setMethodReturnTypePattern(Pattern.compile("void"))
        spec.setMethodTagPattern(Pattern.compile("@case"))

        TestSpec read = roundTrip(TestSpec, spec)

        assertEquals("com\\.foo.*", read.@pkgPattern.pattern())
        assertEquals("@RunWith", read.@classAnnotationPattern.pattern())
        assertEquals(".*Test", read.@classPattern.pattern())
        assertEquals("TestCase", read.@superPattern.pattern())
        assertEquals("@mytest", read.@classTagPattern.pattern())
        assertEquals("@Test", read.@methodAnnotationPattern.pattern())
        assertEquals("test.*", read.@methodPattern.pattern())
        assertEquals("void", read.@methodReturnTypePattern.pattern())
        assertEquals("@case", read.@methodTagPattern.pattern())
    }

    @Test
    void testTestSpecWithNullPatterns() {
        TestSpec spec = new TestSpec()
        spec.setClassPattern(Pattern.compile(".*Test"))
        // all others left null

        TestSpec read = roundTrip(TestSpec, spec)

        assertEquals(".*Test", read.@classPattern.pattern())
        assertNull(read.@pkgPattern)
        assertNull(read.@methodPattern)
        assertNull(read.@methodTagPattern)
    }

    @Test
    void testAggregateOrStrategyWithChildren() {
        AggregateTestDetector aggregate = new AggregateTestDetector(new OrStrategy())
        aggregate.addDetector(new DefaultTestDetector())
        TestSpec spec = new TestSpec()
        spec.setClassPattern(Pattern.compile(".*IT"))
        aggregate.addDetector(spec)

        AggregateTestDetector read = roundTrip(AggregateTestDetector, aggregate)

        assertTrue(read.@strategy instanceof OrStrategy)
        assertEquals(2, read.@detectors.size())
        assertTrue(read.@detectors[0] instanceof DefaultTestDetector)
        assertTrue(read.@detectors[1] instanceof TestSpec)
        assertEquals(".*IT", read.@detectors[1].@classPattern.pattern())
    }

    @Test
    void testAggregateAndStrategyPreserved() {
        AggregateTestDetector aggregate = new AggregateTestDetector(new AndStrategy())
        aggregate.addDetector(new NoTestDetector())

        AggregateTestDetector read = roundTrip(AggregateTestDetector, aggregate)

        assertTrue(read.@strategy instanceof AndStrategy)
        assertEquals(1, read.@detectors.size())
    }

    @Test
    void testSimpleTestSourceMatcher() {
        Set<File> files = [new File("src/Foo.java"), new File("src/BarTest.java")] as Set
        TestSpec spec = new TestSpec()
        spec.setClassPattern(Pattern.compile(".*Test"))
        SimpleTestSourceMatcher matcher = new SimpleTestSourceMatcher(files, spec)

        SimpleTestSourceMatcher read = roundTrip(SimpleTestSourceMatcher, matcher)

        assertTrue(read.matchesFile(new File("src/Foo.java")))
        assertTrue(read.matchesFile(new File("src/BarTest.java")))
        assertFalse(read.matchesFile(new File("src/Other.java")))
        assertTrue(read.getDetector() instanceof TestSpec)
        assertEquals(".*Test", read.getDetector().@classPattern.pattern())
    }

    @Test
    void testFileMappedTestDetectorGraph() {
        // This mirrors what GroovycSupport builds for the Ant -> Groovy flow
        Set<File> testFiles = [new File("test/FooTest.java")] as Set
        TestSpec spec = new TestSpec()
        spec.setClassPattern(Pattern.compile(".*Test"))
        SimpleTestSourceMatcher matcher = new SimpleTestSourceMatcher(testFiles, spec)

        FileMappedTestDetector detector = new FileMappedTestDetector(new NoTestDetector())
        detector.addTestSourceMatcher(matcher)

        FileMappedTestDetector read = roundTrip(FileMappedTestDetector, detector)

        assertEquals(1, read.@testFileMatchers.size())
        assertTrue(read.@testFileMatchers[0] instanceof SimpleTestSourceMatcher)
        assertTrue(read.@testFileMatchers[0].matchesFile(new File("test/FooTest.java")))
        assertTrue(read.@defaultDetector instanceof NoTestDetector)
    }

    @Test
    void testNullDetectorRoundTrips() {
        assertNull(roundTrip(FileMappedTestDetector, (FileMappedTestDetector) null))
    }
}
