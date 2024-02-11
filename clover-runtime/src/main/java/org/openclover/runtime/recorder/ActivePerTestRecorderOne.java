package org.openclover.runtime.recorder;

import org.openclover.runtime.ErrorInfo;
import org.openclover.runtime.Logger;
import org.openclover.runtime.RuntimeType;
import org.openclover.runtime.util.CloverBitSet;
import com_atlassian_clover.CoverageRecorder;

/**
 * Exactly one active per-test recorders
 */
public final class ActivePerTestRecorderOne extends ActivePerTestRecorderAny {
    private volatile boolean[] coverageShortcut;
    final CloverBitSet coverage;
    private final RuntimeType type;
    final long start;
    private final int slice;
    private final int testRunID;

    public ActivePerTestRecorderOne(CoverageRecorder coverageRecorder, CloverBitSet coverage, RuntimeType type, long start, int slice, int testRunID) {
        super(coverageRecorder);
        this.coverageShortcut = new boolean[coverage.size()];
        this.coverage = coverage;
        this.type = type;
        this.start = start;
        this.slice = slice;
        this.testRunID = testRunID;
    }

    @Override
    public void set(int index) {
        if (index > coverageShortcut.length - 1) {
            // reallocate as CloverBitSet is growable; at least double the size to avoid too frequent reallocs
            synchronized (this) {
                final int newSize = Math.max(coverageShortcut.length * 2, index + 1);
                final boolean[] newShortcut = new boolean[newSize];
                System.arraycopy(coverageShortcut, 0, newShortcut, 0, coverageShortcut.length);
                coverageShortcut = newShortcut;
            }
        }

        if (!coverageShortcut[index]) {
            coverageShortcut[index] = true;
            coverage.add(index);
        }
    }

    @Override
    public ActivePerTestRecorderAny testStarted(String type, long start, int slice, int testRunID) {
        return new ActivePerTestRecorderMany(
                coverageRecorder,
                new ActivePerTestRecorderOne[]{
                        new ActivePerTestRecorderOne(coverageRecorder, coverageRecorder.createEmptyHitsMask(), new RuntimeType(type), start, slice, testRunID),
                        this});
    }

    @Override
    public RecordingResult testFinished(String type, String method, String runtimeTestName, long end, int slice, int testRunId, int exitStatus, ErrorInfo errorInfo) {
        if (!matchesTest(type, slice, testRunId)) {
            Logger.getInstance().verbose(
                    "Per-test recorder ending " + ActivePerTestRecorderOne.asString(type, slice, testRunId) + " " +
                            "but different recorder in focus " + ActivePerTestRecorderOne.asString(this.type.name, this.slice, this.testRunID));
            return new RecordingResult(LivePerTestRecording.NULL, this);
        } else {
            final double duration = (end - start) / 1e3; // TODO (nanoTimerEnd - nanoTimerStart) / 1e6;
            // optimization: flush to disk non-empty coverage only
            final LivePerTestRecording perTestRecording = coverage.isModified() ?
                    new FileBasedPerTestRecording(coverageRecorder, coverage, method, runtimeTestName,
                                                  start, end, duration, this.type, slice, testRunId, exitStatus, errorInfo)
                    : LivePerTestRecording.NULL;

            return new RecordingResult(perTestRecording, new ActivePerTestRecorderNone(coverageRecorder));
        }
    }

    boolean matchesTest(String type, int slice, int testRunId) {
        return this.type.matches(type) && this.slice == slice && this.testRunID == testRunId;
    }

    public String toString() {
        return "One:PerTestRecorders[" +
                "type=" + type +
                ", start=" + start +
                ", slice=" + slice +
                ", testRunID=" + testRunID +
                ']';
    }

}
