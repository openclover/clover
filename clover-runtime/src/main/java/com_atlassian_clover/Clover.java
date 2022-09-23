package com_atlassian_clover;

import com.atlassian.clover.instr.ForInstrumentation;
import com.atlassian.clover.CloverNames;
import com.atlassian.clover.CloverProperties;
import com.atlassian.clover.recorder.DelayedRecorder;
import com.atlassian.clover.Environment;
import com.atlassian.clover.ErrorInfo;
import com.atlassian.clover.recorder.FixedSizeCoverageRecorder;
import com.atlassian.clover.recorder.GrowableCoverageRecorder;
import com.atlassian.clover.Logger;
import com.atlassian.clover.recorder.NullRecorder;
import com.atlassian.clover.RecorderLogging;
import com.atlassian.clover.recorder.SharedCoverageRecorder;
import com.atlassian.clover.remote.DistributedClover;
import com.atlassian.clover.remote.RpcMessage;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * user-accessible runtime control for Clover
 */
public final class Clover {
    public static final int NO_SLICE = -1;
    public static final String SECURITY_EXCEPTION_MSG =
            "[CLOVER] FATAL ERROR: Clover could not be initialised because it has insufficient security privileges. "
            + "Please consult the Clover documentation on the security policy file changes required.";

    /**
     * Indicates if Clover has already initialised or in the process of initialisation.
     */
    private static volatile boolean initialisedOrInitialising = false;
    /**
     * The underlying Clover Runtime. Is either an instance of UnitialisedRuntime or InitialisedRuntime and
     * starts off as an UninitialisedRuntime.
     */
    private static volatile Runtime runtime = new UninitialisedRuntime();

    /**
     * Ensure Clover initialisation occurs. Supports re-entrant
     * calls so that initialisation only ever happens once. See CLOV-952
     *
     * @return for most use cases, will return an instance of InitialisedRuntime
     *         but where re-entrant calls occur and !initialisedOrInitialising for the first
     *         call, will return an instance of UninitialisedRuntime for all but the first in
     *         the call chain
     */
    private synchronized static Runtime ensureInitialised() {
        if (!initialisedOrInitialising) {
            initialisedOrInitialising = true;
            //it is possible that InitialisedRuntime's constructor may
            //cause certain customer systems to reentrantly call ensureInitialised()
            //which we must guard against with initialisedOrInitialising
            runtime = new InitialisedRuntime();
        }
        return runtime;
    }

    public static boolean isInitialised() {
        return runtime instanceof InitialisedRuntime;
    }

    ///CLOVER:OFF
    private Clover() {
    }
    ///CLOVER:ON

    /**
     * tell all reachable Clover recorders to flush
     */
    @ForInstrumentation
    public static void globalFlush() {
        ensureInitialised().globalFlush();
    }

    public static int getCurrentSlice() {
        return ensureInitialised().getCurrentSlice();
    }

    public static String getCurrentType() {
        return ensureInitialised().getCurrentType();
    }

    public static long getCurrentSliceStart() {
        return ensureInitialised().getCurrentSliceStart();
    }

    public static int getCurrentTestRunID() {
        return ensureInitialised().getCurrentTestRunID();
    }

    @ForInstrumentation
    public static CoverageRecorder getRecorder(
            final String initChars, final long dbVersion, final long cfgbits, final int maxNumElements,
            final CloverProfile[] profiles, final String[] nvpProperties) {
        return ensureInitialised().getRecorder(initChars, dbVersion, cfgbits, maxNumElements, profiles, nvpProperties);
    }

    public static int getTypeID(String runtimeType) {
        return ensureInitialised().getTypeID(runtimeType);
    }

    /**
     * For testing purposes only
     */
    public static void resetRecorders() {
        ensureInitialised().resetRecorders();
    }

    /**
     * @return whether any coverage recording has been attempted
     */
    public static boolean hasRecorded() {
        return ensureInitialised().hasRecorded();
    }

    public static void allRecordersFlush() {
        ensureInitialised().allRecordersFlush();
    }

    /**
     * Remebmer to update {@link com.atlassian.clover.remote.RpcMessage#METHODS} if you change method's signature.
     */
    public static void allRecordersSliceStart(final String type, final int slice, final long startTime) {
        ensureInitialised().allRecordersSliceStart(type, slice, startTime);
    }

    /**
     * Remebmer to update {@link com.atlassian.clover.remote.RpcMessage#METHODS} if you change method signature.
     */
    public static void allRecordersSliceEnd(final String type, final String method, final String runtimeTestName,
                                            final int slice, final int p, final ErrorInfo ei) {
        ensureInitialised().allRecordersSliceEnd(type, method, runtimeTestName, slice, p, ei);
    }

    /**
     * This is only public for CompilationCombinationTest#recordExecution
     */
    public static CoverageRecorder createRecorder(String dbName, long dbVersion, long cfgbits, int maxNumElements,
            CloverProfile profile, CloverProperties properties) {
        return ensureInitialised().createRecorder(dbName, dbVersion, cfgbits, maxNumElements, profile, properties);
    }

    // these two methods live here rather than in RecordingFile to limit the recording-time dependencies. RecordingFile
    // makes use of regex matchers which require JDK 1.4.

    /**
     * Returns Math.abs() for integer value. It ensures that returned value is always non-negative
     * (yes, Math.abs() can return negative value). In case of value=-2147483648 it returns
     * the nearest possible absolute value i.e. +2147483647.
     *
     * @param value input value
     * @return int non-negative Math.abs()
     */
    private static int abs(int value) {
        return (value != Integer.MIN_VALUE ? Math.abs(value) : Integer.MAX_VALUE);
    }

    /**
     * Returns Math.abs() for long value. It ensures that returned value is always non-negative
     * (yes, Math.abs() can return negative value). In case of value=-9223372036854775808 it returns
     * the nearest possible absolute value, i.e. +9223372036854775807.
     *
     * @param value input value
     * @return long non-negative Math.abs()
     */
    private static long abs(long value) {
        return (value != Long.MIN_VALUE ? Math.abs(value) : Long.MAX_VALUE);
    }

    /**
     * Generate a unique file name for recording snapshot.
     *
     * @param hash   unique hash identifying the coverage recorder
     * @param dbname base name of the clover database (used as prefix)
     * @param timestamp     snapshot timestamp
     * @return String generated file name
     * @see com.atlassian.clover.recorder.RecordingTranscripts#STD_REC_SUFFIX - regexp for recording name
     */
    public static String getRecordingName(int hash, String dbname, long timestamp) {
        // make sure that we don't have a negative number to ensure that generated suffix components
        // match '[0-9a-z]*' pattern. yep, Math.abs can return negative value
        return dbname
                + Integer.toString(abs(hash), 36) + "_"
                + Long.toString(abs(timestamp), 36);
    }

    /**
     * Generate a unique file name for test slice recording snapshot.
     *
     * @param typeid    the runtime type id of the slice
     * @param methodID  test method identified
     * @param runID     number of test run of the method
     * @param hash      unique hash identifying the coverage recorder
     * @param dbname    base name of the clover database (used as prefix)
     * @param timestamp snapshot timestamp
     * @return String generated file name
     * @see com.atlassian.clover.recorder.RecordingTranscripts#SLICE_SUFFIX - regexp for slice recording name
     */
    public static String getSliceRecordingName(int typeid, int methodID, int runID, int hash, String dbname, long timestamp) {
        long globalSliceID = ((long) typeid) << 32 | methodID;
        // make sure that we don't have a negative number to ensure that generated suffix components
        // match '[0-9a-z]*' pattern
        return dbname
                + Long.toString(abs(globalSliceID), 36) + "_"
                + Integer.toString(abs(runID), 36) + "_"
                + Integer.toString(abs(hash), 36) + "_"
                + Long.toString(abs(timestamp), 36) + ".s";
    }

    ///CLOVER:OFF
    @ForInstrumentation
    public static void l(String m) {
        ensureInitialised().l(m);
    }

    @ForInstrumentation
    public static void l(String m, Throwable t) {
        ensureInitialised().l(m, t);
    }
    ///CLOVER:ON

    /**
     * @return an ErrorInfo holding the message and stactrace of Throwable t. *
     */
    public static ErrorInfo getErrorInfo(Throwable t) {
        return ensureInitialised().getErrorInfo(t);
    }

    @ForInstrumentation
    public static CoverageRecorder getNullRecorder() {
        return NullRecorder.INSTANCE;
    }

    public static String stackTraceFor(Throwable throwable) {
        return ensureInitialised().stackTraceFor(throwable);
    }

    /**
     * Utility method for resolving the registry file. Is safe to run whether Clover is initialised or not
     */
    static File resolveRegistryFile(String dbName, CloverProperties properties) {
        try {
            String initStringProp = Environment.substituteSysPropRefs(properties.getProperty(CloverNames.PROP_INITSTRING));
            if (initStringProp != null && initStringProp.length() > 0) {
                Logger.getInstance().verbose("overriding initstring: " + initStringProp);
                return new File(initStringProp);
            }
            String initStringBaseProp = Environment.substituteSysPropRefs(
                    properties.getProperty(CloverNames.PROP_INITSTRING_BASEDIR));
            if (initStringBaseProp != null && initStringBaseProp.length() > 0) {
                Logger.getInstance().verbose("overriding initstring basedir: " + initStringBaseProp);
                File dbFile = new File(dbName);
                return new File(initStringBaseProp, dbFile.getName());
            }
            String initStringPrefixProp = Environment.substituteSysPropRefs(
                    properties.getProperty(CloverNames.PROP_INITSTRING_PREFIX));
            if (initStringPrefixProp != null && initStringPrefixProp.length() > 0) {
                // HACK - this code does not take into acccount any drive letters that may be present
                // on dbName. To use a prefix, users should set relative="yes" to prevent the dbName being resolved
                // prior to this method
                Logger.getInstance().verbose("prepending initstring prefix: " + initStringPrefixProp);
                String newInit = initStringPrefixProp + dbName;
                // handle the case where two separators collide
                if ((initStringPrefixProp.endsWith("/") || initStringPrefixProp.endsWith("\\")) &&
                        (dbName.startsWith("\\") || dbName.startsWith("/"))) {
                    newInit = initStringPrefixProp + dbName.substring(1);
                }
                return new File(newInit);
            }
        } catch (SecurityException e) {
            ///CLOVER:OFF
            Logger.getInstance().verbose("Failed to retrieve Clover properties " + CloverNames.PROP_INITSTRING + "*", e);
            ///CLOVER:ON
            // ignore. can't do much if no perms, and don't want to complain about it because
            // the user most probably hasn't set any anyway.
        }
        return new File(dbName);
    }

    /**
     * Clover's runtime
     */
    private interface Runtime {
        void globalFlush();

        int getCurrentSlice();

        String getCurrentType();

        long getCurrentSliceStart();

        int getCurrentTestRunID();

        CoverageRecorder getRecorder(String initChars, long dbVersion, long cfgbits, int maxNumElements,
                CloverProfile[] profiles, String... nvpProperties);

        int getTypeID(String runtimeType);

        void resetRecorders();

        boolean hasRecorded();

        void allRecordersFlush();

        /**
         * Remebmer to update {@link com.atlassian.clover.remote.RpcMessage#METHODS} if you change method's signature.
         */
        void allRecordersSliceStart(String type, int slice, long startTime);

        /**
         * Remebmer to update {@link com.atlassian.clover.remote.RpcMessage#METHODS} if you change method's signature.
         */
        void allRecordersSliceEnd(String type, String method, String runtimeTestName, int slice, int p, ErrorInfo ei);

        CoverageRecorder createRecorder(String dbName, long dbVersion, long cfgbits, int maxNumElements,
                CloverProfile profile, CloverProperties properties);

        void l(String m);

        void l(String m, Throwable t);

        ErrorInfo getErrorInfo(Throwable t);

        String stackTraceFor(Throwable throwable);
    }

    /**
     * The unititialised Clover runtime. Most methods will fail with an IllegalStateException.
     * Notable exceptions are getRecorder(...) methods which will return a
     * {@link com.atlassian.clover.recorder.DelayedRecorder}
     * instance which will start collecting coverage once Clover is initialised
     */
    private static class UninitialisedRuntime implements Runtime {
        private void throwNotInitialisedException() {
            throw new IllegalStateException("Clover runtime not yet initialised.");
        }

        @Override
        public void globalFlush() {
            //no-op
        }

        @Override
        public boolean hasRecorded() {
            return false;
        }

        @Override
        public void l(String m) {
            //no-op
        }

        @Override
        public void l(String m, Throwable t) {
            //no-op
        }

        @Override
        public CoverageRecorder getRecorder(String initString, long dbVersion, long cfgbits, int maxNumElements,
                CloverProfile[] profiles, String... nvpProperties) {
            return new DelayedRecorder(initString, dbVersion, cfgbits, maxNumElements, profiles, nvpProperties);
        }

        @Override
        public int getCurrentSlice() {
            throwNotInitialisedException();
            return 0;
        }

        @Override
        public String getCurrentType() {
            throwNotInitialisedException();
            return null;
        }

        @Override
        public long getCurrentSliceStart() {
            throwNotInitialisedException();
            return 0;
        }

        @Override
        public int getCurrentTestRunID() {
            throwNotInitialisedException();
            return 0;
        }

        @Override
        public int getTypeID(String runtimeType) {
            throwNotInitialisedException();
            return 0;
        }

        @Override
        public void resetRecorders() {
            throwNotInitialisedException();
        }

        @Override
        public void allRecordersFlush() {
            throwNotInitialisedException();
        }

        @Override
        public void allRecordersSliceStart(String type, int slice, long startTime) {
            throwNotInitialisedException();
        }

        /**
         * Remebmer to update {@link com.atlassian.clover.remote.RpcMessage#METHODS} if you change method signature.
         */
        @Override
        public void allRecordersSliceEnd(String type, String method, String runtimeTestName,
                                         int slice, int p, ErrorInfo ei) {
            throwNotInitialisedException();
        }

        @Override
        public CoverageRecorder createRecorder(String dbName, long dbVersion, long cfgbits, int maxNumElements,
                CloverProfile profile, CloverProperties properties) {
            throwNotInitialisedException();
            return null;
        }

        @Override
        public ErrorInfo getErrorInfo(Throwable t) {
            throwNotInitialisedException();
            return null;
        }

        @Override
        public String stackTraceFor(Throwable throwable) {
            throwNotInitialisedException();
            return null;
        }
    }

    /**
     * The initialised Clover runtime
     */
    private static class InitialisedRuntime implements Runtime {
        private int currentSlice = NO_SLICE;
        private long currentSliceStart = 0;
        private String currentType;
        private int typeID;

        private int testRunID = 0;
        private final Map<String, Integer> typeIDs;

        public InitialisedRuntime() {
            RecorderLogging.init();
            typeID = Math.abs((int) (System.currentTimeMillis() + Clover.class.hashCode()));
            typeIDs = Collections.synchronizedMap(new HashMap<String, Integer>());
        }

        /**
         * tell all reachable Clover recorders to flush
         */
        @Override
        public void globalFlush() {
            allRecordersFlush();
        }

        @Override
        public int getCurrentSlice() {
            return currentSlice;
        }

        @Override
        public String getCurrentType() {
            return currentType;
        }

        @Override
        public long getCurrentSliceStart() {
            return currentSliceStart;
        }

        @Override
        public int getCurrentTestRunID() {
            return testRunID;
        }

        /**
         * map of configured recorder instances *
         */
        private final HashMap<String, CoverageRecorder> RECORDERS = new HashMap<>();

        private DistributedClover distributedRuntime = null;

        /**
         * Cached value of 'clover.profile' system property
         */
        private volatile String cloverProfileName;

        /**
         * Gets called from a static initialisation block in an inner class in each instrumented Java source file.
         */
        @Override
        public CoverageRecorder getRecorder(final String initString,
                final long dbVersion,
                final long cfgbits,
                final int maxNumElements,
                final CloverProfile[] profiles,
                final String... nvpProperties) {

            final CloverProperties properties = new CloverProperties(nvpProperties);

            return AccessController.doPrivileged(new PrivilegedAction<CoverageRecorder>() {
                @Override
                public CoverageRecorder run() {
                    // find current profile (if any)
                    final CloverProfile currentProfile = selectCloverProfile(profiles);
                    // use proper key depending on coverage recorder type
                    final String recorderKey =
                            (currentProfile != null && currentProfile.getCoverageRecorder() == CloverProfile.CoverageRecorderType.SHARED) ?
                                    initString + "_" + cfgbits                      // shared
                                    : initString + "_" + dbVersion + "_" + cfgbits; // fixed or growable

                    CoverageRecorder recorder;
                    synchronized (RECORDERS) {
                        // find existing recorder ...
                        recorder = RECORDERS.get(recorderKey);
                        if (recorder != null) {
                            // ... and resize if necessary
                            Logger.getInstance().debug("[found existing recorder for " + recorderKey + "]");
                            recorder = recorder.withCapacityFor(maxNumElements);
                        } else {
                            // ... or create new one
                            if (Logger.isDebug()) {
                                Logger.getInstance().debug(
                                        "Clover.getRecorder("
                                                + initString + ", "
                                                + dbVersion + ", "
                                                + cfgbits + ", "
                                                + maxNumElements + ", "
                                                + properties + ") "
                                                + "resulting in new recorder called from (first 10 stack elements):\n" +
                                                callerChain(10));
                            }

                            Logger.getInstance().debug("[creating new recorder for " + recorderKey + "]");
                            recorder = createRecorder(initString, dbVersion, cfgbits, maxNumElements, currentProfile, properties);
                            recorder.startRun();
                        }

                        // growable/shared recorders may have emitted a new proxy so use this as the latest version
                        RECORDERS.put(recorderKey, recorder);

                        if (distributedRuntime == null) {
                            distributedRuntime = new DistributedClover(properties, currentProfile);
                        }

                        return recorder;
                    }
                }
            });
        }

        private String callerChain(int maxDepth) {
            final LinkedList<StackTraceElement> elements = new LinkedList<>(
                    Arrays.asList(new Exception().getStackTrace()));
            elements.removeFirst();
            elements.removeFirst();
            final StringBuilder buf = new StringBuilder();
            final Iterator<StackTraceElement> iterator = elements.iterator();
            int depth = 0;
            while (iterator.hasNext() && depth < maxDepth) {
                final StackTraceElement element = iterator.next();
                if (element.getClassName().startsWith("sun.reflect")
                        || element.getClassName().startsWith("java.lang.reflect")) {
                    iterator.remove();
                } else {
                    buf.append(element);
                    buf.append("\n");
                    depth++;
                }
            }
            return buf.toString();
        }

        @Override
        public int getTypeID(String runtimeType) {
            int result;
            Integer val = typeIDs.get(runtimeType);
            if (val == null) {
                //Not thread-safe but also not important (implies a multi-threaded test which we don't support)
                result = typeID++;
                typeIDs.put(runtimeType, result);
            } else {
                result = val;
            }
            return result;
        }

        /**
         * For testing purposes only
         */
        @Override
        public void resetRecorders() {
            synchronized (RECORDERS) {
                RECORDERS.clear();
            }
        }

        /**
         * @return whether any coverage recording has been attempted
         */
        @Override
        public boolean hasRecorded() {
            return RECORDERS.size() > 0;
        }

        @Override
        public void allRecordersFlush() {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    synchronized (RECORDERS) {
                        for (CoverageRecorder recorder : RECORDERS.values()) {
                            recorder.forceFlush();
                        }
                    }
                    return null;
                }
            });
        }

        @Override
        public void allRecordersSliceStart(final String type, final int slice, final long startTime) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    synchronized (RECORDERS) {
                        currentSlice = slice;
                        currentSliceStart = startTime > 0 ? startTime : System.currentTimeMillis();
                        currentType = type;

                        for (CoverageRecorder recorder : RECORDERS.values()) {
                            recorder.sliceStart(type, currentSliceStart, slice, testRunID);
                        }
                        if (distributedRuntime != null) {
                            // see CajoTcpRecorderListener.allRecordersSliceStart signature
                            distributedRuntime.remoteFlush(
                                    RpcMessage.createMethodStart(type, slice, currentSliceStart));
                        }
                    }
                    return null;
                }
            });
        }

        @Override
        public void allRecordersSliceEnd(final String type, final String method, @Nullable final String runtimeTestName,
                                         final int slice, final int p, final ErrorInfo ei) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    synchronized (RECORDERS) {
                        currentSlice = NO_SLICE;
                        long ts = System.currentTimeMillis();
                        for (CoverageRecorder recorder : RECORDERS.values()) {
                            recorder.sliceEnd(type, method, runtimeTestName, ts, slice, testRunID, p, ei);
                        }
                        if (distributedRuntime != null) {
                            // see CajoTcpRecorderListener.allRecordersSliceEnd signature
                            distributedRuntime.remoteFlush(
                                    RpcMessage.createMethodEnd(type, method, runtimeTestName, slice, p, ei));
                        }
                        testRunID++;
                    }

                    return null;
                }
            });

        }

        /**
         * This is only public for CompilationCombinationTest#recordExecution
         */
        @Override
        public CoverageRecorder createRecorder(String dbName, long dbVersion, long cfgbits, int maxNumElements,
                CloverProfile currentProfile, CloverProperties properties) {
            File dbFile = resolveRegistryFile(dbName, properties);

            try {
                // return null recorder if user wants to disable Clover at runtime
                if (isDisableClover()) {
                    Logger.getInstance().verbose("CLOVER: The system property '" + CloverNames.PROP_ENABLE + "' is set to false. Coverage recording is disabled.");
                    return NullRecorder.INSTANCE;
                }

                if ( (currentProfile == null || currentProfile.getCoverageRecorder() == CloverProfile.CoverageRecorderType.FIXED)
                        && (dbFile.exists() && !dbFile.isDirectory() && dbFile.canRead()) ) {
                    // fixed coverage recorder when: no profiles or specified in a profile and database is accessible
                    return FixedSizeCoverageRecorder.createFor(dbFile, dbVersion, maxNumElements, cfgbits);
                } else if (currentProfile != null && currentProfile.getCoverageRecorder() == CloverProfile.CoverageRecorderType.GROWABLE) {
                    // growable coverage recorder: when specified in a profile
                    return GrowableCoverageRecorder.createFor(dbFile.getAbsolutePath(), dbVersion, cfgbits, maxNumElements);
                } else if (currentProfile != null && currentProfile.getCoverageRecorder() == CloverProfile.CoverageRecorderType.SHARED) {
                    // shared coverage recorder: when specified in a profile
                    return SharedCoverageRecorder.createFor(dbFile.getAbsolutePath(), dbVersion, cfgbits, maxNumElements);
                } else {
                    logRecorderCreationFailure(dbFile, null);
                }
            } catch (SecurityException e) {
                Logger.getInstance().warn(SECURITY_EXCEPTION_MSG);
                logRecorderCreationFailure(dbFile, e);
            } catch (Throwable e) {
                logRecorderCreationFailure(dbFile, e);
                if (e instanceof Error) {
                    throw (Error) e;
                }
            }

            return NullRecorder.INSTANCE;
        }

        /**
         * Returns true if Clover coverage recording shall be disabled at runtime.
         * @return true if disabled, false if enabled
         */
        private boolean isDisableClover() {
            try {
                String cloverEnable = System.getProperty(CloverNames.PROP_ENABLE);
                // disable Clover only if user explicitly sets this property to false
                return cloverEnable != null && (cloverEnable.equalsIgnoreCase("false") || cloverEnable.equalsIgnoreCase("no"));
            } catch (SecurityException ex) {
                Logger.getInstance().verbose("Unable to read '" + CloverNames.PROP_ENABLE + "' property. Assuming that Clover is enabled.");
                return false;
            }
        }

        /**
         * Select proper runtime profile:
         * <pre>
         * 1. profiles are not empty?
         *    YES: 2. read {@link CloverNames#PROP_CLOVER_PROFILE} property. success?</li>
         *            YES: ok
         *            NO: assume profile name = "default"
         *         3. find profile on a list. found?
         *            YES: return profile
         *            NO: return <code>null</code>
         *    NO: return <code>null</code>
         * </pre>
         *
         * Note: call in AccessController.doPrivileged() as it reads system property.
         *
         * @param profiles available profiles to choose from
         * @return CloverProfile or <code>null</code>
         */
        private CloverProfile selectCloverProfile(final CloverProfile[] profiles) {
            // 1. profiles are empty?
            if (profiles == null || profiles.length == 0) {
                Logger.getInstance().debug("CLOVER: No profiles defined in instrumented classes. Using standard settings.");
                return null;
            }

            // 2. read system property or take cached value
            if (cloverProfileName == null) {
                synchronized(this) {
                    try {
                        cloverProfileName = System.getProperty(CloverNames.PROP_CLOVER_PROFILE);
                        if (cloverProfileName == null) {
                            Logger.getInstance().debug("CLOVER: System property '" + CloverNames.PROP_CLOVER_PROFILE
                                    + "' was not found. Assuming the 'default' profile.");
                            cloverProfileName = CloverProfile.DEFAULT_NAME;
                        }
                    } catch (SecurityException ex) {
                        Logger.getInstance().verbose("CLOVER: Unable to read '" + CloverNames.PROP_CLOVER_PROFILE +
                                "' system property. Assuming the 'default' profile.", ex);
                        cloverProfileName = CloverProfile.DEFAULT_NAME;
                    }
                }
            }

            // 3. profile found?
            for (CloverProfile profile : profiles) {
                if (profile.getName().equals(cloverProfileName)) {
                    Logger.getInstance().verbose(
                            "CLOVER: Using profile '" + cloverProfileName + "' with settings "
                                    + "[coverageRecorder=" + profile.getCoverageRecorder()
                                    + ( profile.getDistributedCoverage() != null
                                            ? " distributedCoverage=" + profile.getDistributedCoverage().getConfigString()
                                            : "")
                                    + "]");
                    return profile;
                }
            }

            Logger.getInstance().verbose(
                    "CLOVER: Profile '" + cloverProfileName + "' not found in instrumented classes. Using standard settings.");
            return null;
        }

        private void logRecorderCreationFailure(File dbFile, Throwable t) {
            Logger.getInstance().error("CLOVER: Unable to load the coverage database at \"" + dbFile.getAbsolutePath() + "\"");
            Logger.getInstance().error("CLOVER: No coverage data will be gathered.");
            if (t != null) {
                Logger.getInstance().error("CLOVER: " + t.getClass().getName());
                if (t.getMessage() != null) {
                    Logger.getInstance().error("CLOVER: " + t.getMessage(), t);
                }
            }
        }

        ///CLOVER:OFF
        @Override
        public void l(String m) {
            Logger.getInstance().error(m);
        }

        @Override
        public void l(String m, Throwable t) {
            Logger.getInstance().error(m, t);
        }
        ///CLOVER:ON

        /**
         * @return an ErrorInfo holding the message and stactrace of Throwable t. *
         */
        @Override
        public ErrorInfo getErrorInfo(Throwable t) {
            return t == null ? null : new ErrorInfo(t.getMessage(), stackTraceFor(t));
        }

        @Override
        public String stackTraceFor(Throwable throwable) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
    }
}
