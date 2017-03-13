package com_atlassian_clover;

import com.atlassian.clover.remote.DistributedConfig;

import java.io.Serializable;

/**
 * Contains information about the clover profile which was defined during instrumentation
 * and can be selected at runtime via {@link com.atlassian.clover.CloverNames#PROP_CLOVER_PROFILE} system property.
 *
 * Profiles allows to change Clover configuration at runtime without recompiling the sources.
 */
public class CloverProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Possible coverage recorder types */
    public static enum CoverageRecorderType {
        /** Fixed array size recorder, requires presence of clover.db to read the size */
        FIXED,
        /** Resizeable array recorder, grows when needed, no need to have clover.db */
        GROWABLE,
        /** As GROWABLE, but in addition it's being shared if the same initstring is used */
        SHARED
    }

    public static final String DEFAULT_NAME = "default";

    /** Enum field kept as String for serialization under JDK1.4 */
    public static final String DEFAULT_COVERAGE_RECORDER = CoverageRecorderType.FIXED.toString();

    protected String name;

    /** Enum field kept as String for serialization under JDK1.4 */
    protected String coverageRecorder;

    protected DistributedConfig distributedCoverage;

    public CloverProfile(String name, CoverageRecorderType coverageRecorder, DistributedConfig distributedCoverage) {
        this.name = name;
        setCoverageRecorder(coverageRecorder);
        this.distributedCoverage = distributedCoverage;
    }

    /**
     * Constructor with primitive strings. Used by {@link com.atlassian.clover.instr.java.RecorderInstrEmitter}
     * @param name
     * @param coverageRecorder
     * @param distributedCoverage can be null
     */
    public CloverProfile(String name, String coverageRecorder, String distributedCoverage) {
        this.name = name;
        setCoverageRecorder(CoverageRecorderType.valueOf(coverageRecorder)); // wrap String into enum for validation
        setDistributedCoverage(distributedCoverage);
    }

    public DistributedConfig getDistributedCoverage() {
        return distributedCoverage;
    }

    public CoverageRecorderType getCoverageRecorder() {
        return CoverageRecorderType.valueOf(coverageRecorder);
    }

    public String getName() {
        return name;
    }

    protected void setCoverageRecorder(CoverageRecorderType coverageRecorder) {
        this.coverageRecorder = coverageRecorder.toString();
    }

    protected void setDistributedCoverage(String distributedCoverage) {
        if (distributedCoverage != null) {
            this.distributedCoverage = new DistributedConfig(distributedCoverage);
        } else {
            this.distributedCoverage = null;
        }
    }
}
