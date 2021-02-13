package com.atlassian.clover.eclipse.core.views.coverageexplorer;

import com.atlassian.clover.eclipse.core.views.ExplorerViewLabelProvider;

public class CoverageViewLabelProvider extends ExplorerViewLabelProvider {
    public CoverageViewLabelProvider(final CoverageViewSettings settings) {
        super(settings, settings.getTreeColumnSettings());
    }
}
