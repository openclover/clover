package org.openclover.eclipse.core.views.actions;

import org.openclover.eclipse.core.views.SelectionUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import java.util.Set;

import static org.openclover.util.Sets.newHashSet;

public abstract class CloverProjectActionDelegate
    extends CloverViewActionDelegate
    implements IObjectActionDelegate {

    protected IWorkbenchPart part;
    protected Set<IProject> projects = newHashSet();

    @Override
    public void setActivePart(IAction action, IWorkbenchPart workbenchPart) {
        this.part = workbenchPart;
    }

    public IWorkbenchPage getPage() {
        return part == null
            ? view.getViewSite().getPage()
            : part.getSite().getPage();
    }

    @Override
    public final void selectionChanged(IAction action, ISelection selection) {
        projects = SelectionUtils.gatherProjectsForSelection(selection);
        updateStateForSelection(action);
    }

    protected void updateStateForSelection(IAction action) {
        action.setEnabled(true);
    }

    protected Shell getShell() {
        return getPage().getActivePart().getSite().getShell();
    }
}
