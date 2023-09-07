package com.atlassian.clover.idea.actions;

import com.atlassian.clover.idea.util.tmp.TmpPathResolver;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;

public class JumpToActualSourceAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        final OpenFileDescriptor ofd = getOpenFileDescriptor(e);
        if (ofd != null) {
            FileEditorManager.getInstance(e.getData(DataKeys.PROJECT)).openTextEditor(ofd, true);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        final boolean enabled = getOpenFileDescriptor(e) != null;
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(enabled);
    }

    private static OpenFileDescriptor getOpenFileDescriptor(AnActionEvent e) {
        final Navigatable navigatable = e.getData(DataKeys.NAVIGATABLE);
        if (navigatable instanceof OpenFileDescriptor) {
            OpenFileDescriptor ofd = (OpenFileDescriptor) navigatable;
            String tmpPath = ofd.getFile().getPath();
            VirtualFile origFile = ServiceManager.getService(e.getData(DataKeys.PROJECT), TmpPathResolver.class).getMapping(tmpPath);
            if (origFile != null) {
                return new OpenFileDescriptor(ofd.getProject(), origFile, ofd.getOffset());
            }
        }

        return null;
    }

}
