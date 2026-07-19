package org.openclover.core.instr.tests;

import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.openclover.core.util.Sets.newHashSet;

/**
 * A resolved, persistable {@link TestSourceMatcher}: it carries the already-scanned
 * set of matched files plus the {@link TestDetector} to apply to them. It is the
 * serialization-friendly stand-in for Ant's {@code TestSourceSet} (which extends
 * Ant's {@code FileSet} and cannot be referenced from clover-core). See
 * {@code GroovycSupport}, which converts each resolved {@code TestSourceSet} into
 * one of these before the instrumentation config is written to disk.
 */
public class SimpleTestSourceMatcher implements TestSourceMatcher {

    private final Set<File> includedFiles;
    private final TestDetector detector;

    public SimpleTestSourceMatcher(Set<File> includedFiles, TestDetector detector) {
        this.includedFiles = newHashSet(includedFiles);
        this.detector = detector;
    }

    @Override
    public boolean matchesFile(File f) {
        return includedFiles.contains(f);
    }

    @Override
    public TestDetector getDetector() {
        return detector;
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeInt(includedFiles.size());
        for (File file : includedFiles) {
            out.writeUTF(file.getPath());
        }
        TestDetectorIO.writeDetector(out, detector);
    }

    public static SimpleTestSourceMatcher read(TaggedDataInput in) throws IOException {
        final int count = in.readInt();
        final Set<File> includedFiles = newHashSet();
        for (int i = 0; i < count; i++) {
            includedFiles.add(new File(in.readUTF()));
        }
        final TestDetector detector = in.read(TestDetector.class);
        return new SimpleTestSourceMatcher(includedFiles, detector);
    }
}
