package org.openclover.runtime.recorder;

import org.openclover.runtime.ErrorInfo;
import com_atlassian_clover.CoverageRecorder;

/**
 * Base interface for different strategies for handling how the current collection of active per test recorders are made
 * visible across different threads.
 * <p/>
 * Three subclasses: Volatile, Synchronized and SingleThreaded
 */
public interface ThreadVisibilityStrategy extends PerTestRecorder {
    /**
     * A Holder that guarantees correct per-test recorder visibility to all accessing threads. This class is a good
     * balance between performance and correctness because reads (element increment) vastly outweigh writes (tests
     * starting or ending). This class is only guaranteed to work for JVMs that implement the visibility guarantees
     * outlined by JLS for Java 5.
     */
    class Volatile implements ThreadVisibilityStrategy {
        /**
         * Stack of recorders - volatile to allow cheap mostly read/seldom write lock when used with
         * testStarted/testFinished synchronization
         */
        private volatile ActivePerTestRecorderAny recorders;

        public Volatile(CoverageRecorder coverageRecorder) {
            recorders = new ActivePerTestRecorderNone(coverageRecorder);
        }

        /**
         * Adds an in-focus recorder. Method is synchronized to force a memory barrier flush.
         */
        @Override
        public synchronized void testStarted(String runtimeType, long start, int slice, int testRunId) {
            recorders = recorders.testStarted(runtimeType, start, slice, testRunId);
        }

        /**
         * Removes an in-focus recorder. Method is synchronized to force a memory barrier flush.
         */
        @Override
        public synchronized LivePerTestRecording testFinished(String runtimeType, String method,
                /*@Nullable*/ String runtimeTestName,
                long end, int slice, int testRunId, int exitStatus, ErrorInfo ei) {
            RecordingResult sliceAndRecorders = recorders.testFinished(runtimeType, method, runtimeTestName,
                    end, slice, testRunId, exitStatus, ei);
            recorders = sliceAndRecorders.recorders;
            return sliceAndRecorders.recording;
        }

        @Override
        public void set(int index) {
            recorders.set(index);
        }
    }

    /**
     * A Holder that guarantees correct per-test recorder visibility to all accessing threads. This will work for all
     * JVMs but may have significant performance implications because of the excessive synchronization.
     */
    class Synchronized implements ThreadVisibilityStrategy {
        /**
         * Stack of recorders - volatile to allow cheap mostly read/seldom write lock when used with
         * testStarted/testFinished synchronization
         */
        private ActivePerTestRecorderAny recorders;

        public Synchronized(CoverageRecorder coverageRecorder) {
            recorders = new ActivePerTestRecorderNone(coverageRecorder);
        }

        /**
         * Adds an in-focus recorder. Method is synchronized to force a memory barrier flush.
         */
        @Override
        public synchronized void testStarted(String runtimeType, long start, int slice, int testRunId) {
            recorders = recorders.testStarted(runtimeType, start, slice, testRunId);
        }

        /**
         * Removes an in-focus recorder. Method is synchronized to force a memory barrier flush.
         */
        @Override
        public synchronized LivePerTestRecording testFinished(String runtimeType, String method,
                /*Nullable*/ String runtimeTestName,
                long end, int slice, int testRunId, int exitStatus, ErrorInfo ei) {
            RecordingResult sliceAndRecorders = recorders.testFinished(runtimeType, method, runtimeTestName,
                    end, slice, testRunId, exitStatus, ei);
            recorders = sliceAndRecorders.recorders;
            return sliceAndRecorders.recording;
        }

        @Override
        public synchronized void set(int index) {
            recorders.set(index);
        }
    }

    /**
     * A Holder that makes no guarantees about visibility where more than one thread gets or set the per-test recorder.
     * This class should be sufficient for the vast majority of unit tests.
     */
    class SingleThreaded implements ThreadVisibilityStrategy {
        private ActivePerTestRecorderAny recorders;

        public SingleThreaded(CoverageRecorder coverageRecorder) {
            recorders = new ActivePerTestRecorderNone(coverageRecorder);
        }

        @Override
        public void testStarted(String runtimeType, long start, int slice, int testRunId) {
            recorders = recorders.testStarted(runtimeType, start, slice, testRunId);
        }

        @Override
        public LivePerTestRecording testFinished(String runtimeType, String method, /*@Nullable*/ String runtimeTestName,
                long end, int slice, int testRunId, int exitStatus, ErrorInfo ei) {
            RecordingResult sliceAndRecorders = recorders.testFinished(runtimeType, method, runtimeTestName,
                    end, slice, testRunId, exitStatus, ei);
            recorders = sliceAndRecorders.recorders;
            return sliceAndRecorders.recording;
        }

        @Override
        public void set(int index) {
            recorders.set(index);
        }
    }
}
