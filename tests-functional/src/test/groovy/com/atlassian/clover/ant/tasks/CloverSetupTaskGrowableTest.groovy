package com.atlassian.clover.ant.tasks

import groovy.transform.CompileStatic
import org.junit.Test

import static org.openclover.buildutil.testutils.AssertionUtils.assertFileContains
import static org.openclover.buildutil.testutils.AssertionUtils.assertStringContains
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Test for:
 * {@link org_openclover_runtime.Clover#getRecorder(String, long, long, int, org_openclover_runtime.CloverProfile[], String[])}
 * using the &lt;clover-setup/&gt; with the &lt;profile&gt; using the
 * {@link com.atlassian.clover.recorder.GrowableCoverageRecorder}
 * with and without {@link com.atlassian.clover.remote.DistributedConfig}
 */
@CompileStatic
class CloverSetupTaskGrowableTest extends CloverSetupTaskTestBase {

    // see setUp task in clover-setup-growable.xml
    protected static final Map<String, int[]> EXPECTED_COVERAGE = new HashMap<String, int[]>() {{
        put("Foo.java", [
                0, 0, 0, 1, 1, // 0-4
                0, 0, 0, 0, 1, // 5-9
                1, 0, 1, 1, 1, // 10-14
                1, 3, 3, 1, 2, // 15-19
                1, 0, 3        // 20-22
        ] as int[])
        put("Hoo.java", [
                0, 0, 0, 2, 2, // 0-4
        ] as int[])
        put("FooTest.java", [
                0, 0, 0, 0, 0, // 0-4
                0, 0, 1, 1, 0, // 5-9
                0, 0, 0, 1, 1, // 10-14
                0, 1, 1, 1, 0, // 15-19
                1, 1           // 20-21
        ] as int[])
    }}

    CloverSetupTaskGrowableTest() {
        super("clover-setup-growable.xml")
    }

    /**
     * Run 'execute-growable' target and check if coverage report is correct.
     * @throws Exception
     */
    @Test
    void testExecuteGrowable() throws Exception {
        final String targetName = "execute-growable"

        // run target and check if clover-setup passed successfully
        testBase.setUp()
        testBase.executeTarget(targetName)

        // Growable recorder without distributed coverage
        testBase.assertLogContains("GrowableCoverageRecorder[coverage=CoverageMatrix")
        testBase.assertLogContains("Distributed coverage is disabled")
        // two test cases
        testBase.assertLogContains("globalSliceStart(FooTest, 23,") // testFoo
        testBase.assertLogContains("globalSliceEnd(FooTest, 23, 1, null)")
        testBase.assertLogContains("globalSliceStart(FooTest, 27,") // testGoo
        testBase.assertLogContains("globalSliceEnd(FooTest, 27, 1, null)")
        testBase.assertLogContains("Ho ho ho") // from classSetUp()

        // code was executed in another jvm
        assertStringContains("Say foo", getJavaOut(0), false)
        assertStringContains("Say goo", getJavaOut(0), false)
        assertStringContains("Ho ho ho", getJavaOut(0), false)

        // we should have no errors
        assertTrue(getJavaErr(0) == null || getJavaErr(0).trim().isEmpty())

        // three instrumentation sessions = 3 recorders; 2 active recorders running in each JVM
        // * one JVM (distributed coverage is disabled so JVM running Foo does not get globalSliceStart/End call)
        // * two test cases
        // = 4 per-test coverage slices
        // - 2 slice not flushed due to no coverage (from Hoo class)
        // = 2 per-test slices on disk
        testBase.assertLogContains("flushed per-test recording (null)") // twice actually
        assertEquals(2, getPerTestCoverageFiles().length)
        testBase.assertFullLogContains("Processed 2 per-test recording files")

        // two active recorders * 2 JVMs = 4 global recording files
        testBase.assertFullLogContains("Processed 4 recording files")

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
     * @throws Exception
     */
    @Test
    void testExecuteGrowableDistributed() throws Exception {
        final String targetName = "execute-growable-distributed"

        // run target and check if clover-setup passed successfully
        testBase.setUp()
        testBase.executeTarget(targetName)

        // Check if growable recorder is used from a profile
        testBase.assertLogContains("GrowableCoverageRecorder[coverage=CoverageMatrix")

        // Check if distributed coverage server has started in the 'junit' process
        testBase.assertLogContains("Distributed coverage is enabled with: name=execute-growable-distributed;host=127.0.0.1")
        testBase.assertLogContains("Started coverage service: execute-growable-distributed")
        testBase.assertLogContains("Recording proceeding now that 1 client are connected")

        // Check if distributed coverage client has started in the 'java' process
        assertStringContains("Starting distributed coverage client: name=execute-growable-distributed;host=127.0.0.1", getJavaOut(1), false)
        assertStringContains("Attempting connection to: //127.0.0.1:1198/execute-growable-distributed", getJavaOut(1), false)
        assertStringContains("Received remote item: Remote_Stub", getJavaOut(1), false)
        assertStringContains("Say foo", getJavaOut(1), false)
        assertStringContains("Say goo", getJavaOut(1), false)
        assertStringContains("Ho ho ho", getJavaOut(1), false)

        // two test cases
        testBase.assertLogContains("globalSliceStart(FooTest, 23,") // testFoo
        testBase.assertLogContains("globalSliceEnd(FooTest, 23, 1, null)")
        testBase.assertLogContains("globalSliceStart(FooTest, 27,") // testGoo
        testBase.assertLogContains("globalSliceEnd(FooTest, 27, 1, null)")

        // two active recorders (Foo+Hoo, FooTest+Hoo)
        // * 2 JVMs (distributed coverage is enabled so both JVMs have globalSliceStart/End calls)
        // * two test cases
        // = 8 per-test coverage slices
        // - 4 slices not flushed due to no coverage (Hoo)
        // = 4 slices on on disk
        testBase.assertLogContains("flushed per-test recording (null)") // four times actually
        assertEquals(4, getPerTestCoverageFiles().length)
        testBase.assertFullLogContains("Processed 4 per-test recording files")

        // two active recorders * 2 JVMs = 4 global recording files
        testBase.assertFullLogContains("Processed 4 recording files")

        // check xml report content - global counters
        assertXmlFileContains(EXPECTED_COVERAGE, getCloverXmlFile())

        // check html/json report content - per-test coverage
        // we have distributed coverage enabled so per-test should be available in spawned JVM too
        assertFileContains(
                "{\"methods\":[{\"sl\":3}],\"name\":\"testFoo\",\"pass\":true,\"statements\":[{\"sl\":4},{\"sl\":16},{\"sl\":17},{\"sl\":18},{\"sl\":22}]}",
                getCloverFooJsonFile(),
                false)
        assertFileContains(
                "{\"methods\":[{\"sl\":9}],\"name\":\"testGoo\",\"pass\":true,\"statements\":[{\"sl\":10},{\"sl\":16},{\"sl\":17},{\"sl\":19},{\"sl\":20},{\"sl\":22}]}",
                getCloverFooJsonFile(),
                false)

        // but the FooTest.js has this information (i.e. the self-coverage of a test method)
        assertFileContains("{\"methods\":[{\"sl\":7}],\"name\":\"testFoo\",\"pass\":true,\"statements\":[{\"sl\":8}]}",
                getCloverFooTestJsonFile(), false)
        assertFileContains("{\"methods\":[{\"sl\":13}],\"name\":\"testGoo\",\"pass\":true,\"statements\":[{\"sl\":14}]}",
                getCloverFooTestJsonFile(), false)
    }

}