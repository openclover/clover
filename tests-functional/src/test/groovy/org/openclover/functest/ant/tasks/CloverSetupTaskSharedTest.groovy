package org.openclover.functest.ant.tasks

import groovy.transform.CompileStatic
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.openclover.buildutil.testutils.AssertionUtils.assertFileContains
import static org.openclover.buildutil.testutils.AssertionUtils.assertStringContains

/**
 * Test for:
 * {@link org_openclover_runtime.Clover#getRecorder(String, long, long, int, org_openclover_runtime.CloverProfile[], String[])}
 * using the &lt;clover-instr/&gt; with the &lt;profile&gt; using the
 * {@link org.openclover.runtime.recorder.SharedCoverageRecorder}
 * with and without {@link org.openclover.runtime.remote.DistributedConfig}
 */
@CompileStatic
class CloverSetupTaskSharedTest extends CloverSetupTaskTestBase {

    // see setUp task in clover-setup-growable.xml
    protected static final Map<String, int[]> EXPECTED_COVERAGE = new HashMap<String, int[]>() {{
        put("Foo.java", [
                0, 0, 0, 1, 1, // 0-4
                0, 0, 0, 0, 1, // 5-9
                1, 0, 1, 1, 1, // 10-14
                3, 3, 1, 2, 1, // 15-19
                0, 3           // 20-21
        ] as int[])
        put("FooTest.java", [
                0, 0, 0, 0, 0, // 0-4
                0, 0, 1, 1, 0, // 5-9
                0, 0, 0, 1, 1, // 10-14
                0, 1, 1, 1, 0, // 15-19
                1, 1           // 20
        ] as int[])
    }}

    private Map<String, int[]> EXPECTED_COVERAGE_DISTRIBUTED = new HashMap<String, int[]>() {{
        put("Foo.java", [
                0, 0, 0, 0, 0, // 0-4
                0, 0, 0, 0, 0, // 5-9
                0, 0, 0, 0, 0, // 10-14
                0, 0, 0, 0, 0, // 15-19
                0, 0, 0, 0, 1, // 20-24
                1, 0, 1, 1, 0, // 25-29
                1, 1, 1, 3, 3, // 30-34
                1, 2, 1, 0, 3  // 35-39
        ] as int[])
        put("FooTest.java", [
                0, 0, 0, 0, 0, // 0-4
                0, 0, 2, 2, 0, // 5-9
                0, 0, 0, 2, 2, // 10-14
                0, 2, 2, 2, 0, // 15-19
                2, 2           // 20
        ] as int[])
    }}

    private Map<String, int[]> EXPECTED_COVERAGE_TWO_DBS = new HashMap<String, int[]>() {{
        put("Foo.java", [
                0, 0, 0, 1, 1, // 0-4
                0, 0, 0, 0, 1, // 5-9
                1, 0, 1, 1, 1, // 10-14
                3, 3, 1, 2, 1, // 15-19
                0, 3           // 20-21
        ] as int[])
        put("Hoo.java", [
                0, 0, 0, 1, 1, // 0-4
        ] as int[])
        put("FooTest.java", [
                0, 0, 0, 0, 0, // 0-4
                0, 0, 1, 1, 0, // 5-9
                0, 0, 0, 1, 1, // 10-14
                0, 1, 1, 1, 1, // 15-19
                0, 1, 1        // 20-21
        ] as int[])
    }}

    CloverSetupTaskSharedTest() {
        super("clover-setup-shared.xml")
    }

    /**
     * Run 'execute-growable' target and check if coverage report is correct.
     * Test assumptions:
     *  - distributed coverage feature is disabled, Foo class runs in another JVM
     *    - check if Foo.java DO NOT have per-test coverage data in the report
     *    - check if FooTest.java have per-test coverage
     *  - coverage hit counts from both JVMs are taken into account
     * @throws Exception
     */
    @Test
    void testExecuteShared() throws Exception {
        final String targetName = "execute-shared"

        // run target and check if clover-setup passed successfully
        testBase.setUp()
        testBase.executeTarget(targetName)

        // Growable recorder without distributed coverage
        testBase.assertLogContains("SharedCoverageRecorder[growableRecorder=GrowableCoverageRecorder[coverage=CoverageMatrix")
        testBase.assertLogContains("Distributed coverage is disabled")

        // two test cases
        testBase.assertLogContains("globalSliceStart(FooTest, 20,") // testFoo
        testBase.assertLogContains("globalSliceEnd(FooTest, 20, 1, null)")
        testBase.assertLogContains("globalSliceStart(FooTest, 24,") // testGoo
        testBase.assertLogContains("globalSliceEnd(FooTest, 24, 1, null)")

        // code was executed in another jvm
        assertStringContains("Say foo once", getJavaOut(0), false)
        assertStringContains("Say goo once", getJavaOut(0), false)

        // we should have no errors
        assertEquals("There shouldn't be errors", "", getJavaErr(0))

        // one shared per-test recorder instance
        // * only one JVM (not with a distributed coverage, so JVM running 'Foo' does not get globalSliceStart/End call)
        // * one test run
        // * three tests = 3 per-test recording slices
        assertEquals(3, getPerTestCoverageFiles().length)

        // check xml report content - global counters - should have coverage from both JVMs
        assertXmlFileContains(EXPECTED_COVERAGE, getCloverXmlFile())

        // check json report content - per-test coverage; note:
        // because of fact that unit tests spawns another JVM and application logic runs there, but the
        // distributed coverage is disabled, the Foo.js contains no information about per-test coverage
        assertFileContains("clover.testTargets = {}",
                getCloverFooJsonFile(),
                false)

        // ... but the FooTest.js has this information (i.e. the self-coverage of a test method)
        assertFileContains("{\"methods\":[{\"sl\":7}],\"name\":\"testFoo\",\"pass\":true,\"statements\":[{\"sl\":8}]}",
                getCloverFooTestJsonFile(), false)
        assertFileContains("{\"methods\":[{\"sl\":13}],\"name\":\"testGoo\",\"pass\":true,\"statements\":[{\"sl\":14}]}",
                getCloverFooTestJsonFile(), false)
    }

    /**
     * Run 'execute-growable-distributed' target and check if coverage report is correct.
     * Test assumptions:
     *  - distributed coverage feature is enabled, Foo class runs in another JVM
     *    - check if Foo.java have per-test coverage data in the report
     *    - check if FooTest.java have per-test coverage
     *  - coverage hit counts from both JVMs are taken into account
     * @throws Exception
     */
    @Test
    void testExecuteSharedDistributed() throws Exception {
        final String targetName = "execute-shared-distributed"

        // run target and check if clover-setup passed successfully
        testBase.setUp()
        testBase.executeTarget(targetName)

        // Check if growable recorder is used from a profile
        testBase.assertLogContains("SharedCoverageRecorder[growableRecorder=GrowableCoverageRecorder[coverage=CoverageMatrix")

        // Check if distributed coverage server has started in the 'junit' process
        testBase.assertLogContains("Distributed coverage is enabled with: name=clover.tcp.server;host=127.0.0.1")
        testBase.assertLogContains("Started coverage service: clover.tcp.server")
        testBase.assertLogContains("Recording proceeding now that 1 client are connected")

        // Check if distributed coverage client has started in the 'java' process
        assertStringContains("Starting distributed coverage client: name=clover.tcp.server;host=127.0.0.1;port=1199;", getJavaOut(1), false)
        assertStringContains("Attempting connection to: //127.0.0.1:1199/clover.tcp.server", getJavaOut(1), false)
        assertStringContains("Received remote item: Remote_Stub", getJavaOut(1), false)
        assertStringContains("Say foo once", getJavaOut(1), false)
        assertStringContains("Say goo once", getJavaOut(1), false)

        // Check if we have all recording files (1 global +2 per-test from both JVMs)
        testBase.assertFullLogContains("Processed 2 recording files")
        testBase.assertFullLogContains("Processed 5 per-test recording files")

        // two test cases
        testBase.assertLogContains("globalSliceStart(FooTest, 20,") // testFoo
        testBase.assertLogContains("globalSliceEnd(FooTest, 20, 1, null)")
        testBase.assertLogContains("globalSliceStart(FooTest, 24,") // testGoo
        testBase.assertLogContains("globalSliceEnd(FooTest, 24, 1, null)")
        testBase.assertLogContains("globalSliceStart(FooTest, 26") //testNothing
        testBase.assertLogContains("globalSliceEnd(FooTest, 26, 1, null)")

        // one shared per-test recorder instance
        // * two JVMs (distributed coverage is enabled, both JVMs have globalSliceStart/End)
        // * one test run
        // * three tests
        // - 1 test with no coverage (testNothing on 2nd JVM)
        // = 5 per-test recording slices
        assertEquals(5, getPerTestCoverageFiles().length)

        // check html/json report content - per-test coverage
        // we have distributed coverage enabled so per-test should be available in spawned JVM too
        assertFileContains(
                "{\"methods\":[{\"sl\":3}],\"name\":\"testFoo\",\"pass\":true,\"statements\":[{\"sl\":4},{\"sl\":15},{\"sl\":16},{\"sl\":17},{\"sl\":21}]}",
                getCloverFooJsonFile(),
                false)
        assertFileContains(
                "{\"methods\":[{\"sl\":9}],\"name\":\"testGoo\",\"pass\":true,\"statements\":[{\"sl\":10},{\"sl\":15},{\"sl\":16},{\"sl\":18},{\"sl\":19},{\"sl\":21}]}",
                getCloverFooJsonFile(),
                false)

        // but the FooTest.js has this information (i.e. the self-coverage of a test method)
        assertFileContains("{\"methods\":[{\"sl\":7}],\"name\":\"testFoo\",\"pass\":true,\"statements\":[{\"sl\":8}]}",
                getCloverFooTestJsonFile(), false)
        assertFileContains("{\"methods\":[{\"sl\":13}],\"name\":\"testGoo\",\"pass\":true,\"statements\":[{\"sl\":14}]}",
                getCloverFooTestJsonFile(), false)

        // check xml report content - global counters
        assertXmlFileContains(EXPECTED_COVERAGE, getCloverXmlFile())
    }

    /**
     * Run 'execute-shared-multiple-sessions' target and check if coverage report is correct.
     * Test assumptions:
     *  - we call &lt;clover-instr/&gt; for each file separately so that new instrumentation sessions will be written
     *    - check if still one shared recorder will be used
     *  - we overwrite Foo.java file and instrument, compile and run it again
     *    - check if outdated coverage recording file will be discarded and hit counts will be from second run only
     *  - we don't modify FooTest.java
     *    - check if coverage recording files from both test runs are taken so that hit counts are doubled
     *
     * @throws Exception
     */
    @Test
    void testExecuteSharedMultipleSessions() throws Exception {
        final String targetName = "execute-shared-multiple-sessions"

        // run target and check if clover-setup passed successfully
        testBase.setUp()
        testBase.executeTarget(targetName)

        // Growable recorder without distributed coverage
        testBase.assertLogContains("SharedCoverageRecorder[growableRecorder=GrowableCoverageRecorder[coverage=CoverageMatrix")
        testBase.assertLogContains("Distributed coverage is disabled")

        // two test cases from first and second test run (class was not recompiled so method IDs are the same)
        testBase.assertLogContains("globalSliceStart(FooTest, 20,") // testFoo
        testBase.assertLogContains("globalSliceEnd(FooTest, 20, 1, null)")
        testBase.assertLogContains("globalSliceStart(FooTest, 24,") // testGoo
        testBase.assertLogContains("globalSliceEnd(FooTest, 24, 1, null)")

        // code was executed in another jvm - two test runs
        assertStringContains("Say foo once", getJavaOut(10), false)
        assertStringContains("Say goo once", getJavaOut(10), false)
        assertStringContains("Say foo again", getJavaOut(11), false)
        assertStringContains("Say goo again", getJavaOut(11), false)

        // we should have no errors
        assertTrue(getJavaErr(10).length() == 0)
        assertTrue(getJavaErr(11).length() == 0)

        // one shared per-test recorder instance (despite having 3 instrumentation sessions)
        // * one JVM (distributed coverage is disabled, Foo's JVM does not get globalSliceStart/End call)
        // * two test runs
        // * three tests = 6 per-test recording slices
        assertEquals(6, getPerTestCoverageFiles().length)

        // check xml report content - global counters - should have coverage from both JVMs and
        //  - both test runs for FooTest
        //  - second test run for Foo
        assertXmlFileContains(EXPECTED_COVERAGE_DISTRIBUTED, getCloverXmlFile())

        // check json report content - per-test coverage; note:
        // because of fact that unit tests spawns another JVM and application logic runs there, but the
        // distributed coverage is disabled, the Foo.js contains no information about per-test coverage
        assertFileContains("clover.testTargets = {}",
                getCloverFooJsonFile(),
                false)

        // ... but the FooTest.js has this information (i.e. the self-coverage of a test method)
        assertFileContains("{\"methods\":[{\"sl\":7}],\"name\":\"testFoo\",\"pass\":true,\"statements\":[{\"sl\":8}]}",
                getCloverFooTestJsonFile(), false)
        assertFileContains("{\"methods\":[{\"sl\":13}],\"name\":\"testGoo\",\"pass\":true,\"statements\":[{\"sl\":14}]}",
                getCloverFooTestJsonFile(), false)

    }

    /**
     * Run 'execute-shared-multiple-databases' target and check if coverage report is correct.
     *
     * Test assumptions:
     *  - we call &lt;clover-instr/&gt; for each file separately using common or different initstring
     *    - check if still one shared recorder per database will be used
     *    - check if coverage recording files from all databases are taken
     *
     * @throws Exception
     */
    @Test
    void testExecuteSharedMultipleDatabases() throws Exception {
        final String targetName = "execute-shared-multiple-databases"

        // run target and check if clover-setup passed successfully
        testBase.setUp()
        testBase.executeTarget(targetName)

        // shared recorder without distributed coverage
        testBase.assertLogContains("SharedCoverageRecorder[growableRecorder=GrowableCoverageRecorder[coverage=CoverageMatrix")
        testBase.assertLogContains("Distributed coverage is disabled")

        // code was executed in another jvm - two test runs
        assertStringContains("Say foo once", getJavaOut(20), false)
        assertStringContains("Say goo once", getJavaOut(20), false)
        testBase.assertLogContains("Ho ho ho") // from classSetUp

        // we should have no errors
        assertTrue(getJavaErr(20).length() == 0)

        // three active recorders - 1st for Hoo (clover2.db), 2nd for FooTest (clover.db), 3rd for Foo (clover.db in second JVM)
        testBase.assertFullLogContains("Clover.getRecorder(" + getCloverDbFile())  // FooTest is using clover.db
        testBase.assertFullLogContains("Clover.getRecorder(" + getCloverDb2File()) // Hoo is using clover2.db
        assertStringContains("Clover.getRecorder(" + getCloverDbFile(), getJavaOut(20), false) // Foo is using clover.db

        // two shared per-test recorder instances (as we have two databases) in
        // * one JVM (distributed coverage is disabled, JVM running Foo does not get globalSliceStart/End call)
        // * three tests
        // = 6 per-test recording slices
        // - 3 slices not written due to optimization (no coverage)
        // = 3 per-test slices on disk
        // SharedRecorder flush? | clover.db | clover2.db
        // testFoo               | yes       | no
        // testGoo               | yes       | no
        // testNothing           | yes       | no
        assertEquals(3, getPerTestCoverageFiles().length)
        testBase.assertFullLogContains("Processed 3 per-test recording files") // 3 slices from clover.db

        // check xml report content - global counters - should have coverage from both JVMs and
        //  - both test runs for FooTest
        //  - second test run for Foo
        assertXmlFileContains(EXPECTED_COVERAGE_TWO_DBS, getCloverXmlFile())

        // check json report content - per-test coverage; note:
        // because of fact that unit tests spawns another JVM and application logic runs there, but the
        // distributed coverage is disabled, the Foo.js contains no information about per-test coverage
        assertFileContains("clover.testTargets = {}",
                getCloverFooJsonFile(),
                false)

        // ... but the FooTest.js has this information (i.e. the self-coverage of a test method)
        assertFileContains("{\"methods\":[{\"sl\":7}],\"name\":\"testFoo\",\"pass\":true,\"statements\":[{\"sl\":8}]}",
                getCloverFooTestJsonFile(), false)
        assertFileContains("{\"methods\":[{\"sl\":13}],\"name\":\"testGoo\",\"pass\":true,\"statements\":[{\"sl\":14}]}",
                getCloverFooTestJsonFile(), false)

    }


}