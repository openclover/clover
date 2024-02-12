package org.openclover.eclipse.core.views.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.dialogs.IWorkingSetEditWizard;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.workingset.CloverWorkingSet;

public class EditWorkingSetActionDelegate extends UntargetedViewActionDelegate {

    @Override
    public void run(IAction action) {
        final CloverWorkingSet cloverWorkingSet = CloverPlugin.getInstance().getCloverWorkingSet();
        IWorkingSet ws = cloverWorkingSet.getWorkingSet();

        final IWorkingSetManager wsm = CloverPlugin.getInstance().getWorkbench().getWorkingSetManager();
        final IWorkingSetEditWizard wizard = wsm.createWorkingSetEditWizard(ws);

        WizardDialog dialog = new WizardDialog(view.getSite().getShell(), wizard);
        if (dialog.open() == Window.OK) {
            cloverWorkingSet.setElements(wizard.getSelection().getElements());
            //view.refresh();
        }
    }
}
