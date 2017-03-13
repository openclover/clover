package com.atlassian.clover.test

import com.atlassian.clover.ant.tasks.CloverBuildFileTestBase
import com.atlassian.clover.ant.tasks.CloverTestUtil
import com.atlassian.clover.util.FileUtils

/**
 * Tests how Clover integrates with JUnit4 in order to intercept test names at runtime.
 * Test is using 'test/resources/parameterized-junit4' code sample.
 */
class ParameterizedJUnit4Test extends CloverBuildFileTestBase {

    static final String CLOVER_ANT_TEST_RESOURCES_DIR =
            "clover-ant${File.separator}src${File.separator}test${File.separator}resources"
    static final String CODE_EXAMPLE_DIR =
            "${CLOVER_ANT_TEST_RESOURCES_DIR}${File.separator}parameterized-junit4"

    ParameterizedJUnit4Test(String name) {
        super(name)
    }

    @Override
    protected void mainSetUp() throws Exception {
        // using different location for source files than other ant tests
        util = new CloverTestUtil(getClass().getName(), getName()) {
            @Override
            public String getPathToSourceFile(String filename) {
                "${projectDir.absolutePath}${File.separator}${CODE_EXAMPLE_DIR}${File.separator}${filename}"
            }
        }

        // copy build.xml to temporary directory, configure Ant project
        configureProject(copyFile(getAntFileName()).getAbsolutePath(), util.getProperties())
        // copy sources as well
        copyDir("src")
    }

    protected File copyDir(String name) throws IOException {
        def dest = new File(util.getWorkDir(), name)
        def src = new File(util.getPathToSourceFile(name))

        log("Copying file ${src} to ${dest}")
        FileUtils.dirCopy(src, dest, true)
        dest
    }

    @Override
    String getAntFileName() {
        "build.xml"
    }

    void testJUnitRunnerWithCloverInterceptor() throws Exception {
        executeTarget("clean")
        executeTarget("validate")
        assertAntOutputContains("Setting project property: validation.ok -> true")
    }
}
