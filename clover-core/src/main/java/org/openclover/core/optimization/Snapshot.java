package org.openclover.core.optimization;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageDataSpec;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.io.tags.ObjectReader;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedIO;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.core.io.tags.Tags;
import org.openclover.core.io.tags.UnknownTagException;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.api.registry.CoverageDataRange;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.util.Sets;
import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org_openclover_runtime.CloverVersionInfo;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Sets.newHashSet;

/**
 * A snapshot of the known state of testing activity relating to a Clover-enabled project
 * and its database.
 **/
public class Snapshot implements TaggedPersistent {

    public static final long UNKNOWN_DURATION = Long.MIN_VALUE;

    /**
     * On-disk format version. Bump whenever the persisted field layout changes so
     * that snapshots written by an incompatible version are rejected cleanly rather
     * than mis-decoded. Kept in step with the registry format version.
     */
    private static final int SNAPSHOT_FORMAT_VERSION = 50001;

    /**
     * Whitelist of the types that can appear in a serialized snapshot. Only these
     * types can ever be instantiated by the reader; anything else fails with
     * {@link UnknownTagException}.
     * <p>
     * The tag numbers deliberately start at {@code NEXT_TAG + 100} to occupy a
     * distinct range from other tag tables (e.g. {@code InstrSessionSegment.TAGS}
     * uses {@code NEXT_TAG + 0..}), so that accidentally reading a stream with the
     * wrong table fails fast with an {@link UnknownTagException} instead of
     * silently mis-decoding.
     */
    static final Tags TAGS =
        new Tags()
            .registerTag(Snapshot.class.getName(), Tags.NEXT_TAG + 100, (ObjectReader<Snapshot>) Snapshot::read)
            .registerTag(TestMethodCall.class.getName(), Tags.NEXT_TAG + 101, (ObjectReader<TestMethodCall>) TestMethodCall::read)
            .registerTag(SourceState.class.getName(), Tags.NEXT_TAG + 102, (ObjectReader<SourceState>) SourceState::read);

    /**
     * Record the version this snapshot was created with as subsequent
     * builds with different versions will trigger full builds.
     **/
    private final String cloverVersionInfo;
    /**
     * Database versions this snapshot was generated for. More than one
     * version indicates this snapshot has been updated multiple times.
     **/
    private final Set<Long> dbVersions;
    /**
     * The init string of the database this instance is snapshoting
     **/
    private final String initString;

    /**
     * Maps from a string to a collection of {@link TestMethodCall}s.
     * The strings will be any (possibly all) of the following forms:
     * "com/foo/bar/Baz.java"
     * "com.foo.bar.Baz"
     * "com.foo.bar.Baz.testMethod"
     */
    private final Map<String, Set<TestMethodCall>> testLookup;

    /**
     * Map from tests that have been run to their duration in milliseconds.
     **/
    private final Object2LongMap/*<TestMethodCall, long>*/ durationsForTests;

    /**
     * The paths to all known failing tests. Paths are
     * file package paths e.g. com/foo/MyClass.java
     **/
    private final Set<TestMethodCall> failingTests;

    /**
     * Map from TestMEthodCall to a map of file package paths to SourceState.
     * E.g. {TestMethodCall("com.foo.FooTest.testMethod()") -> {"com/foo/Foo.java" -> SourceState(filesize=1000, checksum=29878}}
     */
    private final Map<TestMethodCall, Map<String, SourceState>> perTestSourceStates;

    private long avgSetupTeardownDuration;

    private transient File location;

    private static boolean DEBUG;

    public Snapshot(final CloverDatabase db, File locationTosnapshot) {
        cloverVersionInfo = CloverVersionInfo.formatVersionInfo();
        dbVersions = new LinkedHashSet<>();
        initString = db.getInitstring();
        testLookup = newHashMap();
        perTestSourceStates = newHashMap();
        durationsForTests = newDurationsMap();
        failingTests = newHashSet();
        location = locationTosnapshot;
        updateFor(db);
    }

    /**
     * Field-assigning constructor used when reconstructing a snapshot from disk
     * (see {@link #read(TaggedDataInput)}). {@link #location} is set by the caller.
     */
    private Snapshot(String cloverVersionInfo, Set<Long> dbVersions, String initString,
                     Map<String, Set<TestMethodCall>> testLookup,
                     Object2LongMap/*<TestMethodCall, long>*/ durationsForTests,
                     Set<TestMethodCall> failingTests,
                     Map<TestMethodCall, Map<String, SourceState>> perTestSourceStates,
                     long avgSetupTeardownDuration) {
        this.cloverVersionInfo = cloverVersionInfo;
        this.dbVersions = dbVersions;
        this.initString = initString;
        this.testLookup = testLookup;
        this.durationsForTests = durationsForTests;
        this.failingTests = failingTests;
        this.perTestSourceStates = perTestSourceStates;
        this.avgSetupTeardownDuration = avgSetupTeardownDuration;
    }

    private static Object2LongMap newDurationsMap() {
        return new Object2LongOpenHashMap() {
            @Override
            public long defaultReturnValue() {
                return UNKNOWN_DURATION;
            }
        };
    }

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public void updateFor(final CloverDatabase db) {
        long updateStart = System.currentTimeMillis();

        TestRunTimings testTimings = updateFailedTestsAndTestDurations(db);
        if (isFirstUpdate()) {
            avgSetupTeardownDuration = calcAvgSetupTeardownDuration(testTimings);
        }

        calcHits(db);

        Logger.getInstance().verbose(
            "Took " + (System.currentTimeMillis() - updateStart) + "ms to " + (isFirstUpdate() ? "initialise" : "update") + " the snapshot");

        pushVersion(db);
    }

    private void pushVersion(CloverDatabase db) {
        dbVersions.add(db.getRegistry().getVersion());
    }

    private boolean isFirstUpdate() {
        return dbVersions.size() == 0;
    }

    private TestRunTimings updateFailedTestsAndTestDurations(CloverDatabase db) {
        long earliestStart = Long.MAX_VALUE;
        long latestEnd = 0L;
        long totalTestTime = 0L;
        long started = System.currentTimeMillis();

        int testCount = 0;

        @SuppressWarnings("unchecked")
        final Set<TestCaseInfo> allTestCaseInfos = db.getCoverageData().getTests();
        for (TestCaseInfo tci : allTestCaseInfos) {
            testCount++;

            long duration = tci.getEndTime() - tci.getStartTime();
            totalTestTime += duration;
            earliestStart = Math.min(earliestStart, tci.getStartTime());
            latestEnd = Math.max(latestEnd, tci.getEndTime());
            updatePerTestInfo(db, tci, duration);
        }

        Logger.getInstance().verbose("Took " + (System.currentTimeMillis() - started) + "ms to process all test durations");
        if (testCount == 0) {
            Logger.getInstance().verbose(
                "No test results found in the OpenClover database. Please ensure the source files containing test "
                 + "classes have been instrumented by OpenClover and the tests have been run.");
        } else {
            Logger.getInstance().verbose(
                "Number of test results found in the model: " + testCount);
        }
        return new TestRunTimings(earliestStart, latestEnd, totalTestTime);
    }

    public void updatePerTestInfo(CloverDatabase db, TestCaseInfo tci, long duration) {
        final TestMethodCall testCall = TestMethodCall.createFor(db.getFullModel(), tci);
        if (testCall != null) {
            //Map {"com.foo.bar.Baz.testMethod" -> set<TestMethodCall>}
            //A mutable hashset is used instead of a singleton set to avoid problems if there are
            //name collisions between FQ class and method names. This should not be a problem in practice
            //but preparing for the worst.

            addToTestlookup(testCall.getSourceMethodName(), testCall);
            //A test method on a base class has been called
            if (testCall.isInheritedCall()) {
                addToTestlookup(testCall.getRuntimeMethodName(), testCall);
            }

            //Map {"com/foo/bar/Baz.java" -> set<TestMethodCall>}
            addToTestlookup(testCall.getPackagePath(), testCall);

            //Map {"com.foo.bar.Baz" -> set<TestMethodCall>}
            addToTestlookup(tci.getRuntimeTypeName(), testCall);

            if (tci.isSuccess()) {
                failingTests.remove(testCall);
            } else {
                failingTests.add(testCall);
            }
            if (DEBUG) {
                Logger.getInstance().debug("Duration for individual test '" + testCall + "' = " + duration);
            }
            durationsForTests.put(testCall, duration);
        }
    }

    private void addToTestlookup(String key, TestMethodCall testCall) {
        Set<TestMethodCall> tests = testLookup.get(key);
        tests = tests == null ? Sets.<TestMethodCall>newHashSet() : tests;
        tests.add(testCall);
        testLookup.put(key, tests);
    }

    private void addToStates(TestMethodCall test, String path, SourceState state) {
        Map<String, SourceState> perTestMap = perTestSourceStates.computeIfAbsent(test, k -> newHashMap());
        perTestMap.put(path, state);
    }

   @SuppressWarnings("unchecked")
   private void calcHits(final CloverDatabase db) {
        final long started = System.currentTimeMillis();

        db.getFullModel().visitFiles(fileInfo -> {
            final String packagePath = fileInfo.getPackagePath();
            final SourceState sourceState = new SourceState(fileInfo.getChecksum(), fileInfo.getFileSize());
            final Set<TestMethodCall> testsForFile = testsFor(db.getFullModel(), db.getTestHits((CoverageDataRange) fileInfo));
            for (TestMethodCall test : testsForFile) {
                addToStates(test, packagePath, sourceState);
            }
        });

        Logger.getInstance().verbose("Took " + (System.currentTimeMillis() - started) + "ms to correlate source files paths with test hits");
    }

    private long calcAvgSetupTeardownDuration(TestRunTimings timings) {
        long duration = 0;
        if ((durationsForTests.size() > 1
             && timings.totalTestTime > 0
             && timings.latestEnd > timings.earliestStart)) {

            final long firstToLastTestDuration = timings.latestEnd - timings.earliestStart;
            Logger.getInstance().verbose("Measured first-to-last test duration = " + firstToLastTestDuration + "ms");
            Logger.getInstance().verbose("Aggregate test duration = " + timings.totalTestTime + "ms");
            Logger.getInstance().verbose("Number of test methods = " + durationsForTests.size());
            duration = Math.max(0, ((firstToLastTestDuration - timings.totalTestTime) / (durationsForTests.size() - 1)));
        }
        Logger.getInstance().debug("Calculated average per-test setup/teardown cost = " + duration + "ms");
        return duration;
    }

    private Set<TestMethodCall> testsFor(ProjectInfo project, Collection<TestCaseInfo> tcis) {
        Set<TestMethodCall> tests = newHashSet();
        for (final TestCaseInfo tci : tcis) {
            String testName = TestMethodCall.getSourceMethodNameFor(tci, project);
            Set<TestMethodCall> testsForName = testName == null ? null : testLookup.get(testName);
            if (testsForName != null) {
                tests.addAll(testsForName);
            }
        }
        return tests;
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.writeInt(SNAPSHOT_FORMAT_VERSION);
        out.writeUTF(cloverVersionInfo);
        out.writeUTF(initString);
        out.writeLong(avgSetupTeardownDuration);

        writeDbVersions(out, dbVersions);
        writeTestLookups(out, testLookup);
        writeDurationsForTests(out, durationsForTests);
        writeFailingTests(out, failingTests);
        writePerTestSourceStates(out, perTestSourceStates);
    }

    private static void writeDbVersions(TaggedDataOutput out, Set<Long> dbVersions) throws IOException {
        out.writeInt(dbVersions.size());
        for (final Long version : dbVersions) {
            out.writeLong(version);
        }
    }

    private static void writeTestLookups(TaggedDataOutput out, Map<String, Set<TestMethodCall>> testLookup) throws IOException {
        out.writeInt(testLookup.size());
        for (final Map.Entry<String, Set<TestMethodCall>> entry : testLookup.entrySet()) {
            out.writeUTF(entry.getKey());
            final Set<TestMethodCall> tests = entry.getValue();
            out.writeInt(tests.size());
            for (final TestMethodCall test : tests) {
                out.write(TestMethodCall.class, test);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void writeDurationsForTests(TaggedDataOutput out, Object2LongMap durationsForTests) throws IOException {
        out.writeInt(durationsForTests.size());
        for (final Object key : durationsForTests.keySet()) {
            final TestMethodCall test = (TestMethodCall) key;
            out.write(TestMethodCall.class, test);
            out.writeLong(durationsForTests.getLong(test));
        }
    }

    private static void writeFailingTests(TaggedDataOutput out, Set<TestMethodCall> failingTests) throws IOException {
        out.writeInt(failingTests.size());
        for (final TestMethodCall test : failingTests) {
            out.write(TestMethodCall.class, test);
        }
    }

    private static void writePerTestSourceStates(TaggedDataOutput out,
                                                 Map<TestMethodCall, Map<String, SourceState>> perTestSourceStates) throws IOException {
        out.writeInt(perTestSourceStates.size());
        for (final Map.Entry<TestMethodCall, Map<String, SourceState>> entry : perTestSourceStates.entrySet()) {
            out.write(TestMethodCall.class, entry.getKey());
            final Map<String, SourceState> states = entry.getValue();
            out.writeInt(states.size());
            for (final Map.Entry<String, SourceState> stateEntry : states.entrySet()) {
                out.writeUTF(stateEntry.getKey());
                out.write(SourceState.class, stateEntry.getValue());
            }
        }
    }

    public static Snapshot read(TaggedDataInput in) throws IOException {
        final int formatVersion = in.readInt();
        if (formatVersion != SNAPSHOT_FORMAT_VERSION) {
            throw new IOException("Unsupported snapshot format version " + formatVersion
                + " (expected " + SNAPSHOT_FORMAT_VERSION + ")");
        }
        final String cloverVersionInfo = in.readUTF();
        final String initString = in.readUTF();
        final long avgSetupTeardownDuration = in.readLong();

        final Set<Long> dbVersions = readDbVersions(in);
        final Map<String, Set<TestMethodCall>> testLookup = readTestLookups(in);
        final Object2LongMap durationsForTests = readDurationsForTests(in);
        final Set<TestMethodCall> failingTests = readFailingTests(in);
        final Map<TestMethodCall, Map<String, SourceState>> perTestSourceStates = readPerTestSourceStates(in);

        return new Snapshot(cloverVersionInfo, dbVersions, initString, testLookup,
            durationsForTests, failingTests, perTestSourceStates, avgSetupTeardownDuration);
    }

    private static Set<Long> readDbVersions(TaggedDataInput in) throws IOException {
        final int count = in.readInt();
        final Set<Long> dbVersions = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            dbVersions.add(in.readLong());
        }
        return dbVersions;
    }

    private static Map<String, Set<TestMethodCall>> readTestLookups(TaggedDataInput in) throws IOException {
        final int count = in.readInt();
        final Map<String, Set<TestMethodCall>> testLookup = newHashMap();
        for (int i = 0; i < count; i++) {
            final String key = in.readUTF();
            final int testCount = in.readInt();
            final Set<TestMethodCall> tests = newHashSet();
            for (int j = 0; j < testCount; j++) {
                tests.add(in.read(TestMethodCall.class));
            }
            testLookup.put(key, tests);
        }
        return testLookup;
    }

    @SuppressWarnings("unchecked")
    private static Object2LongMap readDurationsForTests(TaggedDataInput in) throws IOException {
        final int count = in.readInt();
        final Object2LongMap durationsForTests = newDurationsMap();
        for (int i = 0; i < count; i++) {
            final TestMethodCall test = in.read(TestMethodCall.class);
            final long duration = in.readLong();
            durationsForTests.put(test, duration);
        }
        return durationsForTests;
    }

    private static Set<TestMethodCall> readFailingTests(TaggedDataInput in) throws IOException {
        final int count = in.readInt();
        final Set<TestMethodCall> failingTests = newHashSet();
        for (int i = 0; i < count; i++) {
            failingTests.add(in.read(TestMethodCall.class));
        }
        return failingTests;
    }

    private static Map<TestMethodCall, Map<String, SourceState>> readPerTestSourceStates(TaggedDataInput in) throws IOException {
        final int count = in.readInt();
        final Map<TestMethodCall, Map<String, SourceState>> perTestSourceStates = newHashMap();
        for (int i = 0; i < count; i++) {
            final TestMethodCall test = in.read(TestMethodCall.class);
            final int stateCount = in.readInt();
            final Map<String, SourceState> states = newHashMap();
            for (int j = 0; j < stateCount; j++) {
                final String path = in.readUTF();
                states.put(path, in.read(SourceState.class));
            }
            perTestSourceStates.put(test, states);
        }
        return perTestSourceStates;
    }

    public void store() throws IOException {
        if (location.getParentFile() != null && !location.getParentFile().exists()) {
            location.getParentFile().mkdirs();
        }
        try (FileChannel channel = FileChannel.open(location.toPath(), WRITE, CREATE, TRUNCATE_EXISTING)) {
            TaggedIO.write(channel, TAGS, Snapshot.class, this);
        }
    }

    public static Snapshot generateFor(CloverDatabase db) {
        return new Snapshot(db, new File(fileNameForInitString(db.getInitstring())));
    }

    public static Snapshot generateFor(CloverDatabase db, String location) {
        return new Snapshot(db, new File(location));
    }

    public static Snapshot generateFor(String initString, String snapshotPath, CoverageDataSpec spec) throws CloverException {
        return new Snapshot(CloverDatabase.loadWithCoverage(initString, spec), new File(snapshotPath));
    }

    public static Snapshot generateFor(String initString) throws CloverException {
        return new Snapshot(CloverDatabase.loadWithCoverage(initString, new CoverageDataSpec()), fileForInitString(initString));
    }

    public static Snapshot loadFor(String initString) {
        return loadFrom(fileNameForInitString(initString));
    }

    public static Snapshot loadFrom(String path) {
        return loadFromFile(new File(path));
    }

    public static Snapshot loadFrom(File file) {
        return loadFromFile(file);
    }

    public static Snapshot loadFromFile(File file) {
        if (file.exists() && file.isFile() && file.canRead()) {
            try {
                try (FileChannel channel = FileChannel.open(file.toPath(), READ)) {
                    long start = System.currentTimeMillis();
                    Snapshot snapshot = TaggedIO.read(channel, TAGS, Snapshot.class);
                    Logger.getInstance().verbose("Took " + (System.currentTimeMillis() - start) + "ms to load the snapshot file");
                    snapshot.location = file;
                    return snapshot;
                }
            } catch (UnknownTagException e) {
                Logger.getInstance().debug("Failed to load snapshot file at " + file.getAbsolutePath(), e);
                Logger.getInstance().warn("Failed to load snapshot file at " + file.getAbsolutePath() +
                        " because it is no longer valid for this version of OpenClover");
            } catch (Exception e) {
                Logger.getInstance().debug("Failed to load snapshot file at " + file.getAbsolutePath(), e);
                Logger.getInstance().warn("Failed to load snapshot file at " + file.getAbsolutePath());
            }
        } else {
            Logger.getInstance().verbose(
                "Snapshot file at " + file.getAbsolutePath() + " exists / is file / can read: "
                + file.exists() + " / "
                + file.isFile() + " / "
                + file.canRead() + " / ");
        }
        return null;
    }

    public boolean delete() {
        return location.exists() && location.delete();
    }

    public Set<String> getFailingTestPaths() {
        return pathsFor(failingTests);
    }

    private boolean isChangedFile(SourceState fileReference, FileInfo file) {
        return
            fileReference != null
            && file instanceof FullFileInfo
            && ((FullFileInfo)file).changedFrom(fileReference.getChecksum(), fileReference.getFilesize());
    }

    private Set<String> pathsFor(Set<TestMethodCall> tests) {
        Set<String> paths = new HashSet<>(tests.size());
        for (TestMethodCall testReference : tests) {
            paths.add(testReference.getPackagePath());
        }
        return paths;
    }

    public static String fileNameForInitString(String initString) {
        return initString + CloverNames.SNAPSHOT_SUFFIX;
    }

    public static File fileForInitString(String initString) {
        return new File(fileNameForInitString(initString));
    }

    public File getLocation() {
        return location;
    }

    public int getDbVersionCount() {
        return dbVersions.size();
    }

    public String getCloverVersionInfo() {
        return cloverVersionInfo;
    }

    public String getInitString() {
        return initString;
    }

    public Set<Long> getDbVersions() {
        return dbVersions;
    }

    long calculateDurationOf(Set<TestMethodCall> tests) {
        long duration = 0;
        for (TestMethodCall test : tests) {
            final long testFileDuration = durationsForTests.getLong(test);
            if (testFileDuration != UNKNOWN_DURATION) {
                duration += testFileDuration;
            }
        }
        return duration + (tests.size() * avgSetupTeardownDuration);
    }

    public boolean isTooStale(int maxOptimizedBuilds) {
        StringBuffer reason = new StringBuffer();
        boolean tooStale = isTooStale(maxOptimizedBuilds, reason);
        if (DEBUG) {
            Logger.getInstance().info(reason.toString());
        }
        return tooStale;
    }
    
    public boolean isTooStale(int maxOptimizedBuilds, StringBuffer reason) {

        if (!cloverVersionInfo.equals(CloverVersionInfo.formatVersionInfo())) {
            reason.append(Messages.noOptimizationBecauseOldVersion(cloverVersionInfo));
            return true;
        }
        
        //1st build is never optimized
        if ((getDbVersionCount() - 1) >= maxOptimizedBuilds) {
            reason.append(Messages.noOptimizationBecauseInaccurate(maxOptimizedBuilds, getDbVersionCount()));
            return true;
        }

        return false;
    }

    long getMostRecentDbVersion() {
        long version = 0;
        for (Long dbVersion : dbVersions) {
            if (dbVersion > version) {
                version = dbVersion;
            }
        }
        return version;
    }


    boolean isTestAffectedByChanges(TestMethodCall test, Clover2Registry registry, OptimizationSession session) {
        final Map<String, SourceState> perTestStates = perTestSourceStates.get(test);
        final boolean isAffected = perTestStates == null || hasChanges(test, perTestStates, registry, session);
        if (DEBUG) {
            if (perTestStates == null) {
                Logger.getInstance().info("Test " + test + " has no recorded coverage");
            } else {
                Logger.getInstance().info("Test " + test + " was affected by changed source: " + isAffected);
            }
        }
        return isAffected;
    }

    private boolean hasChanges(TestMethodCall testMethod, Map<String, SourceState> perTestStates, Clover2Registry registry, OptimizationSession session) {
        for (Map.Entry<String, SourceState> fileState : perTestStates.entrySet()) {
            final FileInfo fileInfo = registry.getProject().findFile(fileState.getKey());
            if (fileInfo == null || isChangedFile(fileState.getValue(), fileInfo)) {
                if (DEBUG) {
                    if (fileInfo == null) {
                        Logger.getInstance().info("Source file " + fileState.getKey() + " covered by test " + testMethod + " not found in model");
                    } else {
                        Logger.getInstance().info(
                            "Source file " + fileState.getKey() + " covered by test " + testMethod
                            + " changed (was: " + fileState.getValue()
                            + " now: " + new SourceState(fileInfo.getChecksum(), fileInfo.getFileSize()) + ")");
                    }
                }
                if (fileInfo != null) {
                    session.addModifiedPath(fileState.getKey());
                }
                return true;
            }
        }
        if (DEBUG) {
            Logger.getInstance().info("Test " + testMethod + " has no coverage or no source it covered has changed");
        }
        return false;
    }

    /**
     * Package-protected access to symbolically look up {@link TestMethodCall}s.
     * Primarily for use by {@link LocalSnapshotOptimizer}.
     **/
    Set<TestMethodCall> lookupTests(String name) {
        return testLookup.get(name);
    }

    /**
     * Package-protected access to the failing tests.
     * Primarily for {@link SnapshotPrinter}.
     **/
    Set<TestMethodCall> getFailingTests() {
        return failingTests;
    }

    /**
     * Package-protected access to the test lookup.
     * Primarily for {@link SnapshotPrinter}.
     **/
    Map<String, Set<TestMethodCall>> getTestLookup() {
        return testLookup;
    }

    /**
     * Package-protected access to the test lookup.
     * Primarily for {@link SnapshotPrinter}.
     **/
    Map<String, Collection<TestMethodCall>> getFile2TestsMap() {
        final Map<String, Collection<TestMethodCall>> result = newHashMap();
        for (Map.Entry<TestMethodCall, Map<String, SourceState>> mapEntry : perTestSourceStates.entrySet()) {
            final TestMethodCall test = mapEntry.getKey();
            final Map<String, SourceState> value = mapEntry.getValue();
            for (String filePath : value.keySet()) {
                Collection<TestMethodCall> tests = result.computeIfAbsent(filePath, k -> newHashSet());
                tests.add(test);
            }
        }
        return result;
    }

    /** Records the inferred start, end and total test time of a test run. */
    private static final class TestRunTimings {
        private final long earliestStart;
        private final long latestEnd;
        private final long totalTestTime;

        private TestRunTimings(long earliestStart, long latestEnd, long totalTestTime) {
            this.earliestStart = earliestStart;
            this.latestEnd = latestEnd;
            this.totalTestTime = totalTestTime;
        }
    }

}