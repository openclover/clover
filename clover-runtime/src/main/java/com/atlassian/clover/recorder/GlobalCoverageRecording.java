package com.atlassian.clover.recorder;

/**
 * Coverage for the whole app as opposed to per-test coverage. A marker interface with some helpful constants.
 */
public interface GlobalCoverageRecording extends CoverageRecording {
    public static final int FORMAT = 0;
    public static final String ALT_SUFFIX = ".1";
}
