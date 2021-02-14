package org.openclover.eclipse.core.views.coverageexplorer.actions;

import org.openclover.eclipse.core.views.coverageexplorer.CoverageViewSettings;

public class SelectFullCoverageAction extends SelectCoverageAction {

    public SelectFullCoverageAction() {
        super(CoverageViewSettings.COVERAGE_MODEL_FULL);
    }
}
