package org.openclover.eclipse.core.views.actions;

public abstract class SingleCloverProjectActionDelegate extends MultiCloverProjectActionDelegate {
    @Override
    public int getMaxSelectedProjectCount() {
        return 1;
    }
}
