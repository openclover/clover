package org.openclover.groovy.instr;

import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.cfg.instr.InstrumentationConfig;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.runtime.recorder.FixedSizeCoverageRecorder;
import org.openclover.runtime.remote.DistributedConfig;
import org_openclover_runtime.CloverProfile;
import org_openclover_runtime.CoverageRecorder;

import java.util.List;

/**
 * Helper class containing configuration data which is being written into instrumented groovy classes.
 * It contains excerpts from {@link InstrumentationConfig}, {@link InstrumentationSession} and
 * {@link Clover2Registry}
 */
class GroovyInstrumentationConfig {
    /**
     * Path to Clover database.
     */
    final String initString;

    /**
     * Distributed Coverage configuration encoded as string
     *
     * @see DistributedConfig#getConfigString()
     */
    final String distConfig;

    /**
     * Clover registry version
     *
     * @see InstrumentationSession#getVersion()
     */
    final long registryVersion;

    /**
     * Bit mask containing recorder settings like flush policy etc.
     *
     * @see CoverageRecorder#getConfigBits
     */
    final long recorderConfig;

    /**
     * Required capacity of the coverage recorder (for the {@link FixedSizeCoverageRecorder})
     *
     * @see FullFileInfo#getDataIndex()
     * @see FullFileInfo#getDataLength()
     */
    final int maxElements;

    /**
     * List of runtime profiles
     */
    final List<CloverProfile> profiles;

    GroovyInstrumentationConfig(String initString, String distConfig, long registryVersion, long recorderConfig, int maxElements, List<CloverProfile> profiles) {
        this.initString = initString;
        this.distConfig = distConfig;
        this.registryVersion = registryVersion;
        this.recorderConfig = recorderConfig;
        this.maxElements = maxElements;
        this.profiles = profiles;
    }
}
