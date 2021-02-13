package com.atlassian.clover.eclipse.core.ui.editors.cloud;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.views.actions.GenerateCloudJob;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

class RefreshCloudEditorGenerateCloudJob extends GenerateCloudJob {
    private CloudEditor cloudEditor;

    public RefreshCloudEditorGenerateCloudJob(CloudEditor cloudEditor, IProject project) {
        super(project);
        this.cloudEditor = cloudEditor;
    }

    @Override
    protected IStatus activateEditor() {
        try {
            cloudEditor.refresh(true);
        } catch (Throwable t) {
            return new Status(
                Status.ERROR,
                CloverPlugin.ID,
                0, "Refreshing the coverage cloud failed", t);
        }
        return Status.OK_STATUS;
    }
}
