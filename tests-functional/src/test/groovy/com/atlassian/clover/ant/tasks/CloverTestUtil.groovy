package com.atlassian.clover.ant.tasks

import clover.com.google.common.collect.Lists
import clover.com.google.common.collect.Maps
import com.atlassian.clover.ant.testutils.CloverTestFixture
import com.atlassian.clover.testutils.IOHelper

class CloverTestUtil {
    static String PROP_INITSTRING = "clover-initstring"
    public static final String CLOVER_ANT_TEST_BUILD_FILES_DIR = "tests-functional/src/test/resources/com/atlassian/clover/ant/tasks"
    public static final String CLOVER_ANT_TEST_RUN_TMP_DIR = "tests-functional/target/testrun/tmp"

    /**
     * The directory from where the unit tests should be run.
     */
    private File workDir
    private File coverageDb
    private File projDir
    private File historyDir
    private String initString
    private Map<String, String> buildProperties = Maps.newHashMap()
    private float factor = 0.5f
    private String testName
    private String className

    CloverTestUtil(final String className, final String testName) throws Exception {
        this.className = className
        this.testName = testName
        projDir = IOHelper.getProjectDir()

        createWorkDir()
        createHistoryDir()
        setUpCoverageDb()

        coverageDb = new File(initString)
        buildProperties.put(PROP_INITSTRING, initString)
        buildProperties.put("repkg.clover.jar", System.getProperty("repkg.clover.jar"))
        buildProperties.put("history.dir", historyDir.getAbsolutePath())
        buildProperties.put("test.name", testName)
        buildProperties.put("testclass.path", className.replace('.', '/'))
        buildProperties.put("outdir", getWorkDir().getAbsolutePath())
        if (File.separatorChar == '\\') {
            // we have to escape backslash with another backslash - @see RecorderInstrEmitter.asUnicodeString for details
            buildProperties.put("file.separator.unicodevalue", String.format("\\u%04x\\u%04x", (int) File.separatorChar, (int) File.separatorChar))
        } else {
            buildProperties.put("file.separator.unicodevalue", String.format("\\u%04x", (int) File.separatorChar))
        }
    }

    void setFactor(final float factor) {
        this.factor = factor
    }

    void setInitString(final String initString) {
        this.initString = initString
    }

    void setWorkDir(final File workDir) {
        this.workDir = workDir
    }

    File getWorkDir() {
        return workDir
    }

    File getCoverageDb() {
        return coverageDb
    }

    Map<String, String> getProperties() {
        return buildProperties
    }

    File getHistoryDir() {
        return historyDir
    }

    String getInitString() {
        return initString
    }

    File getProjectDir() {
        return projDir
    }

    void setUpCoverageDb() throws Exception {
        setUpCoverageDb(factor)
    }

    /**
     * Sets up the coverage DB by initialising it with
     * known classes and coverage metrics. This will create
     * a new coverageBD each call, and set the location of the db
     * to initString.
     *
     * @param moveFactor
     * @throws IOException
     */
    void setUpCoverageDb(final float moveFactor) throws Exception {
        // create a coverage database.
        final CloverTestFixture fixture = new CloverTestFixture(workDir)
        final List<CloverTestFixture.Clazz> classList = createClassList(moveFactor, workDir)

        initString = fixture.createCoverageDB()
        fixture.register(initString, classList)
        fixture.write(initString, classList)
    }

    List<CloverTestFixture.Clazz> createClassList(final float moveFactor, final File workDir) throws IOException {
        final List<CloverTestFixture.Clazz> classList = Lists.newArrayList()

        classList.add(new CloverTestFixture.Clazz(workDir, "com.cenqua", "Blah",
                new CloverTestFixture.Coverage(0.90f, 0.80f, 0.85f)))

        classList.add(new CloverTestFixture.Clazz(workDir, "com.cenqua.empty", "Empty",
                new CloverTestFixture.Coverage(0f, 0f, 0f, 0)))

        classList.add(new CloverTestFixture.Clazz(workDir, "com.cenqua.none", "NoCover",
                new CloverTestFixture.Coverage(0f, 0f, 0f, 10)))

        classList.add(new CloverTestFixture.Clazz(workDir, "com.cenqua.full", "FullCover",
                new CloverTestFixture.Coverage(1f, 1f, 1f)))

        classList.add(new CloverTestFixture.Clazz(workDir, "com.cenqua.mover", "Mover",
                new CloverTestFixture.Coverage(moveFactor, moveFactor, moveFactor)))

        classList.add(new CloverTestFixture.Clazz(workDir, "com.cenqua.mover", "Shaker",
                new CloverTestFixture.Coverage(moveFactor * 0.5 as float, moveFactor * 0.5 as float, moveFactor * 0.5 as float)))

        classList.add(new CloverTestFixture.Clazz(workDir, "com.cenqua.mover", "Loser",
                new CloverTestFixture.Coverage(1.0 - moveFactor as float, 1.0 - moveFactor as float, 1.0 - moveFactor as float)))

        return classList
    }

    private void createWorkDir() {
        workDir = new File(new File(new File(projDir, CLOVER_ANT_TEST_RUN_TMP_DIR), className.replace('.', '_').replace('$', '_')), testName)
        IOHelper.delete(workDir)
        workDir.mkdirs()
    }

    String getPathToSourceFile(final String filename) {
        return projDir.getAbsolutePath() + File.separator +
                CLOVER_ANT_TEST_BUILD_FILES_DIR + File.separator + filename
    }   

    private void createHistoryDir() {
        historyDir = new File(workDir, "history")
        historyDir.mkdirs()
    }
}
