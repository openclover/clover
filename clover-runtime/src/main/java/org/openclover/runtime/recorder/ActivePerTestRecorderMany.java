package org.openclover.runtime.recorder;

import org.openclover.runtime.ErrorInfo;
import org.openclover.runtime.Logger;
import org.openclover.runtime.RuntimeType;
import com_atlassian_clover.CoverageRecorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * More than two active per-test recorders
 */
public final class ActivePerTestRecorderMany extends ActivePerTestRecorderAny {
    private final ActivePerTestRecorderOne[] those;

    public ActivePerTestRecorderMany(CoverageRecorder coverageRecorder, ActivePerTestRecorderOne[] those) {
        super(coverageRecorder);
        this.those = those;
    }

    @Override
    public void set(int index) {
        for (ActivePerTestRecorderOne thisOne : those) {
            thisOne.set(index);
        }
    }

    @Override
    public ActivePerTestRecorderAny testStarted(String type, long start, int slice, int testRunID) {
        ActivePerTestRecorderOne[] unaries = new ActivePerTestRecorderOne[this.those.length + 1];
        unaries[0] = new ActivePerTestRecorderOne(coverageRecorder, coverageRecorder.createEmptyHitsMask(), new RuntimeType(type), start, slice, testRunID);
        System.arraycopy(this.those, 0, unaries, 1, this.those.length);
        return new ActivePerTestRecorderMany(coverageRecorder, unaries);
    }

    @Override
    public RecordingResult testFinished(String type, String method, String runtimeTestName, long end, int slice, int testRunId, int exitStatus, ErrorInfo errorInfo) {
        RuntimeType runtimeType = new RuntimeType(type);
        if (those[0].matchesTest(type, slice, testRunId)) {
            final double duration = (end - those[0].start) / 1e3; // TODO (nanoTimerEnd - nanoTimerStart) / 1e6;
            // optimization: flush to disk non-empty coverage only
            final LivePerTestRecording livePerTestRecording = those[0].coverage.isModified() ?
                    new FileBasedPerTestRecording(coverageRecorder, those[0].coverage, method, runtimeTestName,
                            those[0].start, end, duration, runtimeType, slice, testRunId, exitStatus, errorInfo)
                    : LivePerTestRecording.NULL;

            if (those.length == 2) {
                return new RecordingResult(livePerTestRecording, those[1]);
            } else {
                ActivePerTestRecorderOne[] unaries = new ActivePerTestRecorderOne[this.those.length - 1];
                System.arraycopy(this.those, 1, unaries, 0, this.those.length - 1);
                return new RecordingResult(livePerTestRecording, new ActivePerTestRecorderMany(coverageRecorder, unaries));
            }
        } else {
            Logger.getInstance().verbose(
                    "Test ending (" + ActivePerTestRecorderMany.asString(type, slice, testRunId) + ") " +
                            "but test recorder in focus doesn't match: " + those[0]);

            ActivePerTestRecorderOne finished = null;
            Collection<ActivePerTestRecorderOne> singles = new ArrayList<>(Arrays.asList(this.those));
            for (Iterator<ActivePerTestRecorderOne> iterator = singles.iterator(); iterator.hasNext(); ) {
                ActivePerTestRecorderOne one = iterator.next();
                boolean matches = one.matchesTest(type, slice, testRunId);
                Logger.getInstance().verbose("Active recorder: " + one);
                if (matches) {
                    iterator.remove();
                    finished = one;
                }
            }

            if (finished == null) {
                Logger.getInstance().verbose(
                        "Test ending (" + ActivePerTestRecorderMany.asString(type, slice, testRunId) + ") " +
                                "but no active per-test recorders match: " + this);
                return new RecordingResult(LivePerTestRecording.NULL, this);
            } else {
                final ActivePerTestRecorderOne[] singlesArray = singles.toArray(new ActivePerTestRecorderOne[0]);
                final double duration = (end - finished.start) / 1e3; // TODO (nanoTimerEnd - nanoTimerStart) / 1e6;
                // optimization: flush to disk non-empty coverage only
                final LivePerTestRecording livePerTestRecording = finished.coverage.isModified() ?
                        new FileBasedPerTestRecording(coverageRecorder, finished.coverage, method, runtimeTestName,
                                finished.start, end, duration, runtimeType, slice, testRunId, exitStatus, errorInfo)
                        : LivePerTestRecording.NULL;

                if (singles.size() == 1) {
                    return new RecordingResult(livePerTestRecording, singlesArray[0]);
                } else {
                    return new RecordingResult(livePerTestRecording, new ActivePerTestRecorderMany(coverageRecorder, singlesArray));
                }
            }
        }
    }

    public String toString() {
        return "Many(" + those.length + "):PerTestRecorders[" +
                "those=" + Arrays.toString(those) +
                ']';
    }
}
