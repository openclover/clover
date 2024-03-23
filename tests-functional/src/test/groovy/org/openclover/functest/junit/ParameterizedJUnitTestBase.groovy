package org.openclover.functest.junit

import groovy.transform.CompileStatic
import org.openclover.core.util.FileUtils
import org.openclover.functest.ant.tasks.CloverBuildFileTestBase
import org.openclover.functest.ant.tasks.CloverTestUtil

/**
 * Tests how OpenClover integrates with JUnit4 in order to intercept test names at runtime.
 * Test is using 'test/resources/parameterized-junit4' code sample.
 */
@CompileStatic
abstract class ParameterizedJUnitTestBase extends CloverBuildFileTestBase {

    static final String TEST_RESOURCES_DIR =
            "tests-functional${File.separator}src${File.separator}test${File.separator}resources"

    abstract String getCodeExampleDir();

    ParameterizedJUnitTestBase(String name) {
        super(name)
    }

    @Override
    protected void mainSetUp() throws Exception {
        // using different location for source files than other ant tests
        util = new CloverTestUtil(getClass().getName(), getName()) {
            @Override
            String getPathToSourceFile(String filename) {
                "${projectDir.absolutePath}${File.separator}${getCodeExampleDir()}${File.separator}${filename}"
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
