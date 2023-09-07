package com.atlassian.clover.idea;

import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.coverage.CoverageManager;
import com.atlassian.clover.idea.feature.FeatureManager;

public interface IProjectPlugin {
    public IdeaCloverConfig getConfig();
    public boolean isEnabled();
    public CoverageManager getCoverageManager();
    public FeatureManager getFeatureManager();
}
