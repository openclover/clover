package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.report.treemap.TreeMapVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

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
