package org.openclover.runtime.recorder;

import org.openclover.runtime.ErrorInfo;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.CloverBitSet;
import org_openclover_runtime.CoverageRecorder;

public final class NullRecorder extends CoverageRecorder {
    public static final NullRecorder INSTANCE = new NullRecorder();

    ///CLOVER:OFF
    private NullRecorder() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void flushNeeded() {
    }

    @Override
    public void forceFlush() {
    }

    @Override
    public void maybeFlush() {
    }

    @Override
    public int iget(int index) {
        return 0;
    }

    @Override
    public void inc(int index) {
    }

    @Override
    public void sliceStart(String runtimeType, long ts, int id, int rid) {
    }

    @Override
    public void sliceEnd(String runtimeType, String method, String runtimeTestName, long ts, int id, int rid, int exitStatus, ErrorInfo ei) {
    }

    @Override
    public void startRun() {
        Logger.getInstance().debug("Started recorder for registry at \"" + getDbName() + "\": " + this);
    }

    @Override
    public String getDbName() {
        return "null";
    }

    @Override
    public String getRecordingName() {
        return "null";
    }

    @Override
    public long getDbVersion() {
        return 0;
    }

    @Override
    public CloverBitSet compareCoverageWith(CoverageSnapshot before) {
        return new CloverBitSet();
    }

    @Override
    public CloverBitSet createEmptyHitsMask() {
        return new CloverBitSet();
    }

    @Override
    public CoverageRecorder withCapacityFor(int maxNumElements) {
        return this;
    }

    @Override
    public CoverageSnapshot getCoverageSnapshot() {
        return null;
    }

    @Override
    public void globalSliceStart(String runtimeType, int id) {
    }

    @Override
    public void globalSliceStart(String runtimeType, int id, long startTime) {
    }

    @Override
    public void globalSliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName, int id) {
    }

    @Override
    public void globalSliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName,
                               int id, int exitStatus, Throwable throwable) {
    }

    @Override
    public String toString() {
        return "NullRecorder";
    }
}
