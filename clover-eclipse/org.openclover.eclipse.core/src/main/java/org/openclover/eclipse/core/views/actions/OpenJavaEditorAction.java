package org.openclover.eclipse.core.views.actions;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.ui.IWorkbenchSite;

/**
 * Action to open the currently selected object in the Java editor. If the current object
 * has no Java editor associated with it then opening does no proceed.
 */
public class OpenJavaEditorAction extends OpenAction {

    public OpenJavaEditorAction(IWorkbenchSite site) {
        super(site);
        setEnabled(false);
        setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
    }
}
