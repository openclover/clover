package org.openclover.runtime.recorder;

import org.openclover.runtime.ErrorInfo;
import org.openclover.runtime.util.CloverBitSet;
import org_openclover_runtime.CoverageRecorder;

import java.io.IOException;

/**
 * {@link CoverageRecorder} which can grow its capacity to record coverage. This is necessary
 * where the original Clover database is unavailable due to various deployment scenarios
 * and Clover should make best efforts to record coverage.
 * <p/>
 * This class will emit proxy {@link CoverageRecorder}s from {@link #withCapacityFor(int)} which can track
 * enough coverage required at the point when they were requested (normally when a Clovered class is first loaded).
 * If more coverage needs to be tracked a subsequent call to {@link #withCapacityFor(int)} should be made for a new proxy
 * which satisfies the new requirements. A call to {@link #withCapacityFor(int)} on a proxy will, if the proxy
 * cannot support the required number of elements, will redirect to the underlying {@link GrowableCoverageRecorder}
 * which will return a sufficenly large proxy (having grown the underlying {@link CoverageMatrix} in a thread safe
 * manner).
 * <p/>
 * Proxies and the original {@link GrowableCoverageRecorder} share coverage by sharing int[] sections of the total
 * logical coverage array (which in essence becomes an int[][] ie a matrix). The master matrix may grow but because the
 * original int[] objects remain in use and shared across all subsequent proxies the total coverage is shared in a relatively
 * thread safe fashion (note: we still suffer the elements[i]++ MT problem).
 */
public class GrowableCoverageRecorder extends BaseCoverageRecorder {
    /**
     * Growable coverage matrix (the matrix instance is immutable but grows by sharing structure with later editions).
     * Although volatile, this member should be rarely accessed or mutated by instrumented code. Mutation will occur
     * when an emitted proxy is insufficient in size.
     */
    private volatile CoverageMatrix coverage;

    /**
     * Factory method. Use this to get an instance of the recorder. Do not call constructors directly
     * (they're not private only for the sake of unit tests).
     */
    public static CoverageRecorder createFor(String dbName, long dbVersion, long cfgbits, int maxNumElements) {
        //Only ever return a proxy so its coverage matrix reference (final) is used by the instrumented
        //class rather than the mutable (but not volatile) matrix reference of the underlying GrowableCoverageRecorder
        return new GrowableCoverageRecorder(dbName, dbVersion, cfgbits, maxNumElements).withCapacityFor(maxNumElements);
    }

    /*private*/ GrowableCoverageRecorder(String dbName, long dbVersion, long cfgbits, int maxNumElements) {
        this(dbName, dbVersion, cfgbits, maxNumElements, GlobalRecordingWriteStrategy.WRITE_TO_FILE);
    }

    /*private*/ GrowableCoverageRecorder(String dbName, long dbVersion, long cfgbits, int maxNumElements, GlobalRecordingWriteStrategy writeStrategy) {
        super(dbName, dbVersion, cfgbits, writeStrategy);
        coverage = new CoverageMatrix(maxNumElements);
    }

    @Override
    protected String write() throws IOException {
        final int[][] hits;
        final int elementCount;
        synchronized (this) {
            hits = coverage.getHits();
            elementCount = coverage.getNumElements();
        }
        return write(hits, elementCount);
    }

    @Override
    public CloverBitSet compareCoverageWith(CoverageSnapshot before) {
        final int[][] hits;
        final int elementCount;
        synchronized (this) {
            hits = coverage.getHits();
            elementCount = coverage.getNumElements();
        }
        int[][] beforeHits = before.getCoverage();
        //Make arrays same length
        if (hits.length > beforeHits.length) {
            final int[][] expandedBeforeHits = new int[hits.length][];
            System.arraycopy(beforeHits, 0, expandedBeforeHits, 0, beforeHits.length);
            for (int i = beforeHits.length; i < expandedBeforeHits.length; i++) {
                expandedBeforeHits[i] = new int[hits[i].length];
            }
            beforeHits = expandedBeforeHits;
        }

        for (int i = 0; i < hits.length; i++) {
            int[] row = hits[i];
            int[] beforeRow = beforeHits[i];
            for (int j = 0; j < row.length; j++) {
                beforeRow[j] = beforeRow[j] - row[j];
            }
        }
        return CloverBitSet.forHits(beforeHits, elementCount);
    }

    @Override
    public CloverBitSet createEmptyHitsMask() {
        return new CloverBitSet(coverage.getNumElements());
    }

    @Override
    public int iget(int index) {
        throw new UnsupportedOperationException("void iget(int) should be called on the FixedProxy");
    }

    @Override
    public void inc(int index) {
        throw new UnsupportedOperationException("void inc(int) should be called on the FixedProxy");
    }

    @Override
    public CoverageRecorder withCapacityFor(int maxNumElements) {
        synchronized (this) {
            if (maxNumElements > coverage.getNumElements()) {
                coverage = new CoverageMatrix(coverage, maxNumElements);
                return new FixedProxy(this, coverage);
            } else {
                return new FixedProxy(this);
            }
        }
    }

    @Override
    public CoverageSnapshot getCoverageSnapshot() {
        final int[][] elements = coverage.elements;
        final int[][] clone = new int[elements.length][];
        for(int i = 0; i < elements.length; i++) {
            clone[i] = elements[i].clone();
        }
        return new CoverageSnapshot(clone);
    }

    /**
     * Immutable coverage matrix which allows for growth of coverage recorded by sharing coverage 'sections'
     * (ie int[]) with enlarged versions of itself.
     **/
    public static class CoverageMatrix {
        static final int WIDTH_EXPONENT = 20;
        static final int WIDTH = 1 << WIDTH_EXPONENT; //1,048,576
        static final int WIDTH_MOD_MASK = WIDTH - 1; //x mod y where y is power of two -> x & (y - 1)

        private final int[][] elements;
        private final int numElements;

        public CoverageMatrix(int numElements) {
            this.elements = new int[heightFor(null, numElements)][WIDTH];
            this.numElements = numElements;
        }

        public CoverageMatrix(CoverageMatrix previous, int numElements) {
            this.elements = new int[heightFor(previous, numElements)][];
            System.arraycopy(previous.elements, 0, this.elements, 0, previous.elements.length);
            for (int i = previous.elements.length; i < this.elements.length; i++) {
                this.elements[i] = new int[WIDTH];
            }
            this.numElements = numElements;
        }

        int heightFor(CoverageMatrix current, int required) {
            int currentHeight = current == null ? 1 : current.elements.length;
            while ((currentHeight * WIDTH) < required) {
                currentHeight = currentHeight << 1;
            }
            return currentHeight;
        }

        int getNumElements() {
            return numElements;
        }

        int[][] getHits() {
            return elements;
        }

        int iget(int index) {
            return ++(elements[index >> WIDTH_EXPONENT][index & WIDTH_MOD_MASK]);
        }

        void inc(int index) {
            elements[index >> WIDTH_EXPONENT][index & WIDTH_MOD_MASK]++;
        }

        public int[] toContiguousCoverage() {
            final int[] contiguous = new int[numElements];
            for (int i = elements.length; i < this.elements.length; i++) {
                System.arraycopy(elements[i], 0, contiguous, i >> WIDTH_EXPONENT, WIDTH);
            }
            return contiguous;
        }

        ///CLOVER:OFF
        @Override
        public String toString() {
            return "CoverageMatrix[" +
                "numElements=" + numElements +
                ", width=" + (elements.length == 0 ? 0 : elements[0].length) +
                ", height=" + elements.length +
                "]";
        }
        ///CLOVER:ON
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "GrowableCoverageRecorder[coverage=" + coverage + "]";
    }
    ///CLOVER:ON

    /**
     * A lightweight proxy which provides just enough coverage capacity
     * for the class that asked for it but delegates to the original
     * GrowableCoverageRecorder for all other functions.
     * <p/>
     * Its CoverageMatrix shares int[] elements with the original
     * GrowableCoverageRecorder's CoverageMatrix.
     */
    public static final class FixedProxy extends CoverageRecorder {
        private final GrowableCoverageRecorder target;
        private final CoverageMatrix coverage;
        private final PerTestRecorder testCoverage;

        FixedProxy(GrowableCoverageRecorder target) {
            this.target = target;
            this.coverage = target.coverage;
            this.testCoverage = target.testCoverage;
        }

        FixedProxy(GrowableCoverageRecorder target, CoverageMatrix coverage) {
            this.target = target;
            this.coverage = coverage;
            this.testCoverage = target.testCoverage;
        }

        @Override
        public int iget(int index) {
            testCoverage.set(index);
            return coverage.iget(index);
        }

        @Override
        public void inc(int index) {
            testCoverage.set(index);
            coverage.inc(index);
        }

        @Override
        public CoverageRecorder withCapacityFor(int maxNumElements) {
            if (maxNumElements > coverage.getNumElements()) {
                //Our portion of the coverage matrix isn't large enough, get a proxy with a larger portion
                return target.withCapacityFor(maxNumElements);
            } else {
                //We have enough of the matrix for the current requirements
                return this;
            }
        }

        ///CLOVER:OFF
        @Override
        public CloverBitSet compareCoverageWith(CoverageSnapshot before) {
            return target.compareCoverageWith(before);
        }

        @Override
        public CoverageSnapshot getCoverageSnapshot() {
            return target.getCoverageSnapshot();
        }

        @Override
        public CloverBitSet createEmptyHitsMask() {
            return target.createEmptyHitsMask();
        }

        @Override
        public void flush() {
            target.flush();
        }

        @Override
        public void flushNeeded() {
            target.flushNeeded();
        }

        @Override
        public void forceFlush() {
            target.forceFlush();
        }

        @Override
        public String getDbName() {
            return target.getDbName();
        }

        @Override
        public long getDbVersion() {
            return target.getDbVersion();
        }

        @Override
        public String getRecordingName() {
            return target.getRecordingName();
        }

        public Thread getShutdownFlusher() {
            return target.getShutdownFlusher();
        }

        @Override
        public void maybeFlush() {
            target.maybeFlush();
        }

        @Override
        public void sliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName,
                             long ts, int id, int rid, int exitStatus, ErrorInfo ei) {
            target.sliceEnd(runtimeType, method, runtimeTestName, ts, id, rid, exitStatus, ei);
        }

        @Override
        public void sliceStart(String runtimeType, long ts, int id, int rid) {
            target.sliceStart(runtimeType, ts, id, rid);
        }

        @Override
        public void startRun() {
            target.startRun();
        }

        @Override
        public String toString() {
            return "FixedProxy[" +
                "target=" + target +
                ", coverage=" + coverage +
                "]";
        }

        @Override
        public void globalSliceStart(String runtimeType, int id) {
            target.globalSliceStart(runtimeType, id);
        }

        @Override
        public void globalSliceStart(String runtimeType, int id, long startTime) {
            target.globalSliceStart(runtimeType, id, startTime);
        }

        @Override
        public void globalSliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName, int id) {
            target.globalSliceEnd(runtimeType, method, runtimeTestName, id);
        }

        @Override
        public void globalSliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName,
                                   int id, int exitStatus, Throwable throwable) {
            target.globalSliceEnd(runtimeType, method, runtimeTestName, id, exitStatus, throwable);
        }
        ///CLOVER:ON
    }
}
