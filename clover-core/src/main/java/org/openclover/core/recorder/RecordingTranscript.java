package org.openclover.core.recorder;

import org.openclover.core.CoverageDataSpec;
import org.openclover.runtime.recorder.CoverageRecording;

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
