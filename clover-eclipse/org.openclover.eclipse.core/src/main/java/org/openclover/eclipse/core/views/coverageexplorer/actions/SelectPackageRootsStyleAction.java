package org.openclover.eclipse.core.views.coverageexplorer.actions;

import org.openclover.eclipse.core.views.coverageexplorer.CoverageViewSettings;
import org.openclover.eclipse.core.views.actions.SelectHierarchyStyleActionDelegate;

public class SelectPackageRootsStyleAction extends SelectHierarchyStyleActionDelegate {
    public SelectPackageRootsStyleAction() {
        super(CoverageViewSettings.HIERARCHY_STYLE_PACKAGE_ROOTS);
    }
}
