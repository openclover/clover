package com.atlassian.clover.ant.tasks

import clover.org.jdom.Document
import clover.org.jdom.Element
import clover.org.jdom.JDOMException
import org.junit.After
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

import static org.junit.Assert.assertArrayEquals
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

/**
 * Base class for &lt;clover-setup/&gt; tests.
 */
abstract class CloverSetupTaskTestBase {
    protected final CloverBuildFileTestBase testBase

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            File dumpFile = new File(testBase.util.getProperties().get("outdir"), "${description.getClassName()}.${description.getMethodName()}.${System.currentTimeMillis()}")
            dumpFile.createNewFile()
            testBase.dumpLogsToFile(dumpFile)
        }
    };

    CloverSetupTaskTestBase(final String antBuildFile) {
        testBase = new CloverBuildFileTestBase(getClass().getName()) {
            @Override
            String getAntFileName() {
                return antBuildFile
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        testBase.tearDown()
    }

    /**
     * Returns list of per-test coverage files found
     * @return String[]
     */
    protected String[] getPerTestCoverageFiles() {
        return getCloverDir().list(new FilenameFilter() {
            boolean accept(File dir, String name) {
                return name.endsWith(".s"); // find '*.s'
            }
        })
    }

    /**
     * Returns a path to directory containing Clover files (clover.db, coverage files, reports etc)
     * @return File
     */
    protected File getCloverDir() {
        return new File(testBase.getProject().getProperty("build.clover.dir"))
    }

    /**
     * Returns file pointed by 'clover.db' property.
     * @return File
     */
    protected File getCloverDbFile() {
        return new File(testBase.getProject().getProperty("clover.db"))
    }

    /**
     * Returns file pointed by 'clover2.db' property.
     * @return File
     */
    protected File getCloverDb2File() {
        return new File(testBase.getProject().getProperty("clover2.db"))
    }

    /**
     * Returns a path to clover.xml XML report produced by build. Must be called after executeTarget().
     * @return File
     */
    protected File getCloverXmlFile() {
        return new File(testBase.getProject().getProperty("clover.xml.file"))
    }

    /**
     * Returns a path to Foo.js produced by build. File contains per-test coverage data.
     * @return File
     */
    protected File getCloverFooJsonFile() {
        return new File(testBase.getProject().getProperty("build.clover.dir") + File.separator
                + "default-pkg" + File.separator + "Foo.js")
    }

    /**
     * Returns a path to Foo.js produced by build. File contains per-test coverage data.
     * @return File
     */
    protected File getCloverFooTestJsonFile() {
        return new File(testBase.getProject().getProperty("build.clover.dir") + File.separator
                + "default-pkg" + File.separator + "FooTest.js")
    }

    /**
     * Returns content of the 'java.out' property
     * @return String
     */
    protected String getJavaOut() {
        return testBase.getProject().getProperty("java.out")
    }

    /**
     * Returns content of the 'java.out.X' property
     * @return String
     */
    protected String getJavaOut(int runNumber) {
        return testBase.getProject().getProperty("java.out." + runNumber)
    }

    /**
     * Returns content of the 'java.err' property
     * @return String
     */
    protected String getJavaErr() {
        return testBase.getProject().getProperty("java.err")
    }

    /**
     * Returns content of the 'java.err.X' property
     * @return String
     */
    protected String getJavaErr(int runNumber) {
        return testBase.getProject().getProperty("java.err." + runNumber)
    }

    protected void assertXmlFileContains(Map<String, int[]> expectedCoverage, File cloverXmlFile) throws JDOMException, IOException {
        XMLReportReader xmlReader = new XMLReportReader()
        Document xmlDoc = xmlReader.loadDocument(cloverXmlFile)

        List<Element> sourceFiles = xmlReader.getSourceFiles(xmlDoc)
        assertEquals(expectedCoverage.size(), sourceFiles.size())

        for (Element sourceFile : sourceFiles) {
            int[] actualHitCounts = xmlReader.getHitCountsForSourceFile(sourceFile)
            int[] expectedHitCounts = expectedCoverage.get(xmlReader.getSourceFileName(sourceFile))
            assertNotNull(actualHitCounts)
            assertNotNull(expectedHitCounts)
            assertArrayEquals(expectedHitCounts, actualHitCounts)
        }
    }

}
