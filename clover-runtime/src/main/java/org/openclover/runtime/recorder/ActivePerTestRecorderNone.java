package org.openclover.runtime.recorder;

import org.openclover.runtime.ErrorInfo;
import org.openclover.runtime.Logger;
import org.openclover.runtime.RuntimeType;
import com_atlassian_clover.CoverageRecorder;

/**
 * No active per-test recorders
 */
public final class ActivePerTestRecorderNone extends ActivePerTestRecorderAny {
    public ActivePerTestRecorderNone(CoverageRecorder coverageRecorder) {
        super(coverageRecorder);
    }

    @Override
    public void set(int index) {
    }

    @Override
    public ActivePerTestRecorderAny testStarted(String type, long start, int slice, int testRunID) {
        return new ActivePerTestRecorderOne(coverageRecorder, coverageRecorder.createEmptyHitsMask(), new RuntimeType(type), start, slice, testRunID);
    }

    @Override
    public RecordingResult testFinished(String type, String method, String runtimeTestName, long end, int slice, int testRunId, int exitStatus, ErrorInfo errorInfo) {
        Logger.getInstance().verbose("Per-test recorder ending " + ActivePerTestRecorderNone.asString(type, slice, testRunId) + " but no current recorder in focus");
        return new RecordingResult(LivePerTestRecording.NULL, this);
    }
}
