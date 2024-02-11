package com.atlassian.clover.optimization

import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.TestUtils
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.instr.InstrumentationSessionImpl
import com.atlassian.clover.registry.entities.FullMethodInfo
import org_openclover_runtime.CloverVersionInfo
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

class SnapshotTest extends TestOptimizationBase {

    @Before
    void setUp() {
        baseSetUp()
    }

    @After
    void tearDown() {
        baseTearDown()
    }

    @Test
    void testLoadingAbsentSnapshotYieldsNull() {
        assertNull(Snapshot.loadFor(registry.getInitstring()))
    }

    @Test
    void testCanCreateValidSnapshotForDbWithNoCoverage() throws Exception {
        Snapshot snapshot = Snapshot.generateFor(
            CloverDatabase.loadWithCoverage(
                registry.getInitstring(),
                new CoverageDataSpec()))

        assertNotNull(snapshot)

        snapshot.store()

        assertTrue(snapshot.getLocation().exists())
        assertEquals(snapshot.getCloverVersionInfo(), CloverVersionInfo.formatVersionInfo())
        assertEquals(snapshot.getDbVersionCount(), 1)
        assertEquals(((Long)snapshot.getDbVersions().iterator().next()).longValue(), registry.getVersion())
    }

    @Test
    void testCanDeleteSnapshot() throws Exception {
        Snapshot snapshot = Snapshot.generateFor(
            CloverDatabase.loadWithCoverage(
                registry.getInitstring(),
                new CoverageDataSpec()))

        assertNotNull(snapshot)

        snapshot.store()

        assertTrue(snapshot.getLocation().exists())
        snapshot.delete()
        assertFalse(snapshot.getLocation().exists())
    }

    @Test
    void testCanCreateLoadAndDeleteSnapshotAtOtherLocation() throws Exception {
        File SnapshotLocation = new File(tmpDir, "foo.teststate")
        Snapshot snapshot = Snapshot.generateFor(
            CloverDatabase.loadWithCoverage(
                registry.getInitstring(),
                new CoverageDataSpec()),
            SnapshotLocation.getAbsolutePath())

        assertNotNull(snapshot)

        snapshot.store()

        assertTrue(snapshot.getLocation().exists())

        Snapshot Snapshot2 = Snapshot.loadFrom(SnapshotLocation.getAbsolutePath())
        assertNotNull(Snapshot2)

        Snapshot2.delete()
        assertFalse(snapshot.getLocation().exists())
    }

    @Test
    void testCanCreateSnapshotForSampleDb() throws Exception {
        runNoAppClassTest_testMain()
        runAppClass2Test_testMain()
        runAppClass23Test_testMain()
        runAppClass234Test_testMain()

        Snapshot snapshot = Snapshot.generateFor(
            CloverDatabase.loadWithCoverage(
                registry.getInitstring(),
                new CoverageDataSpec()))

        assertNotNull(
            snapshot.lookupTests(
                noAppClassTest_testMain.getContainingClass().getContainingFile().getPackagePath()))
        assertNotNull(
            snapshot.lookupTests(
                noAppClassTest_testMain.getContainingClass().getQualifiedName()))
        assertNotNull(
            snapshot.lookupTests(
                noAppClassTest_testMain.getContainingClass().getQualifiedName() + TEST_MAIN_METHOD_SUFFIX))

        assertNotNull(
            snapshot.lookupTests(
                appClass2Test_testMain.getContainingClass().getContainingFile().getPackagePath()))
        assertNotNull(
            snapshot.lookupTests(
                appClass2Test_testMain.getContainingClass().getQualifiedName()))
        assertNotNull(
            snapshot.lookupTests(
                appClass2Test_testMain.getContainingClass().getQualifiedName()) + TEST_MAIN_METHOD_SUFFIX)

        assertNotNull(
            snapshot.lookupTests(
                appClass23Test_testMain.getContainingClass().getContainingFile().getPackagePath()))
        assertNotNull(
            snapshot.lookupTests(
                appClass23Test_testMain.getContainingClass().getQualifiedName()))
        assertNotNull(
            snapshot.lookupTests(
                appClass23Test_testMain.getContainingClass().getQualifiedName()) + TEST_MAIN_METHOD_SUFFIX)

        assertNotNull(
            snapshot.lookupTests(
                appClass234Test_testMain.getContainingClass().getContainingFile().getPackagePath()))
        assertNotNull(
            snapshot.lookupTests(
                appClass234Test_testMain.getContainingClass().getQualifiedName()))
        assertNotNull(
            snapshot.lookupTests(
                appClass234Test_testMain.getContainingClass().getQualifiedName()) + TEST_MAIN_METHOD_SUFFIX)
    }

    @Test
    void testStoringWithoutUpdatingDoesNotUpdateDbVersion() throws Exception {
        Snapshot Snapshot1 =
            Snapshot.generateFor(
                CloverDatabase.loadWithCoverage(
                    registry.getInitstring(),
                    new CoverageDataSpec()))
        Snapshot1.store()

        Snapshot.loadFor(registry.getInitstring()).store()

        Snapshot Snapshot2 =
            Snapshot.loadFor(registry.getInitstring())
        assertEquals(Snapshot1.getDbVersions(), Snapshot2.getDbVersions())
    }

    @Test
    void testStoringWithUpdateAddsDbVersion() throws Exception {
        Snapshot.generateFor(
            CloverDatabase.loadWithCoverage(
                registry.getInitstring(),
                new CoverageDataSpec())).store()

        long startVersion = registry.getVersion()
        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        addClassWithSingleMethod(session, new ContextSet(), THIS_PACKAGE, "AppClass2", "main", false)
        session.finishAndApply()
        registry.saveAndOverwriteFile()

        long endVersion = registry.getVersion()

        Snapshot snapshot = Snapshot.loadFor(registry.getInitstring())
        CloverDatabase database =
            CloverDatabase.loadWithCoverage(
                registry.getInitstring(),
                new CoverageDataSpec())
        snapshot.updateFor(database)
        snapshot.store()

        Snapshot Snapshot2 =
            Snapshot.loadFor(registry.getInitstring())
        assertEquals(Snapshot2.getDbVersions().size(), 2)
        assertTrue(Snapshot2.getDbVersions().contains(new Long(startVersion)))
        assertTrue(Snapshot2.getDbVersions().contains(new Long(endVersion)))
    }

    @Test
    void testTestDurationCalculationIsAccurate() throws Exception {
        final long runStart = 0
        final long setupDuration = 100
        final long teardownDuration = 100

        final long noAppClassTest_testMainDuration = 50
        final long appClass2Test_testMainDuration = 75
        final long appClass23Test_testMainDuration = 100
        final long appClass234Test_testMainDuration = 125

        final long avgSetupTeardownDuration = (setupDuration + teardownDuration)

        long currentTimeMillis = runStart

        runNoAppClassTest_testMain(currentTimeMillis, currentTimeMillis += noAppClassTest_testMainDuration)
        currentTimeMillis += teardownDuration
        runAppClass2Test_testMain(currentTimeMillis += setupDuration, currentTimeMillis += appClass2Test_testMainDuration)
        currentTimeMillis += teardownDuration
        runAppClass23Test_testMain(currentTimeMillis += setupDuration, currentTimeMillis += appClass23Test_testMainDuration)
        currentTimeMillis += teardownDuration
        runAppClass234Test_testMain(currentTimeMillis += setupDuration, currentTimeMillis += appClass234Test_testMainDuration)

        final Snapshot snapshot =
            Snapshot.generateFor(
                CloverDatabase.loadWithCoverage(
                    registry.getInitstring(),
                    //Coverage back to 0
                    new CoverageDataSpec(registry.getVersion())))

        assertEquals(
            snapshot.calculateDurationOf(
                    (findAllTestsFor(snapshot, "NoAppClassTest") +
                    findAllTestsFor(snapshot, "AppClass2Test") +
                    findAllTestsFor(snapshot, "AppClass23Test") +
                    findAllTestsFor(snapshot, "AppClass234Test")) as Set
            ),
            (4 * avgSetupTeardownDuration)
                + noAppClassTest_testMainDuration
                + appClass2Test_testMainDuration
                + appClass23Test_testMainDuration
                + appClass234Test_testMainDuration)

        assertDuration(snapshot, sourceNameFor("NoAppClassTest"), noAppClassTest_testMainDuration, avgSetupTeardownDuration)
        assertDuration(snapshot, sourceNameFor("AppClass2Test"), appClass2Test_testMainDuration, avgSetupTeardownDuration)
        assertDuration(snapshot, sourceNameFor("AppClass23Test"), appClass23Test_testMainDuration, avgSetupTeardownDuration)
        assertDuration(snapshot, sourceNameFor("AppClass234Test"), appClass234Test_testMainDuration, avgSetupTeardownDuration)
    }

    private Collection<TestMethodCall> findAllTestsFor(Snapshot snapshot, String classSimpleName) {
        snapshot.lookupTests(sourceNameFor(classSimpleName))
    }

    private String sourceNameFor(String classSimpleName) {
        return THIS_PACKAGE.replace('.', '/') + "/" + classSimpleName + ".java"
    }

    @Test
    void testCanEstimateTestDurationWhenTestAdded() throws Exception {
        final long firstRunStart = 0
        final long setupDuration = 100
        final long teardownDuration = 100

        final long noAppClassTest_testMainDuration = 50
        final long appClass2Test_testMainDuration = 75
        final long appClass23Test_testMainDuration = 100
        final long appClass234Test_testMainDuration = 125

        final long avgSetupTeardownDuration = (setupDuration + teardownDuration)

        long currentTimeMillis = firstRunStart

        runNoAppClassTest_testMain(currentTimeMillis, currentTimeMillis += noAppClassTest_testMainDuration)
        currentTimeMillis += teardownDuration
        runAppClass2Test_testMain(currentTimeMillis += setupDuration, currentTimeMillis += appClass2Test_testMainDuration)
        currentTimeMillis += teardownDuration
        runAppClass23Test_testMain(currentTimeMillis += setupDuration, currentTimeMillis += appClass23Test_testMainDuration)
        currentTimeMillis += teardownDuration
        runAppClass234Test_testMain(currentTimeMillis += setupDuration, currentTimeMillis += appClass234Test_testMainDuration)

        Snapshot.generateFor(
            CloverDatabase.loadWithCoverage(
                registry.getInitstring(),
                new CoverageDataSpec(registry.getVersion()))).store()

        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        FullMethodInfo addedTest_testMain = addClassWithSingleMethod(session, new ContextSet(), THIS_PACKAGE, "AddedTest", "testMain", true)
        session.finishAndApply()
        registry.saveAndOverwriteFile()
        recorder = TestUtils.newRecorder(registry)

        final long addedTest_testMainDuration = 123
        final long secondRunStart = 5000
        TestUtils.runTestMethod(recorder, THIS_PACKAGE + ".AddedTest", testID, addedTest_testMain,
                new FullMethodInfo[0], secondRunStart, secondRunStart + addedTest_testMainDuration)

        final Snapshot snapshot = Snapshot.loadFor(registry.getInitstring())
        snapshot.updateFor(
            CloverDatabase.loadWithCoverage(
                registry.getInitstring(),
                new CoverageDataSpec(registry.getVersion())))

        assertEquals(
            snapshot.calculateDurationOf(
                    (findAllTestsFor(snapshot, "NoAppClassTest") +
                    findAllTestsFor(snapshot, "AppClass2Test") +
                    findAllTestsFor(snapshot, "AppClass23Test") +
                    findAllTestsFor(snapshot, "AppClass234Test")) as Set
            ),
            (4 * avgSetupTeardownDuration)
                + noAppClassTest_testMainDuration
                + appClass2Test_testMainDuration
                + appClass23Test_testMainDuration
                + appClass234Test_testMainDuration)

        assertDuration(snapshot, sourceNameFor("NoAppClassTest"), noAppClassTest_testMainDuration, avgSetupTeardownDuration)
        assertDuration(snapshot, sourceNameFor("AppClass2Test"), appClass2Test_testMainDuration, avgSetupTeardownDuration)
        assertDuration(snapshot, sourceNameFor("AppClass23Test"), appClass23Test_testMainDuration, avgSetupTeardownDuration)
        assertDuration(snapshot, sourceNameFor("AppClass234Test"), appClass234Test_testMainDuration, avgSetupTeardownDuration)
        assertDuration(snapshot, sourceNameFor("AddedTest"), addedTest_testMainDuration, avgSetupTeardownDuration)
    }

    private void assertDuration(Snapshot snapshot, String testFileName, long duration, long avgSetupTeardown) {
        Set<TestMethodCall> tests = snapshot.lookupTests(testFileName)
        assertEquals(
            "Duration for tests: ${tests} not as expected - individual (calculated) durations: ${durationsFor(tests, snapshot)} for: ${tests}",
            snapshot.calculateDurationOf(tests), duration + avgSetupTeardown)
    }

    private Set<Long> durationsFor(Set<TestMethodCall> tests, Snapshot snapshot) {
        Set durations = null
        if (tests != null) {
            durations = new LinkedHashSet()
            for (Iterator<TestMethodCall> iterator = tests.iterator(); iterator.hasNext();) {
                durations.add(new Long(snapshot.calculateDurationOf(Collections.singleton(iterator.next()))))
            }
        }
        return durations
    }
}
