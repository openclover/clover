package org.openclover.eclipse.core.exclusion;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public abstract class BaseActionDelegate implements IObjectActionDelegate {

    protected ICloverExcludable selectedElement;
    protected IWorkbenchPart part;

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        part = targetPart;
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        selectedElement = null;
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structured = (IStructuredSelection) selection;
            final Object firstElement = structured.getFirstElement();
            if (firstElement instanceof ICloverExcludable) {
                final ICloverExcludable element = (ICloverExcludable) firstElement;
                selectedElement = element;
            }
        }
        action.setEnabled(selectedElement != null);
    }

}