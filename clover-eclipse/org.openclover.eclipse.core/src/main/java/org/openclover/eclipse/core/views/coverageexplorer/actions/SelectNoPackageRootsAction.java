package org.openclover.eclipse.core.views.coverageexplorer.actions;

import org.openclover.eclipse.core.views.coverageexplorer.CoverageViewSettings;
import org.openclover.eclipse.core.views.actions.SelectHierarchyStyleActionDelegate;

public class SelectNoPackageRootsAction extends SelectHierarchyStyleActionDelegate {
    public SelectNoPackageRootsAction() {
        super(CoverageViewSettings.HIERARCHY_STYLE_NO_PACKAGE_ROOTS);
    }
}
