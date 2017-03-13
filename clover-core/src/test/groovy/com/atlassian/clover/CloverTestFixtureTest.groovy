package com.atlassian.clover

import clover.com.google.common.collect.Lists
import com.atlassian.clover.CloverTestFixture.Clazz
import com.atlassian.clover.CloverTestFixture.Coverage
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.registry.metrics.PackageMetrics
import com.atlassian.clover.registry.metrics.ProjectMetrics
import com.atlassian.clover.support.IOHelper
import junit.framework.TestCase

class CloverTestFixtureTest extends TestCase {

    private CloverTestFixture subject
    private File tmpDir

    CloverTestFixtureTest(String testName) {
        super(testName)
    }

    void setUp() throws Exception {
        super.setUp()
        // create a temporary directory.
        tmpDir = IOHelper.createTmpDir(CloverTestFixtureTest.class.getName())
        subject = new CloverTestFixture(tmpDir)
    }

    void tearDown() throws Exception {
        if (!IOHelper.delete(tmpDir)) {
            throw new RuntimeException("Unable to delete temporary test directory " +
                    tmpDir.getAbsolutePath())
        }
        super.tearDown()
    }

    void testSingleClass() throws Exception {
        List<Clazz> classList = Lists.newArrayList()
        classList.add(new Clazz(tmpDir, "com.cenqua", "Test",new Coverage(0.90f, 0.80f, 0.85f)))

        String initStr = subject.createCoverageDB()
        subject.register(initStr, classList)
        subject.write(initStr, classList)

        CloverDatabase db = new CloverDatabase(initStr)
        db.loadCoverageData();        
        ProjectMetrics coverage = (ProjectMetrics)db.getModel(CodeType.APPLICATION).getMetrics()

        assertEquals(0.80f, coverage.getPcCoveredBranches(), 0.001f)
        assertEquals(0.85f, coverage.getPcCoveredMethods(), 0.001f)
        assertEquals(0.90f, coverage.getPcCoveredStatements(), 0.001f)

        List packages = db.getModel(CodeType.APPLICATION).getAllPackages()
        assertEquals(1, packages.size())

        PackageMetrics metrics = (PackageMetrics)((FullPackageInfo)packages.get(0)).getMetrics()

        assertEquals(0.80f, metrics.getPcCoveredBranches(), 0.001f)
        assertEquals(0.85f, metrics.getPcCoveredMethods(), 0.001f)
        assertEquals(0.90f, metrics.getPcCoveredStatements(), 0.001f)
    }

}
