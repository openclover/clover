package com.atlassian.clover.ant.tasks

import clover.com.google.common.collect.Maps

abstract class CloverOptimizeTestBase extends CloverBuildFileTestBase {
    private int spaceCount
    protected Map<String, String> runTargetsForTests
    protected String defaultRunTarget

    protected CloverOptimizeTestBase(String name, String defaultRunTarget, Map<String, String> runTargetsForTests) {
        super(name)
        this.runTargetsForTests = runTargetsForTests
        this.defaultRunTarget = defaultRunTarget
    }

    String getAntFileName() {
        return "clover-optimize.xml"
    }

    protected String getRunTarget() {
        String target = runTargetsForTests.get(getName())
        if (target == null) {
            target = defaultRunTarget
        }
        return target
    }

    protected void mainSetUp() throws Exception {
        util = new CloverTestUtil(getClass().getName(), getName())
        initialiseBuild()
    }

    protected void expectNoTestRunResults(String cycle) throws Exception {
        File testOutputDir = new File(util.getWorkDir().getAbsolutePath(), cycle)
        assertTrue(
            testOutputDir.toString() + " directory should exist",
            testOutputDir.exists() && testOutputDir.isDirectory() && testOutputDir.canRead())
        String[] dirContents = testOutputDir.list()
        for (String dirContent : dirContents) {
            assertTrue(
                    "There should be no test runs for " + cycle + ": " + dirContent,
                    !dirContent.contains("TEST-com.cenqua.clover.testcases.testoptimization."))
        }
    }

    protected String cycle(String index) {
        return "cycle-" + index
    }

    protected String cycle(int index) {
        return cycle(Integer.toString(index))
    }

    protected void rebuildCycleThenRun(int index, boolean snapshot) throws IOException {
        getProject().setProperty("no.source.copy", "true")
        getProject().setProperty("no.source.modification", "true")
        buildThenRun(Integer.toString(index), snapshot, "clean", getRunTarget())
    }

    protected void buildThenRun(int index) throws IOException {
        buildThenRun(Integer.toString(index), true, "clean", getRunTarget())
    }

    protected void buildThenRun(int index, boolean snapshot) throws IOException {
        buildThenRun(Integer.toString(index), snapshot, "clean", getRunTarget())
    }

    protected void buildThenRun(int index, boolean snapshot, String runTestsTarget) throws IOException {
        buildThenRun(Integer.toString(index), snapshot, "clean", runTestsTarget)
    }
    
    protected void buildThenRun(int index, boolean snapshot, String cleanTarget, String runTestsTarget) throws IOException {
        buildThenRun(Integer.toString(index), snapshot, cleanTarget, runTestsTarget)
    }

    protected void buildThenRun(String index, boolean snapshot, String cleanTarget, String runTestsTarget) throws IOException {
        createCopyIncludesFile(index, "**/AppClass" + index + "*.java")

        getProject().setProperty("cycleindex", index)
        setCycleTagProperty(defaultCycleTag(index))
        executeTarget("with.clover")
        executeTarget(cleanTarget)
        executeTarget("build")
        getProject().setProperty("testresultsprefix", cycle(index))
        executeTarget(runTestsTarget)
        if (snapshot) {
            executeTarget("snapshot")
        }
    }

    private void setCycleTagProperty(String tag) {
        getProject().setProperty("cycletag", tag)
    }

    protected String defaultCycleTag(int index) {
        return defaultCycleTag(Integer.toString(index))
    }

    protected String defaultCycleTag(String index) {
        //Add index spaces to the end in order to to get around current
        //checksum collision limitations for small files
        return cycle(index) + spaces()
    }

    protected void buildComplete() throws IOException {
        initialiseBuild()
    }

    protected void initialiseBuild() throws IOException {
        configureProject(copyFile(getAntFileName()).getAbsolutePath(), newProjectProperties())
    }

    protected Map<String, String> newProjectProperties() {
        Map<String, String> properties = Maps.newHashMap(util.getProperties())
        File sampleProjectDir =
            new File(
                util.getProjectDir(),
                "tests-functional/src/test/resources/" + getTestSourceBaseName() + "/" + getName())
        if (!sampleProjectDir.exists()) {
            sampleProjectDir =
                new File(
                    util.getProjectDir(),
                    "tests-functional/src/test/resources/" + getTestSourceBaseName() + "/defaultSampleProject")
        }
        properties.put("sampleproject.dir", sampleProjectDir.getAbsolutePath())
        properties.put("outdir", util.getWorkDir().getAbsolutePath())
        return properties
    }

    protected abstract String getTestSourceBaseName()

    private String spaces() {
        StringBuilder buf = new StringBuilder()
        for(int i = 0; i < spaceCount; i++) {
            buf.append(" ")
        }
        spaceCount++
        return buf.toString()
    }

    protected void createCopyIncludesFile(String index, String pattern) throws IOException {
        File dir = new File(util.getWorkDir().getAbsolutePath(), cycle(index))
        dir.mkdirs()
        File includesFile = new File(dir, "includes.properties")
        includesFile.createNewFile()
        FileWriter fw = new FileWriter(includesFile)
        PrintWriter pw = new PrintWriter(fw)
        pw.write(pattern)
        pw.close()
    }

    protected void expectTestsRunResults(String cycle, Map<String, String[]> testsAndExpectations) throws Exception {
        File testOutputDir = new File(util.getWorkDir().getAbsolutePath(), cycle)
        assertTrue(
            testOutputDir.toString() + " directory should exist",
            testOutputDir.exists() && testOutputDir.isDirectory() && testOutputDir.canRead())

        assertEquals(
            "Too many/few test ran in cycle " + cycle + " - build was not optimized correctly",
            testsAndExpectations.size(),
            testOutputDir.listFiles(new FilenameFilter() {
                boolean accept(File dir, String name) {
                    return name.matches("TEST-com\\.cenqua\\.clover\\.testcases\\.testoptimization\\..*\\.xml")
                }
            }).length)

        for (Map.Entry<String, String[]> entry : testsAndExpectations.entrySet()) {
            String testClassName = entry.getKey()
            File testFile = new File(testOutputDir, "TEST-com.cenqua.clover.testcases.testoptimization." + testClassName + ".xml")
            assertTrue(
                    testFile.toString() + " file should exist for cycle " + cycle,
                    testFile.exists() && testFile.isFile() && testFile.canRead())
        }
    }

    protected void initialSourceCreation() {
        getProject().setProperty("no.source.modification", "true")
    }

    protected void noSourceChange() {
        getProject().setProperty("no.source.modification", "true")
        getProject().setProperty("no.source.copy", "true")
    }

    protected void sourceChange() {
        getProject().setProperty("no.source.copy", "true")
    }
}
