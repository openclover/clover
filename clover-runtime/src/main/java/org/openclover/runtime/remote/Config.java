package org.openclover.runtime.remote;

/**
 */
public interface Config {
    String SEP = ":";

    /**
     * Returns the name of this configuration object
     * @return the name of this configuration
     */
    String getName();

    /**
     * Returns true if this config is enabled.
     * This should be checked before calling any other methods on the config.
     * @return true if enabled, false otherwise.
     */
    boolean isEnabled();

}
