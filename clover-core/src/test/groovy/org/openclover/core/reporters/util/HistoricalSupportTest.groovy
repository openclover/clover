package org.openclover.core.reporters.util

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.FileInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.SourceInfo
import org.openclover.core.cfg.Percentage
import org.openclover.core.registry.FixedSourceRegion
import org.openclover.core.registry.entities.BaseClassInfo
import org.openclover.core.registry.entities.BaseFileInfo
import org.openclover.core.registry.entities.BasePackageInfo
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.FullPackageInfo
import org.openclover.core.registry.entities.FullProjectInfo
import org.openclover.core.registry.entities.Modifiers
import org.openclover.core.registry.metrics.BlockMetrics
import org.openclover.core.reporters.Column
import org.openclover.core.reporters.Columns
import org.openclover.runtime.api.CloverException

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 */
class HistoricalSupportTest {

    @Rule
    public TestName testName = new TestName()

    protected FullProjectInfo project(String projectName) {
        return new FullProjectInfo(projectName, 0)
    }

    protected FullPackageInfo pkg(FullProjectInfo parentProject, String packageName) {
        return new FullPackageInfo(parentProject, packageName, 0)
    }

    protected FullFileInfo file(FullPackageInfo parentPackage, String fileName) {
        return new FullFileInfo(parentPackage, new File(fileName), "UTF-8", 0, 0, 0, 0, 0, 0, 0)
    }

    protected FullClassInfo cls(FullPackageInfo parentPackage, FullFileInfo parentFile, String className) {
        return new FullClassInfo(
                parentPackage, parentFile, 0, className,
                new FixedSourceRegion(0, 0, 0, 0), new Modifiers(),
                false, false, false)
    }

    @Test
    void testGetProjectMetricsDiff() throws CloverException {
        FullProjectInfo thenProj = project("TestProject")
        FullPackageInfo thenPkg1 = pkg(thenProj, "pkg1")
        FullFileInfo thenFile1 = file(thenPkg1, "File1")
        FullClassInfo thenClass1 = cls(thenPkg1, thenFile1, "class1")

        thenProj.addPackage(thenPkg1)
        thenPkg1.addFile(thenFile1)
        thenFile1.addClass(thenClass1)

        FullProjectInfo nowProj = project("TestProject")
        FullPackageInfo nowPkg1 = pkg(nowProj, "pkg1")
        FullFileInfo nowFile1 = file(nowPkg1, "File1")
        FullClassInfo nowClass1 = cls(nowPkg1, nowFile1, "class1")

        FullPackageInfo nowPkg2 = pkg(nowProj, "pkg2")
        FullFileInfo nowFile2 = file(nowPkg2, "File2")
        FullClassInfo nowClass2 = cls(nowPkg2, nowFile2, "class2")

        nowProj.addPackage(nowPkg1)
        nowPkg1.addFile(nowFile1)
        nowFile1.addClass(nowClass1)

        nowProj.addPackage(nowPkg2)
        nowPkg2.addFile(nowFile2)
        nowFile2.addClass(nowClass2)

        setClassMetrics(thenClass1, 15, 10, 20, 5, 15)
        setClassMetrics(nowClass1, 15, 10, 20, 5, 15)
        setClassMetrics(nowClass2, 14,  9, 15, 1,  0)

        final List<MetricsDiffSummary> diffs = HistoricalSupport.getProjectClassesMetricsDiff(thenProj, nowProj, new Percentage("0%"), new Columns.Complexity(), false)
        assertEquals(diffs.toString(), 1, diffs.size())
        assertEquals(diffs.get(0).getName(), nowClass2.getQualifiedName())
    }

    @Test
    void testGetClassMetricsDiff() throws CloverException {
        BasePackageInfo pkgInfo = new BasePackageInfo(null, "TestPackage")
        BaseFileInfo fileInfo = new BaseFileInfo(pkgInfo, testName.methodName, "", 0, 0, 0, 0, 0)
        SourceInfo sourceRegion = new FixedSourceRegion(0,0,0,0)

        BaseClassInfo cThen = new BaseClassInfo(pkgInfo, fileInfo,
                "then", sourceRegion, new Modifiers(),
                false, false, false)
        BaseClassInfo cNow = new BaseClassInfo(pkgInfo, fileInfo,
                "now", sourceRegion, new Modifiers(),
                false, false, false)
        Percentage threshold = new Percentage("0%")

        setClassMetrics(cThen, 15, 10, 20, 5, 15)
        setClassMetrics( cNow, 14,  9, 15, 1,  0)

        MetricsDiffSummary diff


        Column colComplexity = new Columns.Complexity()
        diff = HistoricalSupport.getClassMetricsDiff(cThen, cNow, threshold, colComplexity)
        assertTrue(diff.getPcDiff() == -1)
        assertTrue(colComplexity.getNumber().intValue() == 15)


        Column colCmpCov = new Columns.ComplexityToCoverage()
        diff = HistoricalSupport.getClassMetricsDiff(cThen, cNow, threshold, colCmpCov)
        assertTrue(diff.getPcDiff() == 314)
        assertTrue(colCmpCov.getNumber().intValue() == 336)


        Column colUnCovElements = new Columns.UncoveredElements()
        diff = HistoricalSupport.getClassMetricsDiff(cThen, cNow, threshold, colUnCovElements)
        assertTrue(diff.getPcDiff() as int == 62)
        assertTrue(colUnCovElements.getNumber().intValue() == 95)
    }

    private void setClassMetrics(
        BaseClassInfo c, int complexity, int numBranches, int numStatements, int numCoveredBranches, int numCoveredStatements) {

        BlockMetrics metrics = new BlockMetrics(c)
        metrics.setComplexity(complexity)
        metrics.setNumBranches(numBranches)
        metrics.setNumStatements(numStatements)
        metrics.setNumCoveredBranches(numCoveredBranches)
        metrics.setNumCoveredStatements(numCoveredStatements)

        c.setMetrics(metrics)
    }
}
