package com.atlassian.clover.idea.report.cloud;

import com.atlassian.clover.idea.util.vfs.AbstractEditorProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class CloudEditorProvider extends AbstractEditorProvider {

    CloudEditorProvider() {
        super("Cloud");
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile instanceof CloudVirtualFile;
    }

    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new CloudEditor(project, (CloudVirtualFile) virtualFile);
    }

}