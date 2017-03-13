package com.atlassian.clover.api.registry;

/**
 *
 */
public interface BlockMetrics {

    boolean isEmpty();

    HasMetrics getOwner();

    String getType();

    // cyclomatic complexity

    int getComplexity();

    float getComplexityDensity();

    // statements

    int getNumStatements();

    int getNumCoveredStatements();

    float getPcCoveredStatements();

    // branches

    int getNumBranches();

    int getNumCoveredBranches();

    float getPcCoveredBranches();

    // elements = statements + branches

    int getNumElements();

    int getNumCoveredElements();

    float getPcCoveredElements();

    int getNumUncoveredElements();

    float getPcUncoveredElements();

    // tests

    int getNumTests();

    int getNumTestPasses();

    int getNumTestFailures();

    int getNumTestErrors();

    int getNumTestsRun();

    double getTestExecutionTime();

    double getAvgTestExecutionTime();

    float getPcTestPasses();

    float getPcTestErrors();

    float getPcTestFailures();

    float getPcTestPassesFailures();

}
