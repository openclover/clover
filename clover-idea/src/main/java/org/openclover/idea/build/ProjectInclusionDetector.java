package org.openclover.idea.build;

import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.build.InclusionDetector;
import org.openclover.idea.util.InclusionUtil;
import org.openclover.idea.CloverModuleComponent;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.util.vfs.VfsUtil;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ProjectInclusionDetector implements InclusionDetector {
    private boolean isIncluded;
    private boolean cloverDisabled;
    private boolean includeNotJava;
    private boolean notJava;
    private boolean moduleNotFound;
    private boolean moduleExcluded;
    private boolean pathNotFound;
    private boolean patternExcluded;
    private boolean inTestSources;


    public static InclusionDetector processFile(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(project);
        return processFile(project, virtualFile, false, plugin.getConfig());
    }

    public static InclusionDetector processFileOrDir(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(project);
        return processFile(project, virtualFile, true, plugin.getConfig());
    }


    static InclusionDetector processFile(@NotNull Project project, VirtualFile virtualFile, boolean ignoreType, @NotNull IdeaCloverConfig config) {
        final ProjectInclusionDetector detector = new ProjectInclusionDetector();
        detector.includeNotJava = ignoreType;

        if (!config.isEnabled() || !config.isBuildWithClover()) {
            detector.cloverDisabled = true;
            // let it pass through when this detector is used for reading the existing db, check isCloverDisabled() when instrumenting
        }

        // a) only want to deal with java source files
        if (!FileTypeManager.getInstance().getFileTypeByFile(virtualFile).equals(StdFileTypes.JAVA)) {
            detector.notJava = true;
            if (!ignoreType) {
                return detector;
            }
        }

        final Module module = ModuleUtil.findModuleForFile(virtualFile, project);
        if (module == null) {
            detector.moduleNotFound = true;
        } else {
            final CloverModuleComponent c = CloverModuleComponent.getInstance(module);
            if (c != null && c.getConfig().isExcluded()) {
                detector.moduleExcluded = true;
                return detector;
            }
        }

        final String javaPath = VfsUtil.getRootRelativeFilename(project, virtualFile);
        if (javaPath == null) {
            detector.pathNotFound = true;
            return detector;
        }
        if (!InclusionUtil.isIncluded(javaPath, InclusionUtil.toArray(config.getExcludes(), " ,"), InclusionUtil.toArray(config.getIncludes(), " ,"), true)) {
            detector.patternExcluded = true;
            return detector;
        }

        if (!config.isInstrumentTests()) {
            ModuleRootManager rManager = ModuleRootManager.getInstance(module);
            if (rManager.getFileIndex().isInTestSourceContent(virtualFile)) {
                detector.inTestSources = true;
                return detector;
            }
        }

        detector.isIncluded = true;
        return detector;
    }

    @Override
    public boolean isIncluded() {
        return isIncluded;
    }

    @Override
    public boolean isModuleExcluded() {
        return moduleExcluded;
    }

    /**
     * The file may be included because it's path was not found - such files are included by default
     * @return path was not found so file was included by default
     */
    @Override
    public boolean isPathNotFound() {
        return pathNotFound;
    }

    @Override
    public boolean isPatternExcluded() {
        return patternExcluded;
    }

    @Override
    public boolean isInNoninstrumentedTestSources() {
        return inTestSources;
    }

    @Override
    public boolean isCloverDisabled() {
        return cloverDisabled;
    }

    @Override
    public boolean isNotJava() {
        return notJava;
    }

    @Override
    public boolean isModuleNotFound() {
        return moduleNotFound;
    }
}
