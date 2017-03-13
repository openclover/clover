package com.atlassian.clover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;

public abstract class UntargetedCheckedViewActionDelegate extends UntargetedViewActionDelegate {
    @Override
    public final void run(IAction action) {
        if (action.isChecked()) {
            onSelection(action);
        }
    }

    protected abstract void onSelection(IAction action);
}
