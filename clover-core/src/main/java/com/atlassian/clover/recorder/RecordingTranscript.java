package com.atlassian.clover.recorder;

import com.atlassian.clover.CoverageDataSpec;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * {@link RecordingTranscript} - interface used for {@link com.atlassian.clover.recorder.CoverageRecording}s
 * loaded at report-time
 */
public interface RecordingTranscript extends CoverageRecording {
    long getWriteTimeStamp();
    void read(DataInputStream in, CoverageDataSpec spec) throws IOException;
}
