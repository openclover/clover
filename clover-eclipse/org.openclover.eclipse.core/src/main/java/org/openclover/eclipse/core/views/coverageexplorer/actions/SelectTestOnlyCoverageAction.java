package org.openclover.eclipse.core.views.coverageexplorer.actions;

import org.openclover.eclipse.core.views.coverageexplorer.CoverageViewSettings;

public class SelectTestOnlyCoverageAction extends SelectCoverageAction {

    public SelectTestOnlyCoverageAction() {
        super(CoverageViewSettings.COVERAGE_MODEL_TEST_ONLY);
    }
}
