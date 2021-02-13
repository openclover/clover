package com.atlassian.clover.eclipse.core.views.coverageexplorer.actions;

import com.atlassian.clover.eclipse.core.views.coverageexplorer.CoverageViewSettings;

public class SelectTestOnlyCoverageAction extends SelectCoverageAction {

    public SelectTestOnlyCoverageAction() {
        super(CoverageViewSettings.COVERAGE_MODEL_TEST_ONLY);
    }
}
