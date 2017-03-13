package com.atlassian.clover.eclipse.core.views.coverageexplorer.actions;

import com.atlassian.clover.eclipse.core.views.actions.UntargetedViewActionDelegate;
import com.atlassian.clover.eclipse.core.views.coverageexplorer.CoverageView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

public class HierarchyStyleTreeToggleAction extends UntargetedViewActionDelegate {
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        action.setChecked(((CoverageView)view).showPackageTree());
    }

    @Override
    public void run(IAction action) {
        ((CoverageView)view).showPackageTree(action.isChecked());
    }
}