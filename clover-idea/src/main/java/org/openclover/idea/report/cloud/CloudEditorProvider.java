package org.openclover.idea.report.cloud;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.util.vfs.AbstractEditorProvider;

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