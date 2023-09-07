package com.atlassian.clover.idea.actions.cloudreport;

import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.idea.actions.Constants;
import com.atlassian.clover.idea.report.cloud.CloudVirtualFile;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.PackageFragment;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

public class CloudPackageReportAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        final HasMetrics hasMetrics = Constants.SELECTED_HAS_METRICS.getData(e.getDataContext());
        if (hasMetrics instanceof FullPackageInfo || hasMetrics instanceof PackageFragment) {
            final Project project = DataKeys.PROJECT.getData(e.getDataContext());
            if (project != null) {
                final CloudVirtualFile vf = CloudVirtualFile.getInstance(project);
                vf.setSelectedElement(hasMetrics);
                FileEditorManager.getInstance(project).openFile(vf, true);
            }
        }

    }

    @Override
    public void update(AnActionEvent e) {
        final HasMetrics hasMetrics = Constants.SELECTED_HAS_METRICS.getData(e.getDataContext());
        e.getPresentation().setEnabled(hasMetrics instanceof FullPackageInfo || hasMetrics instanceof PackageFragment);
    }
}
