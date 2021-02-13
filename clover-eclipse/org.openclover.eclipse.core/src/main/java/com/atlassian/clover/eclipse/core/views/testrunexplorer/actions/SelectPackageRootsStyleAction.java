package com.atlassian.clover.eclipse.core.views.testrunexplorer.actions;

import com.atlassian.clover.eclipse.core.views.actions.SelectHierarchyStyleActionDelegate;
import com.atlassian.clover.eclipse.core.views.testrunexplorer.TestRunExplorerViewSettings;

public class SelectPackageRootsStyleAction extends SelectHierarchyStyleActionDelegate {
    public SelectPackageRootsStyleAction() {
        super(TestRunExplorerViewSettings.HIERARCHY_STYLE_PACKAGE_ROOTS);
    }
}
