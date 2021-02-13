package com.atlassian.clover.eclipse.core.views.coverageexplorer.actions;

import com.atlassian.clover.eclipse.core.views.coverageexplorer.CoverageViewSettings;

public class SelectFullCoverageAction extends SelectCoverageAction {

    public SelectFullCoverageAction() {
        super(CoverageViewSettings.COVERAGE_MODEL_FULL);
    }
}
