package org.openclover.core.registry.metrics;

import org.openclover.core.api.registry.HasMetrics;

public class BlockMetrics implements org.openclover.core.api.registry.BlockMetrics {
    /**
     * Constant used to indicate that given metric cannot be calculated (for example statement
     * percentage coverage when code has 0 statements), so it's value is undefined.
     */
    public static float VALUE_UNDEFINED = -1.0f;

    private HasMetrics owner;
    private int numStatements;
    private int numCoveredStatements;
    private int numBranches;
    private int numCoveredBranches;
    private int complexity;
    private int numTests;
    private int numTestPasses;
    private int numTestFailures;
    private int numTestErrors;
    private double testExecutionTime;

    /**
     * Returns true if given metric value is undefined.
     * @see #VALUE_UNDEFINED
     * @param value - value to be checked
     * @return true if it equals VALUE_UNDEFINED, false otherwise
     */
    public static boolean isUndefined(final float value) {
        return (value == VALUE_UNDEFINED);
    }

    public BlockMetrics(HasMetrics owner) {
        this.owner = owner;
    }

    @Override
    public HasMetrics getOwner() {
        return owner;
    }

    public void setOwner(HasMetrics owner) {
        this.owner = owner;
    }

    @Override
    public String getType() {
       return "block";
    }

    @Override
    public int getNumStatements() {
        return numStatements;
    }

    public void addNumStatements(int numStatements) {
        this.numStatements += numStatements;
    }

    public void setNumStatements(int numStatements) {
        this.numStatements = numStatements;
    }

    @Override
    public int getNumCoveredStatements() {
        return numCoveredStatements;
    }

    public void addNumCoveredStatements(int numCoveredStatements) {
        this.numCoveredStatements += numCoveredStatements;
    }

    public void setNumCoveredStatements(int numCoveredStatements) {
        this.numCoveredStatements = numCoveredStatements;
    }

    @Override
    public int getNumBranches() {
        return numBranches;
    }

    public void addNumBranches(int numBranches) {
        this.numBranches += numBranches;
    }

    public void setNumBranches(int numBranches) {
        this.numBranches = numBranches;
    }

    @Override
    public int getNumCoveredBranches() {
        return numCoveredBranches;
    }

    public void addNumCoveredBranches(int numCoveredBranches) {
        this.numCoveredBranches += numCoveredBranches;
    }

    public void setNumCoveredBranches(int numCoveredBranches) {
        this.numCoveredBranches = numCoveredBranches;
    }

    @Override
    public int getComplexity() {
        return complexity;
    }

    public void addComplexity(int complexity) {
        this.complexity += complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    @Override
    public int getNumTests() {
        return numTests;
    }

    public void setNumTests(int numTests) {
        this.numTests = numTests;
    }

    @Override
    public int getNumTestPasses() {
        return numTestPasses;
    }

    public void setNumTestPasses(int numTestPasses) {
        this.numTestPasses = numTestPasses;
    }

    @Override
    public int getNumTestFailures() {
        return numTestFailures;
    }

    public void setNumTestFailures(int numTestFailures) {
        this.numTestFailures = numTestFailures;
    }

    @Override
    public int getNumTestErrors() {
        return numTestErrors;
    }

    public void setNumTestErrors(int numTestErrors) {
        this.numTestErrors = numTestErrors;
    }

    @Override
    public double getTestExecutionTime() {
        return testExecutionTime;
    }

    public void setTestExecutionTime(double testExecutionTime) {
        this.testExecutionTime = testExecutionTime;
    }

    // derived metrics

    @Override
    public int getNumElements() {
        return numStatements + numBranches;
    }

    @Override
    public int getNumCoveredElements() {
        return numCoveredStatements + numCoveredBranches;
    }

    @Override
    public int getNumUncoveredElements() {
        return getNumElements() - getNumCoveredElements();
    }

    @Override
    public float getPcCoveredStatements() {
       return getFraction(numCoveredStatements, numStatements);
    }

    @Override
    public float getPcCoveredBranches() {
       return getFraction(numCoveredBranches, numBranches);
    }

    @Override
    public float getPcCoveredElements() {
       return getFraction(getNumCoveredElements(), getNumElements());
    }

    @Override
    public float getPcUncoveredElements() {
        float pcCovered = getPcCoveredElements();
        return pcCovered < 0 ? VALUE_UNDEFINED : 1.0f - pcCovered;
    }

    @Override
    public double getAvgTestExecutionTime() {
        return getFraction(getTestExecutionTime(), getNumTestsRun());
    }

    @Override
    public float getPcTestPasses() {
        return getFraction(getNumTestPasses(), getNumTestsRun());
    }

    @Override
    public float getPcTestErrors() {
        return getFraction(getNumTestErrors(), getNumTestsRun());
    }

    @Override
    public float getPcTestFailures() {
        return getFraction(getNumTestFailures(), getNumTestsRun());
    }

    @Override
    public int getNumTestsRun() {
        return getNumTestErrors() + getNumTestFailures() + getNumTestPasses();
    }

    @Override
    public float getPcTestPassesFailures() {
        int denom = getNumTestPasses() + getNumTestFailures();
        return denom > 0 ? getFraction(getNumTestPasses(), denom) : 0f;
    }

    @Override
    public boolean isEmpty() {
        return getNumElements() == 0;
    }

    @Override
    public float getComplexityDensity() {
        return getFraction(complexity, numStatements);
    }

    public BlockMetrics add(org.openclover.core.api.registry.BlockMetrics metrics) {
        numStatements += metrics.getNumStatements();
        numBranches += metrics.getNumBranches();
        numCoveredStatements += metrics.getNumCoveredStatements();
        numCoveredBranches += metrics.getNumCoveredBranches();
        complexity += metrics.getComplexity();
        numTests += metrics.getNumTests();
        numTestPasses += metrics.getNumTestPasses();
        numTestErrors += metrics.getNumTestErrors();
        numTestFailures += metrics.getNumTestFailures();
        testExecutionTime += metrics.getTestExecutionTime();
        return this;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlockMetrics that = (BlockMetrics) o;

        if (complexity != that.complexity) return false;
        if (numBranches != that.numBranches) return false;
        if (numCoveredBranches != that.numCoveredBranches) return false;
        if (numCoveredStatements != that.numCoveredStatements) return false;
        if (numStatements != that.numStatements) return false;
        if (numTestErrors != that.numTestErrors) return false;
        if (numTestFailures != that.numTestFailures) return false;
        if (numTestPasses != that.numTestPasses) return false;
        return numTests == that.numTests;
    }

    public int hashCode() {
        int result;
        result = numStatements;
        result = 31 * result + numCoveredStatements;
        result = 31 * result + numBranches;
        result = 31 * result + numCoveredBranches;
        result = 31 * result + complexity;
        result = 31 * result + numTests;
        result = 31 * result + numTestPasses;
        result = 31 * result + numTestFailures;
        result = 31 * result + numTestErrors;
        return result;
    }

    protected double getFraction(double num, int dem) {
        if (dem != 0) {
            return num / dem;
        } else {
            return VALUE_UNDEFINED;
        }
    }

    protected float getFraction(float num, int dem) {
        if (dem != 0) {
            return num / dem;
        } else {
            return VALUE_UNDEFINED;
        }
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "BlockMetrics{" +
            "owner=" + owner +
            ", numStatements=" + numStatements +
            ", numCoveredStatements=" + numCoveredStatements +
            ", numBranches=" + numBranches +
            ", numCoveredBranches=" + numCoveredBranches +
            ", complexity=" + complexity +
            ", numTests=" + numTests +
            ", numTestPasses=" + numTestPasses +
            ", numTestFailures=" + numTestFailures +
            ", numTestErrors=" + numTestErrors +
            ", testExecutionTime=" + testExecutionTime +
            '}';
    }

    ///CLOVER:ON

}
