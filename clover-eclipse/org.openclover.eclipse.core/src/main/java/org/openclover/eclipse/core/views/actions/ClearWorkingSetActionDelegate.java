package org.openclover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.core.runtime.IAdaptable;
import org.openclover.eclipse.core.CloverPlugin;

public class ClearWorkingSetActionDelegate extends UntargetedViewActionDelegate {

    @Override
    public void run(IAction action) {
        CloverPlugin.getInstance().getCloverWorkingSet().setElements(new IAdaptable[] {});
    }
}
