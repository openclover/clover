package com.atlassian.clover.ant.tasks

import com.atlassian.clover.util.FileUtils
import groovy.transform.CompileStatic
import org.apache.tools.ant.BuildFileTestBase

/**
 * Declare the following properties to run this test:
 * project.dir=clover root directory
 */
@CompileStatic
abstract class CloverBuildFileTestBase extends BuildFileTestBase {
    protected CloverTestUtil util

    CloverBuildFileTestBase(String name) {
        super(name)
    }

    abstract String getAntFileName()

    void setUp() throws Exception {
        junitSetUp()
        mainSetUp()
        antFileSetUp()
    }

    void tearDown() throws Exception {
        antFileTearDown()
        mainTearDown()
        junitTearDown()
    }

    /**
     * Returns location of the working directory - 'project.dir' property.
     * @return File
     */
    File getProjectDir() {
        return util.getProjectDir()
    }

    protected void junitSetUp() throws Exception {
        super.setUp()
    }

    protected void junitTearDown() throws Exception {
        super.tearDown()
    }

    protected void antFileSetUp() {
        getProject().executeTarget("setUp")
    }

    protected void antFileTearDown() {
        getProject().executeTarget("tearDown")
    }

    protected void mainSetUp() throws Exception {
        util = new CloverTestUtil(getClass().getName(), getName())
        configureProject(copyFile(getAntFileName()).getAbsolutePath(), util.getProperties())
    }

    protected void mainTearDown() {
    }

    protected File copyFile(String name) throws IOException {
        File dest = new File(util.getWorkDir(), name)
        File src = new File(util.getPathToSourceFile(name))

        log("Copying file " + src + " to " + dest)
        FileUtils.fileCopy(src, dest)
        return dest
    }

    protected void log(String msg) {
        System.out.println("[ANTUNIT] " + msg)
    }

    protected void dumpLogsToFile(File file) {
        file.withWriter { out ->
            out.println "LOGS:"
            out.println getLog()
            out.println "OUTPUT:"
            out.println getOutput()
            out.println "ERROR LOG:"
            out.println getError()
        }
    }

}
