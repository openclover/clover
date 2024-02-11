package org.openclover.idea.actions.cloudreport;

import org.openclover.core.registry.entities.FullPackageInfo;
import org.openclover.idea.actions.Constants;
import org.openclover.idea.report.cloud.CloudVirtualFile;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.entities.PackageFragment;
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
