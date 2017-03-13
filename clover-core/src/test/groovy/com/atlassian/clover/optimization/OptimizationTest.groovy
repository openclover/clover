package com.atlassian.clover.optimization

import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.TestUtils
import com.atlassian.clover.api.optimization.Optimizable
import com.atlassian.clover.api.optimization.OptimizationOptions
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.instr.InstrumentationSessionImpl
import com.atlassian.clover.util.CloverUtils

class OptimizationTest extends TestOptimizationBase {
    void testCantOptimizeIfNoSnapshotOrRegistry() throws Exception {
        File tempDir = TestUtils.createEmptyDirFor(getClass(), getName())

        assertTrue(
            !new LocalSnapshotOptimizer(null, null, new OptimizationOptions.Builder().build()).canOptimize())

        //Null snapshot location and init string
        assertTrue(
            !new LocalSnapshotOptimizer(new OptimizationOptions.Builder().snapshot(null).initString(null).build()).canOptimize())

        //Null snapshot, valid init string
        assertTrue(
            !new LocalSnapshotOptimizer(new OptimizationOptions.Builder().snapshot(null).initString(registry.getInitstring()).build()).canOptimize())

        File snapshotFile = File.createTempFile("foo", "bar", tempDir)
        //Snapshot that doesn't exist
        assertTrue(
            !new LocalSnapshotOptimizer(new OptimizationOptions.Builder().snapshot(snapshotFile).initString(registry.getInitstring()).build()).canOptimize())
        snapshotFile.createNewFile()
        //Snapshot that does exist but is empty
        assertTrue(
            !new LocalSnapshotOptimizer(new OptimizationOptions.Builder().snapshot(snapshotFile).initString(registry.getInitstring()).build()).canOptimize())

        Snapshot snapshot = Snapshot.generateFor(registry.getInitstring(), snapshotFile.getAbsolutePath(), new CoverageDataSpec())
        snapshot.store()

        //Null database
        assertTrue(
            !new LocalSnapshotOptimizer(new OptimizationOptions.Builder().snapshot(snapshotFile).initString(null).build()).canOptimize())

        File databaseFile = File.createTempFile("foo", "bar2", tempDir)
        //Empty database
        assertTrue(
            !new LocalSnapshotOptimizer(new OptimizationOptions.Builder().snapshot(snapshotFile).initString(databaseFile.getAbsolutePath()).build()).canOptimize())
        databaseFile.createNewFile()
        //Empty database
        assertTrue(
            !new LocalSnapshotOptimizer(new OptimizationOptions.Builder().snapshot(snapshotFile).initString(databaseFile.getAbsolutePath()).build()).canOptimize())
    }

    void testOptimizableNameResolution() throws Exception {
        runNoAppClassTest_testMain()
        runAppClass2Test_testMain()
        runAppClass23Test_testMain()
        runAppClass234Test_testMain()

        Snapshot.generateFor(
            CloverDatabase.loadWithCoverage(
                registry.getInitstring(),
                new CoverageDataSpec())).store()

        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        addClassWithSingleMethod(session, new ContextSet(), THIS_PACKAGE, "AppClass4", "testMain", true)
        session.finishAndApply()
        registry.saveAndOverwriteFile()

        Optimizer optimizer = new LocalSnapshotOptimizer(
            new OptimizationOptions.Builder()
                .initString(registry.getInitstring())
                .snapshot(Snapshot.fileForInitString(registry.getInitstring())).build())

        assertTrue(optimizer.canOptimize())

        Optimizable[] optimizables = [
            new MockOptimizable(THIS_PACKAGE + ".AppClass234Test"),
            new MockOptimizable(THIS_PACKAGE + ".AppClass234Test" + TEST_MAIN_METHOD_SUFFIX),
            new MockOptimizable(THIS_PACKAGE.replace('.', '/') + "/AppClass234Test.java"),
            new MockOptimizable("/arbitrary/prefix/" + THIS_PACKAGE.replace('.', '/') + "/AppClass234Test.java"),
            new MockOptimizable("c:/arbitrary/prefix/" + THIS_PACKAGE.replace('.', '/') + "/AppClass234Test.java"),
        ]

        for (Optimizable optimizable : optimizables) {
            assertTrue("Optimizable with name " + optimizable.getName() + " was not included by the Optimizer", optimizer.include(optimizable, new OptimizationSession(new OptimizationOptions.Builder().build())))
        }

        optimizables = [
            new MockOptimizable(THIS_PACKAGE + ".AppClass23Test"),
            new MockOptimizable(THIS_PACKAGE + ".AppClass23Test" + TEST_MAIN_METHOD_SUFFIX),
            new MockOptimizable(THIS_PACKAGE.replace('.', '/') + "/AppClass23Test.java"),
            new MockOptimizable("/arbitrary/prefix/" + THIS_PACKAGE.replace('.', '/') + "/AppClass23Test.java"),
            new MockOptimizable("c:/arbitrary/prefix/" + THIS_PACKAGE.replace('.', '/') + "/AppClass23Test.java"),
        ]

        for (Optimizable optimizable : optimizables) {
            assertTrue("Optimizable with name " + optimizable.getName() + " should not be included by the Optimizer", !optimizer.include(optimizable, new OptimizationSession(new OptimizationOptions.Builder().build())))
        }
    }

    void testAddedTestFlaggedAsRequiringRun() throws Exception {
        runNoAppClassTest_testMain()
        runAppClass2Test_testMain()
        runAppClass23Test_testMain()
        runAppClass234Test_testMain()

        Snapshot.generateFor(
            CloverDatabase.loadWithCoverage(
                registry.getInitstring(),
                new CoverageDataSpec())).store()

        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()
        addClassWithSingleMethod(session, new ContextSet(), THIS_PACKAGE, "AddedTest", "testMain", true)
        session.finishAndApply()
        registry.saveAndOverwriteFile()

        Optimizer optimizer =
            new LocalSnapshotOptimizer(
                new OptimizationOptions.Builder()
                    .initString(registry.getInitstring())
                    .snapshot(Snapshot.fileForInitString(registry.getInitstring())).build())

        assertTrue(optimizer.canOptimize())

        List optimized = optimizer.optimize([ new MockOptimizable(THIS_PACKAGE + ".AddedTest") ])

        assertEquals(1, optimized.size())
        assertEquals(THIS_PACKAGE + ".AddedTest", ((MockOptimizable)optimized.get(0)).getName())
    }

    void testOptimizingTestWithNoKnownTestCases() throws Exception {
        runNoAppClassTest_testMain()
        Snapshot snapshot = Snapshot.generateFor(CloverDatabase.loadWithCoverage(registry.getInitstring(), new CoverageDataSpec()))
        Optimizer optimizer = new LocalSnapshotOptimizer(snapshot, registry, new OptimizationOptions.Builder().build())

        assertTrue(optimizer.canOptimize())
        List<MockOptimizable> input = [ new MockOptimizable("some.new.test"), new MockOptimizable("some.new.test1") ]
        List<MockOptimizable> output = optimizer.optimize(input)

        assertEquals(input, output)

    }


    void testFailingTestsAreRemembered() throws Exception {
        runAppClass234Test_testMain()
        runFailingTest_testFail()

        CloverDatabase cloverDatabase = CloverDatabase.loadWithCoverage(registry.getInitstring(), new CoverageDataSpec())
        Snapshot snapshot = Snapshot.generateFor(cloverDatabase)
        Optimizer optimizer = new LocalSnapshotOptimizer(snapshot, registry, new OptimizationOptions.Builder().build())


        final Optimizable[] optimizables = [
            new MockOptimizable(THIS_PACKAGE + ".FailingTest"),
            new MockOptimizable(THIS_PACKAGE + ".AppClass234Test")
        ]

        Collection<Optimizable> optimized = optimizer.optimize(Arrays.asList(optimizables))
        assertTrue(optimized.contains(new MockOptimizable(THIS_PACKAGE + ".FailingTest")))
        assertEquals(1, optimized.size())

        snapshot.store()
        CloverUtils.scrubCoverageData(registry.getInitstring(), false);   // simulate missing info for failed test
        runAppClass23Test_testMain()

        cloverDatabase = CloverDatabase.loadWithCoverage(registry.getInitstring(), new CoverageDataSpec())
        snapshot.updateFor(cloverDatabase)

        Collection<Optimizable> optimized2 = optimizer.optimize(Arrays.asList(optimizables))
        assertTrue(optimized2.contains(new MockOptimizable(THIS_PACKAGE + ".FailingTest")))
        assertEquals(1, optimized2.size())


    }
    private class MockOptimizable implements Optimizable {
        private String name

        private MockOptimizable(String name) {
            this.name = name
        }

        String getName() {
            return name
        }

        @Override
        boolean equals(Object o) {
            if (this.is(o))
                return true
            if (!(o instanceof MockOptimizable))
                return false

            final MockOptimizable that = (MockOptimizable) o

            if (name != null ? !name.equals(that.name) : that.name != null)
                return false

            return true
        }

        @Override
        int hashCode() {
            return name != null ? name.hashCode() : 0
        }
    }
}
