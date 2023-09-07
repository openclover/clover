package org.openclover.eclipse.core.views.testrunexplorer.actions;

import org.openclover.eclipse.core.views.actions.SelectHierarchyStyleActionDelegate;
import org.openclover.eclipse.core.views.testrunexplorer.TestRunExplorerViewSettings;

public class SelectPackageRootsStyleAction extends SelectHierarchyStyleActionDelegate {
    public SelectPackageRootsStyleAction() {
        super(TestRunExplorerViewSettings.HIERARCHY_STYLE_PACKAGE_ROOTS);
    }
}
