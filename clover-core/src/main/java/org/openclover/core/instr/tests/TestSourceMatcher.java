package org.openclover.core.instr.tests;

import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;

import java.io.File;
import java.io.IOException;


public interface TestSourceMatcher extends TaggedPersistent {

    boolean matchesFile(File f);

    TestDetector getDetector();

    /**
     * Only persistable matchers (see {@link SimpleTestSourceMatcher}) override this.
     * Ant's {@code TestSourceSet} inherits this and is never persisted directly - it
     * is converted to a {@link SimpleTestSourceMatcher} before serialization.
     */
    @Override
    default void write(TaggedDataOutput out) throws IOException {
        throw new UnsupportedOperationException(getClass().getName() + " is not persistable");
    }

}
