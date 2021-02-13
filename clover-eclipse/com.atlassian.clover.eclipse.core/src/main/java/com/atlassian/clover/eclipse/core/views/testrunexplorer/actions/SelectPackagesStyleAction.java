package com.atlassian.clover.eclipse.core.views.testrunexplorer.actions;

import com.atlassian.clover.eclipse.core.views.actions.SelectHierarchyStyleActionDelegate;
import com.atlassian.clover.eclipse.core.views.testrunexplorer.TestRunExplorerViewSettings;

public class SelectPackagesStyleAction extends SelectHierarchyStyleActionDelegate {
    public SelectPackagesStyleAction() {
        super(TestRunExplorerViewSettings.HIERARCHY_STYLE_PACKAGES);
    }
}
