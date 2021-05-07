package org.openclover.eclipse.core.views.coverageexplorer;

import org.openclover.eclipse.core.views.ExplorerViewLabelProvider;

public class CoverageViewLabelProvider extends ExplorerViewLabelProvider {
    public CoverageViewLabelProvider(final CoverageViewSettings settings) {
        super(settings, settings.getTreeColumnSettings());
    }
}
