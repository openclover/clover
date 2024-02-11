package com.atlassian.clover.cfg.instr;

import org.openclover.runtime.api.CloverException;


public class MethodContextDef extends StatementContextDef {
    private int maxComplexity = Integer.MAX_VALUE;
    private int maxStatements = Integer.MAX_VALUE;
    private int maxAggregatedComplexity = Integer.MAX_VALUE;
    private int maxAggregatedStatements = Integer.MAX_VALUE;

    public MethodContextDef() {
    }

    public MethodContextDef(String name, String regexp,
                            int maxComplexity, int maxStatements,
                            int maxAggregatedComplexity, int maxAggregatedStatements) {
        super(name, regexp);
        this.maxComplexity = maxComplexity;
        this.maxStatements = maxStatements;
        this.maxAggregatedComplexity = maxAggregatedComplexity;
        this.maxAggregatedStatements = maxAggregatedStatements;
    }

    public int getMaxComplexity() {
        return maxComplexity;
    }

    public void setMaxComplexity(int maxComplexity) {
        this.maxComplexity = maxComplexity;
    }

    public int getMaxStatements() {
        return maxStatements;
    }

    public void setMaxStatements(int maxStatements) {
        this.maxStatements = maxStatements;
    }

    public int getMaxAggregatedComplexity() {
        return maxAggregatedComplexity;
    }

    public void setMaxAggregatedComplexity(int maxAggregatedComplexity) {
        this.maxAggregatedComplexity = maxAggregatedComplexity;
    }

    public int getMaxAggregatedStatements() {
        return maxAggregatedStatements;
    }

    public void setMaxAggregatedStatements(int maxAggregatedStatements) {
        this.maxAggregatedStatements = maxAggregatedStatements;
    }

    @Override
    public void validate() throws CloverException {
        if (maxComplexity < 0) {
            throw new CloverException("maxComplexity must be greater than 0");
        }
        if (maxComplexity == 0) {
            throw new CloverException("maxComplexity must be greater than 0 because methods have a minimum complexity of 1");
        }
        if (maxStatements < 0) {
            throw new CloverException("maxStatements must be >= 0");
        }
        if (maxAggregatedComplexity < 0) {
            throw new CloverException("maxAggregatedComplexity must be >= 0");
        }
        if (maxAggregatedStatements < 0) {
            throw new CloverException("maxAggregatedStatements must be >= 0");
        }
        if (getRegexp() == null || getRegexp().trim().length() == 0) {
            setRegexp(".*");
        }
        super.validate();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodContextDef that = (MethodContextDef) o;

        if (maxComplexity != that.maxComplexity) return false;
        if (maxStatements != that.maxStatements) return false;
        if (maxAggregatedComplexity != that.maxAggregatedComplexity) return false;
        return maxAggregatedStatements == that.maxAggregatedStatements;
    }

    @Override
    public int hashCode() {
        int result = maxComplexity;
        result = 31 * result + maxStatements;
        result = 31 * result + maxAggregatedComplexity;
        result = 31 * result + maxAggregatedStatements;
        return result;
    }
}
