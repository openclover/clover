package org.openclover.eclipse.core.views.actions;

import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.editors.cloud.CloudEditor;
import org.openclover.eclipse.core.ui.editors.cloud.CloudProjectInput;
import org.openclover.eclipse.core.projects.CloverProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ide.IDE;

public class OpenCloudActionDelegate extends GenerateCloudActionDelegate {

    @Override
    protected GenerateCloudJob createCloudJob(final IProject project) {
        return new GenerateCloudJob(project) {

            @Override
            protected IStatus activateEditor() {
                final IStatus[] openEditorStatus = new IStatus[] {Status.OK_STATUS};

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IDE.openEditor(
                                getPage(),
                            new CloudProjectInput(CloverProject.getFor(project)),
                                CloudEditor.ID);
                        } catch (Throwable t) {
                            openEditorStatus[0] = new Status(Status.ERROR, CloverPlugin.ID, 0, CloverEclipsePluginMessages.FAILED_TO_OPEN_CLOUD_EDITOR(), t);
                        }
                    }
                });
                return openEditorStatus[0];
            }

        };
    }
}
