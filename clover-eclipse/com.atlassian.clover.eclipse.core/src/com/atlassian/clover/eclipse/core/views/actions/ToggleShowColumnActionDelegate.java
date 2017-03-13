package com.atlassian.clover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;

public abstract class ToggleShowColumnActionDelegate extends UntargetedCheckedViewActionDelegate {
    private int columnId;

    protected ToggleShowColumnActionDelegate(int columnId) {
        this.columnId = columnId;
    }

    @Override
    protected void onSelection(IAction action) {

    }

    protected abstract void enableColumn(boolean enabled);
}
