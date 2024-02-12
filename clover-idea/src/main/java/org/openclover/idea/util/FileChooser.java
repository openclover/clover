package org.openclover.idea.util;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.openclover.runtime.Logger;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper for {@link com.intellij.openapi.fileChooser.FileChooser}. The original one have difference
 * in interface between IDEA versions.
 */
public class FileChooser  {

    @Nullable
    public static VirtualFile[] chooseFiles(final Component component, final FileChooserDescriptor fileChooser) {
        final String idea12FailMessage = "Failed to execute FileChooser.chooseFiles() - possible API incompatiblity. Falling back to older API.";
        final String idea11FailMessage = "Failed to execute FileChooser.chooseFiles() - possible API incompatiblity. Report a bug?";
        VirtualFile[] vf = null;
        boolean tryOldApi;

        try {
            // try calling IDEA12+ API first
            // vf = FileChooser.chooseFiles(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(), this, null, null)
            final Method chooseFiles = com.intellij.openapi.fileChooser.FileChooser.class.getMethod("chooseFiles",
                    FileChooserDescriptor.class, Component.class, Project.class, VirtualFile.class);
            vf = (VirtualFile[])chooseFiles.invoke(null, fileChooser, component, null, null);
            tryOldApi = false;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            Logger.getInstance().verbose(idea12FailMessage, ex);
            tryOldApi = true;
        }

        if (tryOldApi) {
            try {
                // no luck? fallback to IDEA11 and older
                // vf = FileChooser.chooseFiles(component, fileChooser)
                final Method chooseFiles = com.intellij.openapi.fileChooser.FileChooser.class.getMethod("chooseFiles",
                        Component.class, FileChooserDescriptor.class);
                vf = (VirtualFile[])chooseFiles.invoke(null, component, fileChooser);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                Logger.getInstance().verbose(idea11FailMessage, ex);
            }
        }

        return vf;
    }

}
