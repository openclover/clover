package org.openclover.core.registry.metrics;

import org.openclover.core.api.registry.HasMetrics;

public class ClassMetrics extends BlockMetrics {

    private int numMethods;
    private int numCoveredMethods;
    private int numTestMethods;

    public ClassMetrics(HasMetrics owner) {
        super(owner);
    }

    @Override
    public String getType() {
       return "class";
    }

    public int getNumMethods() {
        return numMethods;
    }

    public void addNumMethods(int numMethods) {
        this.numMethods += numMethods;
    }

    public void setNumMethods(int numMethods) {
        this.numMethods = numMethods;
    }

    public String getChildType() {
        return "method";
    }

    public int getNumChildren() {
        return getNumMethods();
    }

    public int getNumCoveredMethods() {
        return numCoveredMethods;
    }

    public void addNumCoveredMethods(int numCoveredMethods) {
        this.numCoveredMethods += numCoveredMethods;
    }

    public void setNumCoveredMethods(int numCoveredMethods) {
        this.numCoveredMethods = numCoveredMethods;
    }

    public int getNumTestMethods() {
        return numTestMethods;
    }

    public void addNumTestMethods(int numTestMethods) {
        this.numTestMethods += numTestMethods;
    }

    public void setNumTestMethods(int numTestMethods) {
        this.numTestMethods = numTestMethods;
    }

    // derived metrics

    @Override
    public int getNumElements() {
        return super.getNumElements() + numMethods;
    }

    @Override
    public int getNumCoveredElements() {
        return super.getNumCoveredElements() + numCoveredMethods;
    }

    public float getPcCoveredMethods() {
        return getFraction(getNumCoveredMethods(), getNumMethods());
    }

    public float getAvgMethodComplexity() {
        return getFraction(super.getComplexity(), getNumMethods());
    }

    public float getAvgStatementsPerMethod() {
        return getFraction(getNumStatements(), getNumMethods());
    }

    public ClassMetrics add(ClassMetrics metrics) {
        super.add(metrics);
        numMethods += metrics.numMethods;
        numCoveredMethods += metrics.numCoveredMethods;
        numTestMethods += metrics.numTestMethods;
        return this;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ClassMetrics that = (ClassMetrics) o;

        if (numCoveredMethods != that.numCoveredMethods) return false;
        if (numMethods != that.numMethods) return false;
        return numTestMethods == that.numTestMethods;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + numMethods;
        result = 31 * result + numCoveredMethods;
        result = 31 * result + numTestMethods;
        return result;
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "ClassMetrics{" +
            "numMethods=" + numMethods +
            ", numCoveredMethods=" + numCoveredMethods +
            ", numTestMethods=" + numTestMethods +
            "} " + super.toString();
    }
    ///CLOVER:ON
}
