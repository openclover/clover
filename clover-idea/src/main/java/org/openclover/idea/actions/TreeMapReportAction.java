package org.openclover.idea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.openclover.idea.report.treemap.TreeMapVirtualFile;

public class TreeMapReportAction extends AbstractReportAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = DataKeys.PROJECT.getData(event.getDataContext());
        if (project != null) {
            final VirtualFile vf = TreeMapVirtualFile.getInstance(project);
            FileEditorManager.getInstance(project).openFile(vf, true);
        }
    }
}
