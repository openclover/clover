package org.openclover.idea.actions.cloudreport;

import org.openclover.idea.actions.AbstractReportAction;
import org.openclover.idea.report.cloud.CloudVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

public class CloudReportAction extends AbstractReportAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            final CloudVirtualFile vf = CloudVirtualFile.getInstance(project);
            vf.setSelectedElement(null);
            FileEditorManager.getInstance(project).openFile(vf, true);
        }

    }
}