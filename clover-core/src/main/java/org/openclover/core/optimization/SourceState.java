package org.openclover.core.optimization;

import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;

import java.io.IOException;

/**
 * Records the interesting bits of a FullFileInfo for later comparison. Only used in this source file.
 */
final class SourceState implements TaggedPersistent {
    private final long checksum;
    private final long filesize;

    SourceState(long checksum, long filesize) {
        this.checksum = checksum;
        this.filesize = filesize;
    }

    public long getChecksum() {
        return checksum;
    }

    public long getFilesize() {
        return filesize;
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeLong(checksum);
        out.writeLong(filesize);
    }

    public static SourceState read(TaggedDataInput in) throws IOException {
        final long checksum = in.readLong();
        final long filesize = in.readLong();
        return new SourceState(checksum, filesize);
    }

    @Override
    public String toString() {
        return "SourceState{" +
                "checksum=" + checksum +
                ", filesize=" + filesize +
                '}';
    }
}
