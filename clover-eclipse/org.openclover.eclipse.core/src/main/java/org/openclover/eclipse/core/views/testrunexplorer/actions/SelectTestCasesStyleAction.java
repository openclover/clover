package org.openclover.eclipse.core.views.testrunexplorer.actions;

import org.openclover.eclipse.core.views.actions.SelectHierarchyStyleActionDelegate;
import org.openclover.eclipse.core.views.testrunexplorer.TestRunExplorerViewSettings;

public class SelectTestCasesStyleAction extends SelectHierarchyStyleActionDelegate {
    public SelectTestCasesStyleAction() {
        super(TestRunExplorerViewSettings.HIERARCHY_STYLE_FLAT_TEST_CASES);
    }
}
