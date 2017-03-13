package com.atlassian.clover;

import com.atlassian.clover.recorder.GlobalCoverageRecordingTranscript;
import com.atlassian.clover.registry.CoverageDataProvider;

public interface ApplicationCoverage extends CoverageDataProvider {
    public int getCoverageSize();
    public void addCoverage(GlobalCoverageRecordingTranscript recording);
}
