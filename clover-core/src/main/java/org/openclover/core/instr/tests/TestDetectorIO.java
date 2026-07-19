package org.openclover.core.instr.tests;

import org.openclover.core.io.tags.TaggedDataOutput;

import java.io.IOException;

/**
 * Helpers for writing polymorphic {@link TestDetector} values with the tag-based
 * format.
 * <p>
 * The tag codec keys the on-disk tag off the <em>declared</em> {@code Class} passed
 * to {@link TaggedDataOutput#write}, not the runtime type, so writing requires
 * dispatching to the concrete class. Reading stays polymorphic: the tag in the
 * stream selects the concrete reader, so callers just use
 * {@code in.read(TestDetector.class)}.
 */
public final class TestDetectorIO {

    private TestDetectorIO() {
    }

    /**
     * Writes a possibly-null {@link TestDetector}, dispatching on its concrete type
     * so the whitelist tag matches. A null is written under an arbitrary registered
     * tag (the reader returns null regardless of which one).
     */
    public static void writeDetector(TaggedDataOutput out, TestDetector detector) throws IOException {
        if (detector == null) {
            out.write(DefaultTestDetector.class, null);
        } else if (detector instanceof NoTestDetector) {
            out.write(NoTestDetector.class, (NoTestDetector) detector);
        } else if (detector instanceof DefaultTestDetector) {
            out.write(DefaultTestDetector.class, (DefaultTestDetector) detector);
        } else if (detector instanceof TestSpec) {
            out.write(TestSpec.class, (TestSpec) detector);
        } else if (detector instanceof AggregateTestDetector) {
            out.write(AggregateTestDetector.class, (AggregateTestDetector) detector);
        } else if (detector instanceof FileMappedTestDetector) {
            out.write(FileMappedTestDetector.class, (FileMappedTestDetector) detector);
        } else {
            throw new IOException("Cannot persist test detector of type " + detector.getClass().getName());
        }
    }
}
