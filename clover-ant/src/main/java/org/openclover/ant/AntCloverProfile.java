package org.openclover.ant;

import org_openclover_runtime.CloverProfile;
import org.openclover.runtime.remote.DistributedConfig;

import java.util.Arrays;
import java.util.Locale;

/**
 * Handles the following declaration
 * <pre>
 *     &lt;profile name="default" coverageRecorder="fixed|growable|shared"&gt;
 *        [&lt;distributedCoverage/&gt;]
 *     &lt;profile&gt;
 * </pre>
 * in Ant build script. Default attribute values are:
 *  <li>name=default</li>
 *  <li>coverageRecorder=fixed</li>
 *
 * @see AntCloverProfiles
 */
public class AntCloverProfile extends CloverProfile {

    public AntCloverProfile() {
        // initialize with default values
        super(DEFAULT_NAME, DEFAULT_COVERAGE_RECORDER, null);
    }

    /**
     * Setter for Ant's <code>&lt;profile name="..."&gt;</code> attribute.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Setter for Ant's <code>&lt;profile coverageRecorder="..."&gt;</code> attribute.
     */
    public void setCoverageRecorder(String recorderName) {
        try {
            // wrap String into enum in order to validate value
            coverageRecorder = CloverProfile.CoverageRecorderType.valueOf(recorderName.toUpperCase(Locale.ENGLISH)).toString();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid value of the coverageRecorder attribute. Allowed values: "
                    + Arrays.toString(CloverProfile.CoverageRecorderType.values()),
                    ex);
        }
    }

    /**
     * Setter for Ant's
     * <pre>
     *    &lt;profile&gt;
     *        &lt;distributedCoverage ... /&gt;
     *    &lt;profile&gt;
     * </pre>
     * @param config passed by Ant
     */
    public void addConfiguredDistributedCoverage(DistributedConfig config) {
        distributedCoverage = config;
    }
}
