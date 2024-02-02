package org.openclover.idea.coverage;

import java.io.File;

public interface SnapshotFileMutex {
    void lockFile(File file);
    void releaseFile(File file);
}
