package com.atlassian.clover.eclipse.core.views.coverageexplorer.actions;

import com.atlassian.clover.eclipse.core.views.coverageexplorer.CoverageViewSettings;
import com.atlassian.clover.eclipse.core.views.actions.SelectHierarchyStyleActionDelegate;

public class SelectPackageRootsStyleAction extends SelectHierarchyStyleActionDelegate {
    public SelectPackageRootsStyleAction() {
        super(CoverageViewSettings.HIERARCHY_STYLE_PACKAGE_ROOTS);
    }
}
