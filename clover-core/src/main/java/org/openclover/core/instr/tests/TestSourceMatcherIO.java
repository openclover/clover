package org.openclover.core.instr.tests;

import org.openclover.core.io.tags.TaggedDataOutput;

import java.io.IOException;

/**
 * Helpers for writing polymorphic {@link TestSourceMatcher} values with the
 * tag-based format.
 * <p>
 * The tag codec keys the on-disk tag off the <em>declared</em> {@code Class} passed
 * to {@link TaggedDataOutput#write}, not the runtime type, so writing requires
 * dispatching to the concrete class. Reading stays polymorphic: the tag in the
 * stream selects the concrete reader, so callers just use
 * {@code in.read(TestSourceMatcher.class)}.
 */
public final class TestSourceMatcherIO {

    private TestSourceMatcherIO() {
    }

    /**
     * Writes a possibly-null {@link TestSourceMatcher}. Only
     * {@link SimpleTestSourceMatcher} is persistable.
     */
    public static void writeMatcher(TaggedDataOutput out, TestSourceMatcher matcher) throws IOException {
        if (matcher == null) {
            out.write(SimpleTestSourceMatcher.class, null);
        } else if (matcher instanceof SimpleTestSourceMatcher) {
            out.write(SimpleTestSourceMatcher.class, (SimpleTestSourceMatcher) matcher);
        } else {
            throw new IOException("Cannot persist test source matcher of type " + matcher.getClass().getName());
        }
    }
}
