package org.openclover.runtime.recorder;

/**
 * Coverage for the whole app as opposed to per-test coverage. A marker interface with some helpful constants.
 */
public interface GlobalCoverageRecording extends CoverageRecording {
    int FORMAT = 0;
    String ALT_SUFFIX = ".1";
}
