package com.atlassian.clover.idea.util.ui;

import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.FileInfoRegion;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

public class ScrollUtil {

    private ScrollUtil() {
    }

    public static void scrollToSourceRegion(Project project, FileInfoRegion region) {
        File srcFile = ((FullFileInfo) region.getContainingFile()).getPhysicalFile();
        VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(srcFile);


        if (vf != null) {
            final int line = region.getStartLine() - 1;
            final int column = region.getStartColumn() - 1;

            FileEditorManager fem = FileEditorManager.getInstance(project);
            OpenFileDescriptor ofd = new OpenFileDescriptor(project, vf, line, column);
            fem.openTextEditor(ofd, true);
        }
    }
}
