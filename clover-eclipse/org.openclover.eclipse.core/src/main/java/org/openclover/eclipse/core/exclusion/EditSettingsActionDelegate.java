package org.openclover.eclipse.core.exclusion;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.openclover.eclipse.core.projects.settings.ProjectPropertyPage;

public class EditSettingsActionDelegate extends BaseActionDelegate {

    @Override
    public void run(IAction action) {
        if (part != null && selectedElement != null) {
            try {
                ProjectPropertyPage.focusSourceTab(selectedElement.getProject());
            } catch (CoreException e) {
                // ignore
            }
            final PreferenceDialog propertyDialog = PreferencesUtil.createPropertyDialogOn(part.getSite().getShell(),
                    selectedElement.getProject(), ProjectPropertyPage.ID, null, null);
            propertyDialog.open();
        }
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        part = targetPart;
    }
}
