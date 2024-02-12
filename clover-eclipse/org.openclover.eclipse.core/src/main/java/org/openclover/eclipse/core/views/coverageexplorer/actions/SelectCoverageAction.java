package org.openclover.eclipse.core.views.coverageexplorer.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.openclover.eclipse.core.views.actions.SelectionUntargetedCoverageViewActionDelegate;
import org.openclover.eclipse.core.views.coverageexplorer.CoverageView;

public abstract class SelectCoverageAction extends SelectionUntargetedCoverageViewActionDelegate {
    private int coverageModel;

    public SelectCoverageAction(int style) {
        this.coverageModel = style;
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        action.setChecked(((CoverageView)view).getCoverageModel() == coverageModel);
    }

    @Override
    public void onSelection(IAction action) {
        ((CoverageView)view).setCoverageModel(coverageModel);
    }
}
