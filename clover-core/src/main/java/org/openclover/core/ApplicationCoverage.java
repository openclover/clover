package org.openclover.core;

import org.openclover.core.recorder.GlobalCoverageRecordingTranscript;
import org.openclover.core.api.registry.CoverageDataProvider;

public interface ApplicationCoverage extends CoverageDataProvider {
    int getCoverageSize();
    void addCoverage(GlobalCoverageRecordingTranscript recording);
}
