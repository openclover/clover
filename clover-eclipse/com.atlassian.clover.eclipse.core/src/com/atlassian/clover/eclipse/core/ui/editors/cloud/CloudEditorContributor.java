package com.atlassian.clover.eclipse.core.ui.editors.cloud;

import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.ui.CloverPluginIcons;
import com.atlassian.clover.eclipse.core.ui.editors.CloverProjectInput;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;

/**
 * Manages the installation/deinstallation of global actions for the cloud editors.
 * Responsible for the redirection of global actions to the active editor.
 */
public class CloudEditorContributor extends MultiPageEditorActionBarContributor {
    private CloudEditor cloudEditor;
    private Action refreshAction;

    public CloudEditorContributor() {
        super();
        createActions();
    }

    @Override
    public void setActiveEditor(IEditorPart part) {
        if (part instanceof CloudEditor) {
            cloudEditor = (CloudEditor) part;
        }
        super.setActiveEditor(part);
    }

    @Override
    public void setActivePage(IEditorPart part) {
    }

    private void createActions() {

        refreshAction = new Action() {
            @Override
            public void run() {
                final CloverProjectInput input =
                    cloudEditor == null
                        ? null
                        : (CloverProjectInput) cloudEditor.getEditorInput();

                if (input != null) {
                    new RefreshCloudEditorGenerateCloudJob(cloudEditor, input.getProject().getProject()).schedule();
                }
            }
        };

        refreshAction.setText(CloverEclipsePluginMessages.REFRESH_CLOUD());
        refreshAction.setToolTipText(CloverEclipsePluginMessages.REFRESH_CLOUD_TOOL_TIP());
        refreshAction.setImageDescriptor(CloverPlugin.getImageDescriptor(CloverPluginIcons.PROJECT_REFRESH_ICON));
    }

    @Override
    public void contributeToMenu(IMenuManager manager) {
        IMenuManager menu = manager.findMenuUsingPath(IWorkbenchActionConstants.M_PROJECT);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
        menu.add(new Separator());
        menu.add(refreshAction);
    }

    @Override
    public void contributeToToolBar(IToolBarManager manager) {
        manager.add(new Separator());
        manager.add(refreshAction);
    }

}
