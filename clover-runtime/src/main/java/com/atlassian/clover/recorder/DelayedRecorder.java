package com.atlassian.clover.recorder;

import com.atlassian.clover.instr.ForInstrumentation;
import com.atlassian.clover.ErrorInfo;
import com.atlassian.clover.Logger;
import com.atlassian.clover.util.CloverBitSet;
import com_atlassian_clover.Clover;
import com_atlassian_clover.CloverProfile;
import com_atlassian_clover.CoverageRecorder;

/**
 * Coverage recorder where it is initially inactive and not recording any coverage but later switches on
 * on the first public method call once the Clover runtime has been initialised.
 * <p/>
 * This class is used for situations where Clover is mid-initialisation but a {@link CoverageRecorder}
 * has been requested. The request should be honored and coverage recording should start
 * once Clover finishes initialisation. 
 */
public class DelayedRecorder extends CoverageRecorder {
    private enum RecordingState {
        WaitingForClover {
            @Override
            public void syncWithCloverRuntime(DelayedRecorder delayedRecorder, RecorderDelegateConstructor constructor) {
                if (Clover.isInitialised()) {
                    Logger.getInstance().warn("Re-entrant Clover initialisation detected. Some coverage may not have been recorded before this point.");
                    delayedRecorder.changeState(Running, constructor.create());
                }
            }},

        Running {
            @Override
            public void syncWithCloverRuntime(DelayedRecorder delayedRecorder, RecorderDelegateConstructor constructor) { }
        };

        public abstract void syncWithCloverRuntime(DelayedRecorder delayedRecorder, RecorderDelegateConstructor constructor);
    }

    /** Callback to get the recorder once Clover is initialised */
    private interface RecorderDelegateConstructor {
        CoverageRecorder create();
    }

    private final RecorderDelegateConstructor delegateConstructor;
    /** The recorder. Starts off null and eventually becomes a full recorder */
    private transient CoverageRecorder delegate = NullRecorder.INSTANCE;
    /** The current state of this class w.r.t. recording coverage */
    private transient RecordingState state = RecordingState.WaitingForClover;

    public DelayedRecorder(final String initString, final long dbVersion, final long cfgbits, final int maxNumElements,
                           final CloverProfile[] profiles, final String... nvpProperties) {
        delegateConstructor = new RecorderDelegateConstructor() {
            @Override
            public CoverageRecorder create() {
                return Clover.getRecorder(initString, dbVersion, cfgbits, maxNumElements, profiles, nvpProperties);
            }
        };
    }

    /** Callback method used by the existing state to change the state and the coverage recorder delegate */
    private synchronized void changeState(RecordingState state, CoverageRecorder delegate) {
        this.state = state;
        this.delegate = delegate;
    }

    private CoverageRecorder syncWithCloverRuntime() {
        state.syncWithCloverRuntime(this, delegateConstructor);
        return delegate;
    }

    @Override
    public String getDbName() {
        return syncWithCloverRuntime().getDbName();
    }

    @Override
    public String getRecordingName() {
        return syncWithCloverRuntime().getRecordingName();
    }

    @Override
    public long getDbVersion() {
        return syncWithCloverRuntime().getDbVersion();
    }

    @Override
    public CloverBitSet compareCoverageWith(CoverageSnapshot before) {
        return syncWithCloverRuntime().compareCoverageWith(before);
    }

    @Override
    public CloverBitSet createEmptyHitsMask() {
        return syncWithCloverRuntime().createEmptyHitsMask();
    }

    @Override
    public void startRun() {
        syncWithCloverRuntime().startRun();
    }

    @Override
    @ForInstrumentation
    public void flushNeeded() {
        syncWithCloverRuntime().flushNeeded();
    }

    @Override
    @ForInstrumentation
    public void maybeFlush() {
        syncWithCloverRuntime().maybeFlush();
    }

    @Override
    public void forceFlush() {
        syncWithCloverRuntime().forceFlush();
    }

    @Override
    public void flush() {
        syncWithCloverRuntime().flush();
    }

    @Override
    public void sliceStart(String runtimeType, long ts, int id, int rid) {
        syncWithCloverRuntime().sliceStart(runtimeType, ts, id, rid);
    }

    @Override
    public void sliceEnd(String runtimeType, String method, String runtimeTestName, long ts, int id, int rid, int exitStatus, ErrorInfo errorInfo) {
        syncWithCloverRuntime().sliceEnd(runtimeType, method, runtimeTestName, ts, id, rid, exitStatus, errorInfo);
    }

    @Override
    @ForInstrumentation
    public void inc(int index) {
        syncWithCloverRuntime().inc(index);
    }

    @Override
    @ForInstrumentation
    public int iget(int index) {
        return syncWithCloverRuntime().iget(index);
    }

    @Override
    public CoverageRecorder withCapacityFor(int maxNumElements) {
        return syncWithCloverRuntime().withCapacityFor(maxNumElements);
    }

    @Override
    @ForInstrumentation
    public void globalSliceStart(String runtimeType, int id) {
        syncWithCloverRuntime().globalSliceStart(runtimeType, id);
    }

    @Override
    public void globalSliceStart(String runtimeType, int id, long startTime) {
        syncWithCloverRuntime().globalSliceStart(runtimeType, id, startTime);
    }

    @Override
    @ForInstrumentation
    public void globalSliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName, int id) {
        syncWithCloverRuntime().globalSliceEnd(runtimeType, method, runtimeTestName, id);
    }

    @Override
    @ForInstrumentation
    public void globalSliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName,
                               int id, int exitStatus, Throwable throwable) {
        syncWithCloverRuntime().globalSliceEnd(runtimeType, method, runtimeTestName, id, exitStatus, throwable);
    }

    @Override
    public CoverageSnapshot getCoverageSnapshot() {
        return syncWithCloverRuntime().getCoverageSnapshot();
    }
}
