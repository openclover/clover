package org.openclover.eclipse.core.views.actions;

import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.openclover.eclipse.core.views.ExplorerView;

public class CloverViewActionDelegate
    extends ActionDelegate
    implements IViewActionDelegate {

    protected ExplorerView view;

    @Override
    public void init(IViewPart view) {
        this.view = (ExplorerView)view;
    }
}
