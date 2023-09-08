package com.atlassian.clover.optimization;

import clover.it.unimi.dsi.fastutil.objects.Object2LongMap;
import clover.it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.CloverNames;
import com.atlassian.clover.CoverageDataSpec;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.registry.entities.BaseFileInfo;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.registry.CoverageDataRange;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.FileInfoVisitor;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com_atlassian_clover.CloverVersionInfo;
import org.openclover.util.Sets;

import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.openclover.util.Maps.newHashMap;
import static org.openclover.util.Sets.newHashSet;

/**
 * A snapshot of the known state of testing activity relating to a Clover-enabled project
 * and its database.
 **/
public class Snapshot implements Serializable {
    private static final long serialVersionUID = 6684083217918243192L;

    public static final long UNKNOWN_DURATION = Long.MIN_VALUE;

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
        durationsForTests = new Object2LongOpenHashMap() {
            private static final long serialVersionUID = 6851581250481388361L;

            @Override
            public long defaultReturnValue() {
                return UNKNOWN_DURATION;
            }
        };
        failingTests = newHashSet();
        location = locationTosnapshot;
        updateFor(db);
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
                "No test results found in the Clover database. Please ensure the source files containing test "
                 + "classes have been instrumented by Clover and the tests have been run.");
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
        Map<String, SourceState> perTestMap = perTestSourceStates.get(test);
        if (perTestMap == null) {
            perTestMap = newHashMap();
            perTestSourceStates.put(test, perTestMap);
        }
        perTestMap.put(path, state);
    }

   @SuppressWarnings("unchecked")
   private void calcHits(final CloverDatabase db) {
        final long started = System.currentTimeMillis();

        db.getFullModel().visitFiles(new FileInfoVisitor() {
            @Override
            public void visitFileInfo(BaseFileInfo file) {
                final String packagePath = file.getPackagePath();
                final SourceState sourceState = new SourceState(file.getChecksum(), file.getFilesize());
                final Set<TestMethodCall> testsForFile = testsFor(db.getFullModel(), db.getTestHits((CoverageDataRange) file));
                for (TestMethodCall test : testsForFile) {
                    addToStates(test, packagePath, sourceState);
                }
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

    private Set<TestMethodCall> testsFor(FullProjectInfo project, Collection<TestCaseInfo> tcis) {
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

    public void store() throws IOException {
        if (!location.exists()) {
            if (location.getParentFile() != null && !location.getParentFile().exists()) {
                location.getParentFile().mkdirs();
            }
            location.createNewFile();
        }
        ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(location.toPath()));
        oos.writeObject(this);
        oos.close();
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
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
                    long start = System.currentTimeMillis();
                    Snapshot snapshot = (Snapshot) ois.readObject();
                    Logger.getInstance().verbose("Took " + (System.currentTimeMillis() - start) + "ms to load the snapshot file");
                    snapshot.location = file;
                    return snapshot;
                }
            } catch (InvalidClassException e) {
                Logger.getInstance().debug("Failed to load snapshot file at " + file.getAbsolutePath(), e);
                Logger.getInstance().warn("Failed to load snapshot file at " + file.getAbsolutePath() + " because it is no longer valid for this version of Clover");
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
            && ((FullFileInfo)file).changedFrom(fileReference.checksum, fileReference.filesize);
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
                            + " now: " + new SourceState(fileInfo.getChecksum(), fileInfo.getFilesize()) + ")");
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
                Collection<TestMethodCall> tests = result.get(filePath);
                if (tests == null) {
                    tests = newHashSet();
                    result.put(filePath, tests);
                }
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

    /** Records the interesting bits of a FullFileInfo for later comparison. Only used in this source file. */
    private static final class SourceState implements Serializable {
        private static final long serialVersionUID = -3186007190113270192L;

        private final long checksum;
        private final long filesize;

        SourceState(long checksum, long filesize) {
            this.checksum = checksum;
            this.filesize = filesize;
        }

        @Override
        public String toString() {
            return "SourceState{" +
                "checksum=" + checksum +
                ", filesize=" + filesize +
                '}';
        }
    }
}