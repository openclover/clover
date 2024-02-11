package com.atlassian.clover.ant.tasks

import com.atlassian.clover.instr.java.RecorderInstrEmitter
import org.openclover.runtime.CloverNames
import com_atlassian_clover.CloverProfile
import groovy.transform.CompileStatic
import org.apache.tools.ant.RuntimeConfigurable
import org.apache.tools.ant.Target
import org.apache.tools.ant.Task
import org.junit.Test

import static com.atlassian.clover.testutils.AssertionUtils.assertFileContains
import static com.atlassian.clover.testutils.AssertionUtils.assertFileMatches
import static com.atlassian.clover.testutils.AssertionUtils.assertStringContains
import static com.atlassian.clover.testutils.AssertionUtils.assertStringMatches
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * Test for:
 * {@link com_atlassian_clover.Clover#getRecorder(String, long, long, int, com_atlassian_clover.CloverProfile[], String[])}
 * as well as &lt;clover-setup/&gt; and &lt;clover-instr/&gt;.
 */
@CompileStatic
class CloverSetupTaskProfilesCorrectTest extends CloverSetupTaskTestBase {

    public static final String PROPERTY_NOT_FOUND_MSG =
            "CLOVER: System property '" + CloverNames.PROP_CLOVER_PROFILE + "' was not found. Assuming the 'default' profile."

    public static final String USING_PROFILE_MSG = "CLOVER: Using profile '%s' with settings [coverageRecorder=%s"

    public static final String NO_PROFILES_DEFINED_MSG = "CLOVER: No profiles defined in instrumented classes. Using standard settings."

    public static final String PROFILE_NOT_FOUND_MSG = "CLOVER: Profile '%s' not found in instrumented classes. Using standard settings."

    public static final String DISABLING_CLOVER_MSG = "CLOVER: The system property 'clover.enable' is set to false. Coverage recording is disabled."

    CloverSetupTaskProfilesCorrectTest() {
        super("clover-setup-profiles-correct.xml")
    }

    /**
     * Returns value of the 'foo.instr.file' property which holds a location of the instrumented Foo.java file
     * for a current test case. Use this method AFTER <code>testBase.executeTarget()</code> call.
     * See clover-setup-profiles-correct.xml targets.
     *
     * @return File
     */
    protected File getCloverFooInstrFile() {
        return new File(testBase.getProject().getProperty("foo.instr.file"))
    }

    /**
     * Run 'setup-with-profiles' target and check if &lt;profiles&gt; tag has been parsed by Ant
     * and data passed to the CloverSetupTask.
     *
     * @throws Exception
     */
    @Test
    void testSetupWithProfiles() throws Exception {
        final String targetName = "setup-with-profiles"
        // run target and check if clover-setup passed successfully
        testBase.setUp()
        testBase.expectLogContaining(targetName, "Clover is enabled with initstring")
        // ... and the clover compiler adapter was set
        assertTrue(CloverSetupTask.CLOVER_ADAPTER.equals(testBase.getProject().getProperty("build.compiler")))

        // check if clover-setup is on task list
        Target target = (Target) testBase.getProject().getTargets().get(targetName)
        assertNotNull(target)
        Task cloverSetupTask = target.getTasks()[0]
        assertNotNull(cloverSetupTask)
        assertEquals("clover-setup", cloverSetupTask.getTaskName())

        // check actual values of clover-setup/profiles/(profile)*
        RuntimeConfigurable profilesTag = (RuntimeConfigurable) cloverSetupTask.getRuntimeConfigurableWrapper()
                .getChildren().nextElement()
        Enumeration profilesEnum = profilesTag.getChildren()
        int profileCount = 0
        while (profilesEnum.hasMoreElements()) {
            profilesEnum.nextElement()
            profileCount++
        }
        assertEquals(3, profileCount)
    }

    /**
     * Run 'execute-no-profiles' target.
     * Test input:
     * - no profiles compiled into code,
     * - no clover.profile system property
     * Expected: when there are no profiles, Clover selects fixed coverage recorder.
     *
     * @throws Exception
     */
    @Test
    void testExecuteNoProfiles() throws Exception {
        final String targetName = "execute-no-profiles"
        testBase.setUp()
        testBase.executeTarget(targetName)

        // check instrumented file content
        final String expectedProfilesCode = "public static ${CloverProfile.name}[] profiles = { };"
        assertFileContains(expectedProfilesCode, getCloverFooInstrFile(), false)

        // check execution log
        assertStringContains(NO_PROFILES_DEFINED_MSG, getJavaOut(), false)
    }

    /**
     * Run 'execute-default-profile' target.
     * Test input:
     * - one "default" profile compiled into code,
     * - no clover.profile system property
     * <p/>
     * Expected: when profiles are compiled-in, but no system property defined,
     * Clover selects the "default" profile.
     *
     * @throws Exception
     */
    @Test
    void testExecuteDefaultProfile() throws Exception {
        final String targetName = "execute-default-profile"
        testBase.setUp()
        testBase.executeTarget(targetName)

        // check instrumented file content
        final String expectedProfilesCode =
                "public static ${CloverProfile.name}[] profiles = { " +
                        "new ${CloverProfile.name}" +
                        "(\"\\u0064\\u0065\\u0066\\u0061\\u0075\\u006c\\u0074\", \"GROWABLE\", null)};"
        assertFileContains(expectedProfilesCode, getCloverFooInstrFile(), false)

        // check execution log
        assertStringContains(PROPERTY_NOT_FOUND_MSG, getJavaOut(), false)
        assertStringContains(String.format(USING_PROFILE_MSG, "default", "GROWABLE"), getJavaOut(), false)
    }

    /**
     * Run 'execute-other-profile' target.
     * Test input:
     * - profiles "default", "other", "remote" compiled into code,
     * - clover.profile=other system property
     * Expected: Clover selects coverage recorder from "other" profile
     *
     * @throws Exception
     */
    @Test
    void testExecuteOtherProfile() throws Exception {
        final String targetName = "execute-other-profile"
        testBase.setUp()
        testBase.executeTarget(targetName)

        // check instrumented file content
        final String expectedProfilesCode =
                "public static ${CloverProfile.name}[] profiles = { " +
                        "new ${CloverProfile.name}(" +
                        RecorderInstrEmitter.asUnicodeString("default") + ", \"GROWABLE\", null)," +
                        "new ${CloverProfile.name}(" +
                        RecorderInstrEmitter.asUnicodeString("other") + ", \"SHARED\", null)," +
                        "new ${CloverProfile.name}(" +
                        RecorderInstrEmitter.asUnicodeString("remote") + ", \"FIXED\", " +
                        "\"\\u0068\\u006f\\u0073\\u0074" // ...
        assertFileContains(expectedProfilesCode, getCloverFooInstrFile(), false)
        // ... extra check for closing "};"
        final String expectedProfilesRegexp =
                "public static .*\\[\\] profiles = \\{ .*\\};"
        assertFileMatches(expectedProfilesRegexp, getCloverFooInstrFile(), false)

        // check execution log
        assertStringContains(String.format(USING_PROFILE_MSG, "other", "SHARED"), getJavaOut(), false)
        assertStringContains("SharedCoverageRecorder[growableRecorder=GrowableCoverageRecorder[coverage=CoverageMatrix", getJavaOut(), false)
    }

    /**
     * Run 'execute-not-found-profile' target.
     * Test input:
     * - two profiles - "default" and "one" compiled into code,
     * - clover.profile=two system property
     * Expected: "two" profile not found, warning, Clover selects standard coverage recorder.
     *
     * @throws Exception
     */
    @Test
    void testExecuteNotFoundProfile() throws Exception {
        final String targetName = "execute-not-found-profile"
        testBase.setUp()
        testBase.executeTarget(targetName)

        // check instrumented file content
        final String expectedProfilesCode =
                "public static ${CloverProfile.name}\\[\\] profiles = \\{ " +
                        "new ${CloverProfile.name}.*GROWABLE.*" +
                        "new ${CloverProfile.name}.*FIXED.*\\};"
        assertFileMatches(expectedProfilesCode, getCloverFooInstrFile(), false)

        // check execution log
        assertStringContains(String.format(PROFILE_NOT_FOUND_MSG, "two"), getJavaOut(), false)
        assertStringContains("FixedSizeCoverageRecorder", getJavaOut(), false)
    }

    /**
     * Run 'execute-not-found-null-profile' target.
     * Test is similar to 'execute-not-found-profile' but checks against null profiles array.
     * Test input:
     * - no profiles compiled into code,
     * - clover.profile=some system property
     * Expected: no profiles defined, selects standard coverage recorder
     *
     * @throws Exception
     */
    @Test
    void testExecuteNotFoundNullProfile() throws Exception {
        final String targetName = "execute-not-found-null-profile"
        testBase.setUp()
        testBase.executeTarget(targetName)

        // check instrumented file content
        final String expectedProfilesCode = "public static ${CloverProfile.name}[] profiles = { };"
        assertFileContains(expectedProfilesCode, getCloverFooInstrFile(), false)

        // check execution log
        assertStringContains(NO_PROFILES_DEFINED_MSG, getJavaOut(), false)
        assertStringContains("FixedSizeCoverageRecorder", getJavaOut(), false)
    }

    /**
     * Run 'execute-distributed-coverage-from-profile' target.
     * Test input:
     * - distributed coverage is written both as child node of &lt;clover-instr/&gt; and in the profile
     * - clover.profile=default system property
     * Expected:
     * - presence of the profile shall override settings from top-level node
     *
     * @throws Exception
     */
    @Test
    void testDistributedCoverageFromProfile() throws Exception {
        final String targetName = "execute-distributed-coverage-from-profile"
        testBase.setUp()
        testBase.executeTarget(targetName)

        // check instrumented file content
        final String expectedProfilesCode =
                "public static ${CloverProfile.name}\\[\\] profiles = \\{ " +
                        "new ${CloverProfile.name}" +
                        ".*\\\\u0064\\\\u0065\\\\u0066\\\\u0061\\\\u0075\\\\u006c\\\\u0074.*" + // "default" as unicode
                        "\\};"
        assertFileMatches(expectedProfilesCode, getCloverFooInstrFile(), false)

        // check execution log
        assertStringContains(
                String.format(USING_PROFILE_MSG, "default", "FIXED distributedCoverage=host=host.from.profile;timeout=10]"),
                getJavaOut(), false)
        assertStringMatches("Distributed coverage is enabled with: name=clover.tcp.server;host=host.from.profile",
                getJavaOut(), false)
    }

    /**
     * Run 'execute-distributed-coverage-from-top-level' target.
     * Test input:
     * - distributed coverage is written both as child node of &lt;clover-instr/&gt
     * - the 'default' profile has no distributed coverage defined
     * - clover.profile system property is not defined
     * Expected:
     * - as property is not present, 'default' profile is used, as it does not contain distributedCoverage tag,
     * settings from clover-instr/distributedCoverage are taken
     *
     * @throws Exception
     */
    @Test
    void testDistributedCoverageFromTopLevel() throws Exception {
        final String targetName = "execute-distributed-coverage-from-top-level"
        testBase.setUp()
        testBase.executeTarget(targetName)

        // check instrumented file content
        final String expectedProfilesCode =
                "public static ${CloverProfile.name}\\[\\] profiles = \\{ " +
                        "new ${CloverProfile.name}" +
                        ".*\\\\u0064\\\\u0065\\\\u0066\\\\u0061\\\\u0075\\\\u006c\\\\u0074.*" + // "default" as unicode
                        "\\};"
        assertFileMatches(expectedProfilesCode, getCloverFooInstrFile(), false)

        // check execution log
        assertStringContains(
                String.format(USING_PROFILE_MSG, "default", "FIXED"),
                getJavaOut(), false)
        assertStringMatches("Distributed coverage is enabled with: name=clover.tcp.server;host=host.from.top.level",
                getJavaOut(), false)
    }

    /**
     * Run 'execute-distributed-coverage-use-default' target.
     * Test input:
     * - distributed coverage is written in the profile
     * - clover.profile system property is not defined
     * Expected:
     * - as profile is not selected, it will take the "default" one, and enable distributed coverage
     *
     * @throws Exception
     */
    @Test
    void testDistributedCoverageUseDefault() throws Exception {
        final String targetName = "execute-distributed-coverage-use-default"
        testBase.setUp()
        testBase.executeTarget(targetName)

        // check instrumented file content
        final String expectedProfilesCode =
                "public static ${CloverProfile.name}\\[\\] profiles = \\{ " +
                        "new ${CloverProfile.name}" +
                        ".*\\\\u0064\\\\u0065\\\\u0066\\\\u0061\\\\u0075\\\\u006c\\\\u0074.*" + // "default" as unicode
                        "\\};"
        assertFileMatches(expectedProfilesCode, getCloverFooInstrFile(), false)

        // check execution log
        assertStringContains(String.format(USING_PROFILE_MSG, "default", "FIXED"), getJavaOut(), false)
        assertStringMatches("Distributed coverage is enabled with: name=clover.tcp.server;host=host.from.profile",
                getJavaOut(), false)
    }

    /**
     * Run 'disable-clover-at-runtime' target.
     * Input:
     *   clover.enable=false system property
     * Expected:
     *   NullRecorder used for instrumented code
     * @throws Exception
     */
    @Test
    void testDisableCloverAtRuntime() throws Exception {
        final String targetName = "disable-clover-at-runtime"
        testBase.setUp()
        testBase.executeTarget(targetName)

        // check execution log
        assertStringContains(DISABLING_CLOVER_MSG, getJavaOut(), false)
        assertStringContains("NullRecorder", getJavaOut(), false)
    }
}