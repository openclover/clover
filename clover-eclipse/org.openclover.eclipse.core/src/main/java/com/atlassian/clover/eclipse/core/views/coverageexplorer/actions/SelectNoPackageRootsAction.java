package com.atlassian.clover.eclipse.core.views.coverageexplorer.actions;

import com.atlassian.clover.eclipse.core.views.coverageexplorer.CoverageViewSettings;
import com.atlassian.clover.eclipse.core.views.actions.SelectHierarchyStyleActionDelegate;

public class SelectNoPackageRootsAction extends SelectHierarchyStyleActionDelegate {
    public SelectNoPackageRootsAction() {
        super(CoverageViewSettings.HIERARCHY_STYLE_NO_PACKAGE_ROOTS);
    }
}
