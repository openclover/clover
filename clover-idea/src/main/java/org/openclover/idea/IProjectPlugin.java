package org.openclover.idea;

import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.feature.FeatureManager;

public interface IProjectPlugin {
    public IdeaCloverConfig getConfig();
    public boolean isEnabled();
    public CoverageManager getCoverageManager();
    public FeatureManager getFeatureManager();
}
