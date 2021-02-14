package org.openclover.eclipse.core.views.coverageexplorer.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.openclover.eclipse.core.views.coverageexplorer.CoverageView;
import org.openclover.eclipse.core.views.actions.UntargetedViewActionDelegate;

public class HideCoverageUnavailableAction extends UntargetedViewActionDelegate {
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        action.setChecked(((CoverageView)view).shouldHideUnavailableCoverage());
    }

    @Override
    public void run(IAction action) {
        ((CoverageView)view).setShouldHideUnavailableCoverage(action.isChecked());
    }
}
