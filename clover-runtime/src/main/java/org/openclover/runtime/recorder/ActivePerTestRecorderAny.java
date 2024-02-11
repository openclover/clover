package org.openclover.runtime.recorder;

import org.openclover.runtime.ErrorInfo;
import org_openclover_runtime.CoverageRecorder;

/**
 * Any number of active per-test recorders
 */
public abstract class ActivePerTestRecorderAny {
    protected final CoverageRecorder coverageRecorder;

    protected static String asString(String typeName, int slice, int testRunId) {
        return "[" + typeName + "," + slice + "," + testRunId + "]";
    }

    public ActivePerTestRecorderAny(CoverageRecorder coverageRecorder) {
        this.coverageRecorder = coverageRecorder;
    }

    /**
     * Records coverage for the slot at the given index for the active per-test recorder(s)
     */
    public abstract void set(int index);

    /**
     * Registers that a test has started.
     *
     * @return ordered collection of per-test recorders with the newest per-test first.
     */
    public abstract ActivePerTestRecorderAny testStarted(String type, long start, int slice, int testRunID);

    /**
     * Registers that a test has ended.
     *
     * @return recording results with is: an ordered collection of per-test recorders with the matching per-test
     *         removed and a coverage slice to be written to disk.
     */
    public abstract RecordingResult testFinished(String type, String method, String runtimeTestName,
             long end, int slice, int testRunId, int exitStatus, ErrorInfo errorInfo);
}
