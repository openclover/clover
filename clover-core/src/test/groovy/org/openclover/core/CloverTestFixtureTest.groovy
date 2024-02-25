package org.openclover.core

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openclover.buildutil.testutils.IOHelper
import org.openclover.core.CloverTestFixture.Clazz
import org.openclover.core.CloverTestFixture.Coverage
import org.openclover.core.registry.entities.FullPackageInfo
import org.openclover.core.registry.metrics.PackageMetrics
import org.openclover.core.registry.metrics.ProjectMetrics

import static org.junit.Assert.assertEquals
import static org.openclover.core.util.Lists.newArrayList

class CloverTestFixtureTest {

    private CloverTestFixture subject
    private File tmpDir

    @Before
    void setUp() throws Exception {
        // create a temporary directory.
        tmpDir = IOHelper.createTmpDir(CloverTestFixtureTest.class.getName())
        subject = new CloverTestFixture(tmpDir)
    }

    @After
    void tearDown() throws Exception {
        if (!IOHelper.delete(tmpDir)) {
            throw new RuntimeException("Unable to delete temporary test directory " +
                    tmpDir.getAbsolutePath())
        }
    }

    @Test
    void testSingleClass() throws Exception {
        List<Clazz> classList = newArrayList()
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

        PackageMetrics metrics = (PackageMetrics) packages.get(0).getMetrics()

        assertEquals(0.80f, metrics.getPcCoveredBranches(), 0.001f)
        assertEquals(0.85f, metrics.getPcCoveredMethods(), 0.001f)
        assertEquals(0.90f, metrics.getPcCoveredStatements(), 0.001f)
    }

}
