package com.atlassian.clover.instr.groovy

import com.atlassian.clover.CloverNames

import org.junit.Test

import static com.atlassian.clover.groovy.utils.TestUtils.assertStringContains
import static com.atlassian.clover.groovy.utils.TestUtils.assertStringMatches
import com_atlassian_clover.CloverProfile
import com.atlassian.clover.remote.DistributedConfig

/**
 * Integration tests that detect if the correct list of CloverProfile's is being embedded
 * during compilation and selected at runtime.
 *
 * @see com.atlassian.clover.ant.tasks.CloverSetupTaskProfilesCorrectTest
 */
public class GroovyProfilesTest extends TestBase {
    public static final String PROPERTY_NOT_FOUND_MSG =
        "CLOVER: System property '" + CloverNames.PROP_CLOVER_PROFILE + "' was not found. Assuming the 'default' profile.";

    public static final String USING_PROFILE_MSG = "CLOVER: Using profile '%s' with settings [coverageRecorder=%s";

    public static final String NO_PROFILES_DEFINED_MSG = "CLOVER: No profiles defined in instrumented classes. Using standard settings.";

    public static final String PROFILE_NOT_FOUND_MSG = "CLOVER: Profile '%s' not found in instrumented classes. Using standard settings.";

    public static final String fooGroovyContent = """
                public class Foo {
                  public static void main(String[] args) {
                    println "Foo!"
                  }
                }
              """;

    public GroovyProfilesTest(methodName, specificName, groovyAllJar) {
        super(methodName, specificName, groovyAllJar);
    }

    public GroovyProfilesTest(String testName) {
        super(testName);
    }

    /**
     * Test input:
     *  - no profiles compiled into code,
     *  - no clover.profile system property
     * Expected:
     *  - when there are no profiles, Clover selects fixed coverage recorder.
     */
    public void testExecuteNoProfiles() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy": fooGroovyContent],
                "-Dclover.grover.ast.dump=true"
        )
        def result = run("Foo", ["-Dclover.logging.level=debug"])

        // check execution log
        assertStringContains(NO_PROFILES_DEFINED_MSG, result.stdOut, false)
    }

    /**
     * Test input:
     * - one "default" profile compiled into code,
     * - no clover.profile system property
     * Expected:
     *  - when profiles are compiled-in, but no system property defined, Clover selects the "default" profile.
     *
     * @throws Exception
     */
    public void testExecuteDefaultProfile() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy": fooGroovyContent],
                "-Dclover.grover.ast.dump=true",
                [],
                {
                    it.addProfile(new CloverProfile("default", "GROWABLE", null))
                    it
                }
        )
        def result = run("Foo", ["-Dclover.logging.level=debug"])

        // check execution log
        assertStringContains(PROPERTY_NOT_FOUND_MSG, result.stdOut, false);
        assertStringContains(String.format(USING_PROFILE_MSG, "default", "GROWABLE"), result.stdOut, false);
    }

    /**
     * Test:
     * - profiles "default", "other", "remote" compiled into code,
     * - clover.profile=other system property
     * Expected:
     * - Clover selects coverage recorder from "other" profile
     */
    public void testExecuteOtherProfile() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy": fooGroovyContent],
                "-Dclover.grover.ast.dump=true",
                [],
                {
                    it.addProfile(new CloverProfile("default", "GROWABLE", null))
                    it.addProfile(new CloverProfile("other", "SHARED", null))
                    it.addProfile(new CloverProfile(
                            "remote",
                            "FIXED",
                            new DistributedConfig("name=tcp-config;port=7777;host=myhost.com;timeout=500;numClients=10;retryPeriod=500").configString))
                    it
                }
        )
        def result = run("Foo", ["-Dclover.logging.level=debug", "-Dclover.profile=other"])

        // check execution log
        assertStringContains(String.format(USING_PROFILE_MSG, "other", "SHARED"), result.stdOut, false);
        assertStringContains("SharedCoverageRecorder[growableRecorder=GrowableCoverageRecorder[coverage=CoverageMatrix",
                result.stdOut, false);
    }

    /**
     * Test input:
     * - two profiles - "default" and "one" compiled into code,
     * - clover.profile=two system property
     * Expected:
     *  - "two" profile not found, warning, Clover selects standard coverage recorder.
     */
    public void testExecuteNotFoundProfile() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy": fooGroovyContent],
                "-Dclover.grover.ast.dump=true",
                [],
                {
                    it.addProfile(new CloverProfile("default", "GROWABLE", null))
                    it.addProfile(new CloverProfile("one", "FIXED", null))
                    it
                }
        )
        def result = run("Foo", ["-Dclover.logging.level=debug", "-Dclover.profile=two"])

        // check execution log
        assertStringContains(String.format(PROFILE_NOT_FOUND_MSG, "two"), result.stdOut, false);
        assertStringContains("FixedSizeCoverageRecorder", result.stdOut, false);
    }

    /**
     * Test is similar to 'execute-not-found-profile' but checks against null profiles array.
     * Test input:
     * - no profiles compiled into code,
     * - clover.profile=some system property
     * Expected: no profiles defined, selects standard coverage recorder
     */
    public void testExecuteNotFoundNullProfile() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy": fooGroovyContent],
                "-Dclover.grover.ast.dump=true"
        )
        def result = run("Foo", ["-Dclover.logging.level=debug", "-Dclover.profile=some"])

        // check execution log
        assertStringContains(NO_PROFILES_DEFINED_MSG, result.stdOut, false);
        assertStringContains("FixedSizeCoverageRecorder", result.stdOut, false);
    }

    /**
     * Test input:
     * - distributed coverage is written both as child node of <clover-instr/> and in the profile
     * - clover.profile=default system property
     * Expected:
     * - presence of the profile shall override settings from top-level node
     */
    public void testDistributedCoverageFromProfile() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy": fooGroovyContent],
                "-Dclover.grover.ast.dump=true",
                [],
                {
                    it.setDistributedConfig(new DistributedConfig("host=host.from.top.level;timeout=10"))
                    it.addProfile(new CloverProfile(
                            "default",
                            "FIXED",
                            new DistributedConfig("host=host.from.profile;timeout=10").configString))
                    it
                }
        )
        def result = run("Foo", ["-Dclover.logging.level=debug", "-Dclover.profile=default"])

        // check execution log
        assertStringContains(
                String.format(USING_PROFILE_MSG, "default", "FIXED distributedCoverage=host=host.from.profile;timeout=10]"),
                result.stdOut, false);
        assertStringMatches("Distributed coverage is enabled with: name=clover.tcp.server;host=host.from.profile",
                result.stdOut, false);
    }

    /**
     * Test input:
     * - distributed coverage is written both as child node of <clover-instr/>
     * - the 'default' profile has no distributed coverage defined
     * - clover.profile system property is not defined
     * Expected:
     * - as property is not present, 'default' profile is used, as it does not contain distributedCoverage tag,
     * settings from clover-instr/distributedCoverage are taken
     */
    @Test
    public void testDistributedCoverageFromTopLevel() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy": fooGroovyContent],
                "-Dclover.grover.ast.dump=true",
                [],
                {
                    it.setDistributedConfig(new DistributedConfig("host=host.from.top.level;timeout=10"))
                    it.addProfile(new CloverProfile("default", "FIXED", null))
                    it.addProfile(new CloverProfile(
                            "other",
                            "FIXED",
                            new DistributedConfig("host=host.from.profile;timeout=10").configString))
                    it
                }
        )
        def result = run("Foo", ["-Dclover.logging.level=debug"])

        // check execution log
        assertStringContains(
                String.format(USING_PROFILE_MSG, "default", "FIXED"),
                result.stdOut, false);
        assertStringMatches("Distributed coverage is enabled with: name=clover.tcp.server;host=host.from.top.level",
                result.stdOut, false);
    }

    /**
     * Test input:
     * - distributed coverage is written in the profile
     * - clover.profile system property is not defined
     * Expected:
     * - as profile is not selected, it will take the "default" one, and enable distributed coverage
     */
    @Test
    public void testDistributedCoverageUseDefault() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy": fooGroovyContent],
                "-Dclover.grover.ast.dump=true",
                [],
                {
                    it.setDistributedConfig(new DistributedConfig("host=host.from.top.level;timeout=10"))
                    it.addProfile(new CloverProfile(
                            "default",
                            "FIXED",
                            new DistributedConfig("host=host.from.profile;timeout=10").configString))
                    it
                }
        )
        def result = run("Foo", ["-Dclover.logging.level=debug"])

        // check execution log
        assertStringContains(String.format(USING_PROFILE_MSG, "default", "FIXED"), result.stdOut, false);
        assertStringMatches("Distributed coverage is enabled with: name=clover.tcp.server;host=host.from.profile",
                result.stdOut, false);
    }


    public GroovyProfilesTest(String methodName, String specificName, File groovyAllJar) {
        super(methodName, specificName, groovyAllJar)
    }
}