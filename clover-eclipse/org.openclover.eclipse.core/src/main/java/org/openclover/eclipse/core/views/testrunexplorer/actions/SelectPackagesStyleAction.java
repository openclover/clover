package org.openclover.eclipse.core.views.testrunexplorer.actions;

import org.openclover.eclipse.core.views.actions.SelectHierarchyStyleActionDelegate;
import org.openclover.eclipse.core.views.testrunexplorer.TestRunExplorerViewSettings;

public class SelectPackagesStyleAction extends SelectHierarchyStyleActionDelegate {
    public SelectPackagesStyleAction() {
        super(TestRunExplorerViewSettings.HIERARCHY_STYLE_PACKAGES);
    }
}
