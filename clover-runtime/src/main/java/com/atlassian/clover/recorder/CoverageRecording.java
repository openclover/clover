package com.atlassian.clover.recorder;

/**
 * Recording of data
 */
public interface CoverageRecording {
    long getDbVersion();
    int getFormat();
}
