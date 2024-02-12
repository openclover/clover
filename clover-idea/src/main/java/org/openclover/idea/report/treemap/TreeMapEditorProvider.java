package org.openclover.idea.report.treemap;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.util.vfs.AbstractEditorProvider;

public class TreeMapEditorProvider extends AbstractEditorProvider {

    TreeMapEditorProvider() {
        super("TreeMap");
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile instanceof TreeMapVirtualFile;
    }

    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new TreeMapEditor(project, (TreeMapVirtualFile) virtualFile);
    }

}
