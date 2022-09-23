package com.atlassian.clover;

import com.atlassian.clover.recorder.GlobalCoverageRecordingTranscript;
import com.atlassian.clover.registry.CoverageDataProvider;

public interface ApplicationCoverage extends CoverageDataProvider {
    int getCoverageSize();
    void addCoverage(GlobalCoverageRecordingTranscript recording);
}
