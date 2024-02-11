package org.openclover.runtime.recorder;

/**
 * Recording of data
 */
public interface CoverageRecording {
    long getDbVersion();
    int getFormat();
}
