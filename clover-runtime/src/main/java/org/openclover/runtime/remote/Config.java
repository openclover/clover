package org.openclover.runtime.remote;

/**
 */
public interface Config {
    String SEP = ":";

    /**
     * Returns true if this config is enabled.
     * This should be checked before calling any other methods on the config.
     * @return true if enabled, false otherwise.
     */
    boolean isEnabled();

}
