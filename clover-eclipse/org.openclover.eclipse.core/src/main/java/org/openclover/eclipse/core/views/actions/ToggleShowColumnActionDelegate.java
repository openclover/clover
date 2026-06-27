package org.openclover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;

public abstract class ToggleShowColumnActionDelegate extends UntargetedCheckedViewActionDelegate {

    protected ToggleShowColumnActionDelegate(int columnId) {
    }

    @Override
    protected void onSelection(IAction action) {

    }

    protected abstract void enableColumn(boolean enabled);
}
