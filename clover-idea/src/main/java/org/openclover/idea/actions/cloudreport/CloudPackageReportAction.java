package org.openclover.idea.actions.cloudreport;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageFragment;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.idea.actions.Constants;
import org.openclover.idea.report.cloud.CloudVirtualFile;

public class CloudPackageReportAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        final HasMetrics hasMetrics = Constants.SELECTED_HAS_METRICS.getData(e.getDataContext());
        if (hasMetrics instanceof PackageInfo || hasMetrics instanceof PackageFragment) {
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
        e.getPresentation().setEnabled(hasMetrics instanceof PackageInfo || hasMetrics instanceof PackageFragment);
    }
}
