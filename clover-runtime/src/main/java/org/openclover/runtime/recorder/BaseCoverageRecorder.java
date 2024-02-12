package org.openclover.runtime.recorder;

import org.openclover.runtime.CloverNames;
import org.openclover.runtime.ErrorInfo;
import org.openclover.runtime.Logger;
import org_openclover_runtime.Clover;
import org_openclover_runtime.CoverageRecorder;

import java.io.File;
import java.io.IOException;

public abstract class BaseCoverageRecorder extends CoverageRecorder {
    private static final int MIN_INTERVAL = 200;

    // force dependent classes to load early.
    private static final Class<?>[] FORCE_THESE_TO_LOAD = {
            LiveGlobalCoverageRecording.class,
            FileBasedGlobalCoverageRecording.class,
            FileBasedGlobalCoverageRecording.REQUIRED_CLASSES.getClass() // force loading dependencies
    };

    /** D indicates whether coverage needs flushing **/
    protected boolean flushNeeded = true;
    protected long lastFlush = -1;
    protected int flushInterval = 0;
    protected boolean useAlternate = false;
    protected Thread shutdownFlusher;
    protected Thread activeFlusher;
    /** if true, periodic flushing occurs on a separate thread, rather than passively **/
    private boolean activeFlush;
    private boolean directedOnly = false;
    private boolean shutdownHookEnabled = true;
    private boolean useCurrentThreadGroup = true;
    private boolean sliceFlushingEnabled = true;
    private volatile boolean flushInProgress;
    private volatile boolean keepFlushing;

    protected final long dbVersion;
    protected final String dbName;
    protected final String recName;
    protected final String alternateRecName;
    protected final PerTestRecorder testCoverage;
    protected final GlobalRecordingWriteStrategy writeStrategy;

    public BaseCoverageRecorder(String dbName, long dbVersion, long cfgbits, GlobalRecordingWriteStrategy writeStrategy) {
        processConfigBits(cfgbits);
        this.testCoverage = newPerSliceRecorder();
        this.writeStrategy = writeStrategy;

        this.dbVersion = dbVersion;
        if (flushInterval < MIN_INTERVAL) {
            flushInterval = MIN_INTERVAL;
        }
        this.dbName = dbName;

        int hashcode = hashCode();
        long initTS = System.currentTimeMillis();
        recName = Clover.getRecordingName(hashcode, dbName, initTS);
        alternateRecName = recName + GlobalCoverageRecording.ALT_SUFFIX;
    }

    private PerTestRecorder newPerSliceRecorder() {
        String perTestDiff = System.getProperty(CloverNames.PROP_PER_TEST_COV);
        if ("diff".equalsIgnoreCase(perTestDiff)) {
            return new PerTestRecorder.Diffing(this);
        } else if ("off".equalsIgnoreCase(perTestDiff) || "none".equalsIgnoreCase(perTestDiff) || "false".equalsIgnoreCase(perTestDiff)) {
            return new PerTestRecorder.Null();
        } else {
            String perTestThreadingModel = System.getProperty(CloverNames.PROP_PER_TEST_COV_THREADING);
            if ("volatile".equalsIgnoreCase(perTestThreadingModel)) {
                return new ThreadVisibilityStrategy.Volatile(this);
            } else if ("synchronized".equalsIgnoreCase(perTestThreadingModel)) {
                return new ThreadVisibilityStrategy.Synchronized(this);
            } else {
                return new ThreadVisibilityStrategy.SingleThreaded(this);
            }
        }
    }

    private void processConfigBits(long cfg) {
        flushInterval = (int)(cfg & FLUSH_INTERVAL_MASK);
        int cfgbits = (int)(cfg >> 32);
        int flushpolicy = cfgbits & FLUSHPOLICY_MASK;
        activeFlush = false;
        if (flushpolicy == FLUSHPOLICY_DIRECTED) {
            directedOnly = true;
        }
        else if (flushpolicy == FLUSHPOLICY_THREADED) {
            activeFlush = true;

        }
        useCurrentThreadGroup = (cfgbits & USE_CURRENT_THREADGROUP_MASK) != 0;

        shutdownHookEnabled = (cfgbits & DISABLE_SHUTDOWNHOOK_MASK) == 0;

        sliceFlushingEnabled = (cfgbits & DISABLE_SLICE_FLUSHING_MASK) == 0;
    }

    protected String chooseRecordingName() {
        return useAlternate ? alternateRecName : recName;
    }

    protected String write(int[][] hits, int elementCount) throws IOException {
        return writeStrategy.write(chooseRecordingName(), dbVersion, lastFlush, hits, elementCount);
    }

    @Override
    public final void globalSliceStart(String runtimeType, int id) {
        globalSliceStart(runtimeType, id, System.currentTimeMillis());
    }

    @Override
    public final void globalSliceStart(String runtimeType, int id, long startTime) {
        if (Logger.isDebug()) {
            Logger.getInstance().debug("globalSliceStart(" + runtimeType + ", " + id + ", " + startTime + ")");
        }
        Clover.allRecordersSliceStart(runtimeType, id, startTime);
    }

    @Override
    public void globalSliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName, int id) {
        if (Logger.isDebug()) {
            Logger.getInstance().debug("globalSliceEnd(" + runtimeType + ", " + id + ")");
        }
        Clover.allRecordersSliceEnd(runtimeType, method, runtimeTestName, id, -1, null);
    }

    @Override
    public final void globalSliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName, int id, int exitStatus, Throwable throwable) {
        if (Logger.isDebug()) {
            Logger.getInstance().debug("globalSliceEnd(" + runtimeType + ", " + id + ", " + exitStatus + ", " + throwable + ")");
        }
        Clover.allRecordersSliceEnd(runtimeType, method, runtimeTestName, id, exitStatus, Clover.getErrorInfo(throwable));
    }

    class CloverFlushThread extends Thread {
        public CloverFlushThread(ThreadGroup group) {
            super(group, "CloverFlushThread");
        }

        public void requestFlush() {
            forceFlush();
        }

        @Override
        public void run() {
            while (keepFlushing) {
                try {
                    Thread.sleep(flushInterval);
                } catch (InterruptedException e) {
                    // ignore
                }

                if (keepFlushing && System.currentTimeMillis() - lastFlush >= flushInterval) {
                    flush();
                }
            }
        }
    }

    @Override
    public void startRun() {
        createLiveRecordingFile();

        // if we've started mid slice, the slice starts here for this recorder.
        int slice = Clover.getCurrentSlice();
        if (slice != Clover.NO_SLICE) {
            sliceStart(Clover.getCurrentType(), Clover.getCurrentSliceStart(), slice, Clover.getCurrentTestRunID());
        }

        if (activeFlush) {
            activeFlusher = new CloverFlushThread(getTargetThreadGroup());
            keepFlushing = true;
            activeFlusher.setDaemon(true);
            activeFlusher.start();
            Logger.getInstance().debug("Started active flush thread for registry at " + dbName + ", interval= "+flushInterval+"");
        }

        if (shutdownHookEnabled) {
            try {
                shutdownFlusher =  new Thread(getTargetThreadGroup(), "CloverShutdownFlusher") {
                    @Override
                    public void run() {
                        if (activeFlush) {
                            keepFlushing = false;
                            activeFlusher.interrupt();
                        }
                        //Ensure that a flush fully completes before returning from this method
                        forceFlush(null, true);
                    }
                };
                Runtime.getRuntime().addShutdownHook(shutdownFlusher);
                Logger.getInstance().debug("Added shutdown hook for registry at " + dbName + "");
            }
            catch (Throwable t) {
                Logger.getInstance().verbose("Got exception registering a shutdown hook", t);
            }
        }

        Logger.getInstance().debug("Started recorder for registry at \"" + dbName + "\": " + this);
    }

    private void createLiveRecordingFile() {
        final File liveRecFile = new File(dbName + CloverNames.LIVEREC_SUFFIX);
        if (!liveRecFile.exists()) {
            boolean created = false;
            Exception whyNot = null;
            try {
                created = !liveRecFile.createNewFile();
                liveRecFile.deleteOnExit();
            } catch (IOException e) {
                whyNot = e;
            }
            if (!created) {
                Logger.getInstance().verbose(
                    "Failed to newly create the recording-is-live flag file at " + liveRecFile.getAbsolutePath() +
                    (whyNot == null ? " because the file already exists" : " because of an error"), whyNot);
            }
        }
    }

    private ThreadGroup getTargetThreadGroup() {

        ThreadGroup target = Thread.currentThread().getThreadGroup();
        if (useCurrentThreadGroup) {
            return target;
        }
        ThreadGroup parent = target;
        while (parent != null) {
           target = parent;
           parent = target.getParent();
        }
        return target;
    }

    @Override
    public String getRecordingName() {
        return recName;
    }

    @Override
    public long getDbVersion() {
        return dbVersion;
    }

    @Override
    public String getDbName() {
        return dbName;
    }

    public Thread getShutdownFlusher() {
        return shutdownFlusher;
    }

    @Override
    public void flushNeeded() {
        flushNeeded = true;
    }

    @Override
    public void maybeFlush() {
        if (directedOnly || activeFlush) {
            return;
        }
        if (System.currentTimeMillis() - lastFlush > flushInterval) {
            // in interval mode, always flush
            forceFlush();
        }
    }

    @Override
    public void forceFlush() {
        forceFlush(Logger.getInstance(), false);
    }

    @Override
    public void flush() {
        flush(Logger.getInstance(), false);
    }

    private void forceFlush(Logger logger, boolean guaranteeFlushHasFinishedOnce) {
        flushNeeded = true;
        flush(logger, guaranteeFlushHasFinishedOnce);
    }

    /**
     * Flushes the recorder but only if we have noted a flush is required.
     * If a flush is in progress and guaranteeFlushHasFinishedOnce is true
     * then this method will only return after at least one (possibly two)
     * flushes have been completed.
     * This is useful during JVM shutdown to ensure the JVM doesn't terminate
     * while we're still flushing coverage.
     */
    private void flush(Logger logger, boolean guaranteeFlushHasFinishedOnce) {
        if (!flushNeeded || (flushInProgress && !guaranteeFlushHasFinishedOnce)) {
            return;
        }

        synchronized (recName) {
            // a little kludge here, to cope with flushes that happen in the same millisecond in
            // practice this never happens, but in testing, because arrays tend to be tiny, and
            // This code also ensures timestamps are always increasing. Previously, timestamps
            // could get out of order if more than two flushes occurred in the same millisecond
            long now = System.currentTimeMillis();
            lastFlush = (lastFlush >= now) ? lastFlush + 1 : now;

            try {
                flushInProgress = true;
                String memento = write();
                useAlternate = !useAlternate;
                if (logger != null) {
                    logger.debug("[flushed recorder \"" + memento
                            + "\" (file = \"" + (!useAlternate ? alternateRecName : recName)
                            + "\", writeTimestamp = " + lastFlush
                            + ", now = " + now + ")]");
                }
                flushNeeded = false;
            }
            catch (Exception e) {
                logFlushProblem(logger, e);
            }
            catch (Error e) {
                logFlushProblem(logger, e);
                throw e;
            }
            finally {
                flushInProgress = false;
            }
        }
    }

    private void logFlushProblem(Logger logger, Throwable t) {
        if (logger != null) {
            logger.error(t.getClass().getName() + " flushing coverage for recorder " + recName + ": " + t.getMessage());
        }
    }

    @Override
    public void sliceStart(String runtimeType, long ts, int id, int rid) {
        if (sliceFlushingEnabled) {
            testCoverage.testStarted(runtimeType, ts, id, rid);
        }
    }

    /**
     *
     * @param runtimeType the runtime type of the slice
     * @param method the qualified method name
     * @param runtimeTestName name of the test (at runtime) or <code>null</code> if unknown
     * @param endTime timestamp for the flush
     * @param id the method id of the slice
     * @param rid the run id of the slice
     * @param exitStatus == -1 if no result was recorded, 0 if abnormal exit, 1 if normal exit
     * @param ei An ErrorInfo for any RuntimeException or Error thrown (if any, otherwise null)
     */
    @Override
    public void sliceEnd(String runtimeType, String method, /*@Nullable*/ String runtimeTestName,
                         long endTime, int id, int rid, int exitStatus, ErrorInfo ei) {
        //Force a full flush after every test completes.
        //This will ensure the second last recording file (for flush on JVM exit)
        //has accurate coverage so if the last recording file is corrupted
        //as sometimes seems to happen when the JVM exits, the coverage should
        //still be accurately recorded.
        if (Boolean.getBoolean(CloverNames.PROP_FLUSH_EVERY_TEST)) {
            forceFlush();
        }

        if (sliceFlushingEnabled) {
            Logger logger = Logger.getInstance();
            try {
                final String memento = testCoverage.testFinished(runtimeType, method, runtimeTestName,
                        endTime, id, rid, exitStatus, ei).transcribe();
                logger.debug("[flushed per-test recording (" + memento + ") ]");
            } catch (Exception e) {
                logger.error(e.getClass().getName() + " flushing per-test coverage for recorder " + recName + ": " + e.getMessage());
            } catch (Error e) {
                logger.error(e.getClass().getName() + " flushing per-test coverage for recorder " + recName + ": " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Writes coverage data to disk
     */
    protected abstract String write() throws IOException;

    /**
     * Increment slot at index
     */
    @Override
    public abstract void inc(int index);

    /**
     * @return coverage for slot at index but increment by one before evaluation
     */
    @Override
    public abstract int iget(int index);
}
