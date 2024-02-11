package org_openclover_runtime;

import com.atlassian.clover.instr.ForInstrumentation;
import org.openclover.runtime.recorder.CoverageSnapshot;
import org.openclover.runtime.ErrorInfo;
import org.openclover.runtime.util.CloverBitSet;

/**
 * Mostly abstract base class for coverage recorders.
 * Any substantial methods should be declared here as abstract but defined in
 * BaseCoverageRecorder.
 */
public abstract class CoverageRecorder {
    public static final int FLUSHPOLICY_DIRECTED = 0;
    public static final int FLUSHPOLICY_INTERVAL = 1;
    public static final int FLUSHPOLICY_THREADED = 2;

    /**
     * Config information is packed into a single long which is added to instrumentation.
     * Bits
     *  0-31 = integer flush interval in milliseconds
     * 32-34 = flush policy: 0 = directed, 1 = interval, 2 = threaded
     * 35-37 = NO LONGER USED: was: recording format: 0 = integer, 1 = boolean
     *    38 = disable shutdown hook: 0 = enable, 1 = disable
     *    39 = use current threadgroup for thread creation: 0 = top, 1 = current
     *    40 = disable slice flushing: 0 = enable, 1 = disable
     */
    public static final long FLUSH_INTERVAL_MASK = Integer.MAX_VALUE;
    public static final int FLUSHPOLICY_MASK = 0x7;
    public static final int DISABLE_SHUTDOWNHOOK_MASK = 0x1 << 7;
    public static final int USE_CURRENT_THREADGROUP_MASK = 0x1 << 8;
    public static final int DISABLE_SLICE_FLUSHING_MASK = 0x1 << 9;

    public abstract String getDbName();

    public abstract String getRecordingName();

    public abstract long getDbVersion();

    /** @return a bit set mask for elements that have had coverage since the CoverageSnapshot was generated */
    public abstract CloverBitSet compareCoverageWith(CoverageSnapshot before);

    /** @return an empty bit set mask sufficient for the current number of elements */
    public abstract CloverBitSet createEmptyHitsMask();

    public abstract void startRun();

    @ForInstrumentation
    public abstract void flushNeeded();

    @ForInstrumentation
    public abstract void maybeFlush();

    public abstract void forceFlush();

    public abstract void flush();

    public abstract void sliceStart(String runtimeType, long ts, int id, int rid);

    public abstract void sliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName,
                                  long ts, int id, int rid, int exitStatus, ErrorInfo errorInfo);

    /** Increment slot at index */
    @ForInstrumentation
    public abstract void inc(int index);

    /** @return coverage for slot at index but increment by one before evaluation */
    @ForInstrumentation
    public abstract int iget(int index);

    /**
     * Returns an instance that *may* support recording the number of elements requested (throw growth)
     * else accepts (but harmlessly) discards recording for elements that can't be accepted.
     */
    public abstract CoverageRecorder withCapacityFor(int maxNumElements);

    ///CLOVER:OFF
    @ForInstrumentation
    public final void rethrow(Throwable t) {
        //Filled in by ASM due to JLS requirement that checked exceptions are
        //in the throws clause
    }
    ///CLOVER:ON

    @ForInstrumentation
    public abstract void globalSliceStart(String runtimeType, int id);

    public abstract void globalSliceStart(String runtimeType, int id, long startTime);

    @ForInstrumentation
    public abstract void globalSliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName, int id);

    @ForInstrumentation
    public abstract void globalSliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName, int id, int exitStatus, Throwable throwable);

    public static long getConfigBits(
        long flushPolicy, int flushInterval, boolean useCurrentThreadGroup,
        boolean disableShutdownHook, boolean disableSlicedFlushing) {

        long result = flushInterval;

        result += (flushPolicy << 32);

        if (disableShutdownHook) {
            result += (0x1L << 7+32);
        }

        if (useCurrentThreadGroup) {
            result += (0x1L << 8+32);
        }

        if (disableSlicedFlushing) {
            result += (0x1L << 9+32);
        }

        return result;
    }

    public abstract CoverageSnapshot getCoverageSnapshot();
}
