package com.atlassian.clover.recorder;

import java.io.IOException;

/**
 * How coverage recordings are written (to a file, to the network, in memory [for testing] etc)
 */
public interface GlobalRecordingWriteStrategy {
    static final GlobalRecordingWriteStrategy WRITE_TO_FILE = new GlobalRecordingWriteStrategy() {
        @Override
        public String write(String recordingFileName, long dbVersion, long lastFlush, int[][] hits, int elementCount) throws IOException {
            return new FileBasedGlobalCoverageRecording(
                recordingFileName, dbVersion, lastFlush, hits, elementCount).write();
        }
    };

    public String write(String recordingFileName, long dbVersion, long lastFlush, int[][] hits, int elementCount) throws IOException;
}
