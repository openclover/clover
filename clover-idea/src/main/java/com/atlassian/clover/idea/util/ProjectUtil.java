package com.atlassian.clover.idea.util;

import com.atlassian.clover.idea.util.vfs.VfsUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;

public class ProjectUtil {

    private ProjectUtil() {
    }

    /**
     * The project workspace is the root directory below which clover should
     * use as its work area. All build / tmp files should be generated within
     * the project workspace.
     *
     * @param project project to retrieve workspace for
     * @return workspace dir
     */
    public static File getProjectWorkspace(final Project project) {
        return new File(getProjectDirectory(project), ".clover");
    }

    @NotNull
    public static File getProjectDirectory(final Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project argument can not be null.");
        }
        Application app = ApplicationManager.getApplication();
        return app.runReadAction((Computable<File>) () -> {
            final VirtualFile baseDir = project.getBaseDir();
            final String basePath = baseDir != null ? baseDir.getPath() : FileUtil.getTempDirectory();
            return new File(basePath);
        });
    }

    /**
     * The module workspace is similar to the project workspace. However, it
     * relates to a projects module rather then the project itself.
     *
     * @param module module
     * @return module workspace dir
     */
    public static File getModuleWorkspace(final Module module) {
        Application app = ApplicationManager.getApplication();
        return app.runReadAction((Computable<File>) () -> {
            File projectWksp = getProjectWorkspace(module.getProject());
            return new File(projectWksp, module.getName());
        });
    }

    // returns true when model has been modified
    public static boolean excludeFromProject(final Project project, final VirtualFile excludedDir) {
        final ModuleManager mManager = ModuleManager.getInstance(project);
        final Module[] modules = mManager.getModules();

        boolean modified = false;

        for (Module module : modules) {
            final ModuleRootManager rManager = ModuleRootManager.getInstance(module);

            if (rManager.getFileIndex().isInContent(excludedDir) && needsAddingExcludes(rManager, excludedDir)) {
                final ModifiableRootModel rootModel = rManager.getModifiableModel();
                for (ContentEntry modifiableEntry : rootModel.getContentEntries()) {
                    if (needsAddingAnExclude(modifiableEntry, excludedDir)) {
                        modifiableEntry.addExcludeFolder(excludedDir);
                    }
                }
                rootModel.commit();
                modified = true;
            }
        }
        return modified;
    }

    private static boolean needsAddingExcludes(ModuleRootManager rManager, VirtualFile excludedDir) {
        for (ContentEntry entry : rManager.getContentEntries()) {
            if (needsAddingAnExclude(entry, excludedDir)) {
                return true;
            }
        }
        return false;
    }

    private static boolean needsAddingAnExclude(ContentEntry entry, VirtualFile excludedDir) {
        final VirtualFile contentEntryFile = entry.getFile();

        return !Arrays.asList(entry.getExcludeFolderFiles()).contains(excludedDir)
                && contentEntryFile != null
                && VfsUtil.isAncestor(contentEntryFile, excludedDir, false);
    }
}
