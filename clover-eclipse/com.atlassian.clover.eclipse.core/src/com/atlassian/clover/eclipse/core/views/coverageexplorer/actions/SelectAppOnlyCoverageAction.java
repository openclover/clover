package com.atlassian.clover.eclipse.core.views.coverageexplorer.actions;

import com.atlassian.clover.eclipse.core.views.coverageexplorer.CoverageViewSettings;

public class SelectAppOnlyCoverageAction extends SelectCoverageAction {
    public SelectAppOnlyCoverageAction() {
        super(CoverageViewSettings.COVERAGE_MODEL_APP_ONLY);
    }
}
