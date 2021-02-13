package com.atlassian.clover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;

public class NullCloverProjectActionDelegate extends CloverProjectActionDelegate {
    @Override
    protected void updateStateForSelection(IAction action) {
        action.setEnabled(false);
    }
}
