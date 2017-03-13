package com.atlassian.clover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

/**
 * Selects a hierarchy style in the coverage view.
 */
public abstract class SelectHierarchyStyleActionDelegate extends SelectionUntargetedCoverageViewActionDelegate {
    private int style;

    public SelectHierarchyStyleActionDelegate(int style) {
        this.style = style;
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        action.setChecked(view.getHierarchyStyle() == style);
    }

    @Override
    public void onSelection(IAction action) {
        view.setHierarchyStyle(style);
    }
}
