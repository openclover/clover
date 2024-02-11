package org.openclover.runtime.recorder;

/**
 * Helper class which keeps result of the per-test recording.
 */
public final class RecordingResult {
    public final LivePerTestRecording recording;
    public final ActivePerTestRecorderAny recorders;

    public RecordingResult(LivePerTestRecording recording, ActivePerTestRecorderAny recorders) {
        this.recording = recording;
        this.recorders = recorders;
    }
}
