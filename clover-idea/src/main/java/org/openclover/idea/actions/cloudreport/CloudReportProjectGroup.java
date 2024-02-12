package org.openclover.idea.actions.cloudreport;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import org.openclover.idea.report.cloud.CloudVirtualFile;

public class CloudReportProjectGroup extends DefaultActionGroup {
    @Override
    public void update(AnActionEvent e) {
        final Project project = e.getData(DataKeys.PROJECT);
        if (project == null) {
            e.getPresentation().setVisible(false);
        } else {
            final CloudVirtualFile cvf = CloudVirtualFile.getInstance(project);
            e.getPresentation().setVisible(cvf.getSelectedElement() == null);
        }
    }
}
