package com.atlassian.clover.idea.actions.cloudreport;

import com.atlassian.clover.idea.report.cloud.CloudVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;

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
