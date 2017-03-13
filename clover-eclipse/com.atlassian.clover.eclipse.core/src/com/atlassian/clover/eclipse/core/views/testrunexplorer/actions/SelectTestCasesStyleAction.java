package com.atlassian.clover.eclipse.core.views.testrunexplorer.actions;

import com.atlassian.clover.eclipse.core.views.actions.SelectHierarchyStyleActionDelegate;
import com.atlassian.clover.eclipse.core.views.testrunexplorer.TestRunExplorerViewSettings;

public class SelectTestCasesStyleAction extends SelectHierarchyStyleActionDelegate {
    public SelectTestCasesStyleAction() {
        super(TestRunExplorerViewSettings.HIERARCHY_STYLE_FLAT_TEST_CASES);
    }
}
