package com.atlassian.clover.ant.testutils

import com.atlassian.clover.registry.metrics.*

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Helper class to set metrics
 */
class MetricsHelper {

    static void assertMetricsEquals(FileMetrics expectedMetrics, FileMetrics actualMetrics) {
        assertMetricsEquals(expectedMetrics, (ClassMetrics) actualMetrics)
        assertEquals("getLineCount", expectedMetrics.getLineCount(), actualMetrics.getLineCount())
        assertEquals("getNcLineCount", expectedMetrics.getNcLineCount(), actualMetrics.getNcLineCount())
    }

    static void assertMetricsEquals(ClassMetrics expectedMetrics, ClassMetrics actualMetrics) {
        assertMetricsEquals(expectedMetrics,
                (com.atlassian.clover.api.registry.BlockMetrics)actualMetrics)
        assertEquals("getNumMethods", expectedMetrics.getNumMethods(), actualMetrics.getNumMethods())
        assertEquals("getNumMethods", expectedMetrics.getNumTestMethods(), actualMetrics.getNumTestMethods())
        assertEquals("getNumCoveredMethods", expectedMetrics.getNumCoveredMethods(), actualMetrics.getNumCoveredMethods())
    }

    static void assertMetricsEquals(com.atlassian.clover.api.registry.BlockMetrics metrics, com.atlassian.clover.api.registry.BlockMetrics filteredMetrics) {
        assertEquals("getNumBranches", metrics.getNumBranches(), filteredMetrics.getNumBranches())
        assertEquals("getNumStatements", metrics.getNumStatements(), filteredMetrics.getNumStatements())
        assertEquals("getNumElements", metrics.getNumElements(), filteredMetrics.getNumElements())

        assertEquals("getNumCoveredBranches", metrics.getNumCoveredBranches(), filteredMetrics.getNumCoveredBranches())
        assertEquals("getNumCoveredStatements", metrics.getNumCoveredStatements(), filteredMetrics.getNumCoveredStatements())
        assertEquals("getNumCoveredElements", metrics.getNumCoveredElements(), filteredMetrics.getNumCoveredElements())
        assertEquals("getNumUncoveredElements", metrics.getNumUncoveredElements(), filteredMetrics.getNumUncoveredElements())

        assertEquals("getComplexity", metrics.getComplexity(), filteredMetrics.getComplexity())
        assertTrue("getComplexityDensity", metrics.getComplexityDensity() == filteredMetrics.getComplexityDensity())
    }

    static BlockMetrics setBlockMetrics(BlockMetrics metrics,
                                        int statements, int coveredStatements,
                                        int branches, int coveredBranches, int complexity,
                                        int tests, int testPasses, int testErrors, int testFailures,
                                        float testTime) {
        metrics.setNumStatements(statements)
        metrics.setNumCoveredStatements(coveredStatements)
        metrics.setNumBranches(branches)
        metrics.setNumCoveredBranches(coveredBranches)
        metrics.setComplexity(complexity)
        metrics.setNumTests(tests)
        metrics.setNumTestPasses(testPasses)
        metrics.setNumTestErrors(testErrors)
        metrics.setNumTestFailures(testFailures)
        metrics.setTestExecutionTime(testTime)
        metrics
    }

    static ClassMetrics setClassMetrics(ClassMetrics metrics,
                                        int statements, int coveredStatements,
                                        int branches, int coveredBranches, int complexity,
                                        int tests, int testPasses, int testErrors, int testFailures,
                                        float testTime,
                                        int methods, int coveredMethods) {
        setBlockMetrics(metrics, statements, coveredStatements, branches, coveredBranches, complexity,
                tests, testPasses, testErrors, testFailures, testTime)
        metrics.setNumMethods(methods)
        metrics.setNumCoveredMethods(coveredMethods)
        metrics
    }

    static FileMetrics setFileMetrics(FileMetrics metrics,
                                      int statements, int coveredStatements,
                                      int branches, int coveredBranches, int complexity,
                                      int tests, int testPasses, int testErrors, int testFailures,
                                      float testTime,
                                      int methods, int coveredMethods,
                                      int classes, int lineCount, int ncLineCount) {
        setClassMetrics(metrics, statements, coveredStatements, branches, coveredBranches, complexity,
                tests, testPasses, testErrors, testFailures, testTime,
                methods, coveredMethods)
        metrics.setNumClasses(classes)
        metrics.setLineCount(lineCount)
        metrics.setNcLineCount(ncLineCount)
        metrics
    }

    static PackageMetrics setPackageMetrics(PackageMetrics metrics,
                                            int statements, int coveredStatements,
                                            int branches, int coveredBranches, int complexity,
                                            int tests, int testPasses, int testErrors, int testFailures,
                                            float testTime,
                                            int methods, int coveredMethods,
                                            int classes, int lineCount, int ncLineCount,
                                            int files) {
        setFileMetrics(metrics, statements, coveredStatements, branches, coveredBranches, complexity,
                tests, testPasses, testErrors, testFailures, testTime,
                methods, coveredMethods,
                classes, lineCount, ncLineCount)
        metrics.setNumFiles(files)
        metrics
    }

    static ProjectMetrics setProjectMetrics(ProjectMetrics metrics,
                                            int statements, int coveredStatements,
                                            int branches, int coveredBranches, int complexity,
                                            int tests, int testPasses, int testErrors, int testFailures,
                                            float testTime,
                                            int methods, int coveredMethods,
                                            int classes, int lineCount, int ncLineCount,
                                            int files, int packages) {
        setPackageMetrics(metrics, statements, coveredStatements, branches, coveredBranches, complexity,
                tests, testPasses, testErrors, testFailures, testTime, methods, coveredMethods,
                classes, lineCount, ncLineCount, files)
        metrics.setNumPackages(packages)
        metrics
    }
}
