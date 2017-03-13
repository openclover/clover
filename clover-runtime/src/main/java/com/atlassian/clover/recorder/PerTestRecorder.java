package com.atlassian.clover.recorder;

import com.atlassian.clover.util.CloverBitSet;
import com.atlassian.clover.ErrorInfo;
import com.atlassian.clover.RuntimeType;
import com_atlassian_clover.CoverageRecorder;
import org.jetbrains.annotations.Nullable;

/**
 * A recorder for per-test coverage. There may be more than one recorder active at a time although this is to be
 * discouraged. Where more than one is active at a time, all will record per-test coverage.
 */
public interface PerTestRecorder {
    public static final int NO_EXIT_RESULT = -1;
    public static final int ABNORMAL_EXIT = 0;
    public static final int NORMAL_EXIT = 1;

    public static class Null implements PerTestRecorder {
        @Override
        public void testStarted(String runtimeType, long start, int slice, int testRunId) {
        }

        @Override
        public LivePerTestRecording testFinished(String runtimeType, String method, @Nullable String runtimeTestName,
                                                 long end, int slice, int testRunId, int exitStatus, ErrorInfo ei) {
            return LivePerTestRecording.NULL;
        }

        @Override
        public void set(int index) {
        }
    }

    public static class Diffing implements PerTestRecorder {
        protected final CoverageRecorder coverageRecorder;
        protected long start;
        protected CoverageSnapshot startingCoverage;

        public Diffing(CoverageRecorder coverageRecorder) {
            this.coverageRecorder = coverageRecorder;
        }

        @Override
        public void set(int index) {
        }

        @Override
        public void testStarted(String type, long start, int slice, int testRunID) {
            this.start = start;
            this.startingCoverage = coverageRecorder.getCoverageSnapshot();
        }

        @Override
        public LivePerTestRecording testFinished(String type, String method, @Nullable String runtimeTestName,
                                                 long end, int slice, int testRunId, int exitStatus, ErrorInfo errorInfo) {
            final double duration = (end - start) / 1e3; // TODO (nanoTimerEnd - nanoTimerStart) / 1e6;
            return new FileBasedPerTestRecording(
                    coverageRecorder,
                    coverageRecorder.compareCoverageWith(startingCoverage),
                    method, runtimeTestName, start, end, duration, new RuntimeType(type),
                    slice, testRunId, exitStatus, errorInfo);
        }

        private CloverBitSet diff(CloverBitSet start, CloverBitSet end) {
            end.subtractInPlace(start);
            return end;
        }
    }

    public void testStarted(String runtimeType, long start, int slice, int testRunId);

    public LivePerTestRecording testFinished(String runtimeType, String method, @Nullable String runtimeTestName,
                                             long end, int slice, int testRunId, int exitStatus, ErrorInfo ei);

    public void set(int index);
}