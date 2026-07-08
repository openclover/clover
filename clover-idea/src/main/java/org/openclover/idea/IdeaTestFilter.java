package org.openclover.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.reporters.filters.DefaultTestFilter;

import java.io.File;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class IdeaTestFilter extends DefaultTestFilter {
    protected final Project project;

    protected IdeaTestFilter(boolean inverted, @NotNull Project project) {
        super(inverted);
        this.project = project;
    }

    public IdeaTestFilter(@NotNull Project project) {
        this(false, project);
    }

    @Override
    public IdeaTestFilter invert() {
        return new IdeaTestFilter(!isInverted(), project);
    }

    protected boolean isInTestFolder(File file) {
        final VirtualFile virtualFile;
        try {
            virtualFile = VfsUtil.findFileByURL(file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot convert File to VirtualFile (" + file + ")", e);
        }
        if (virtualFile == null) {
            return false;
        }

        // the file-index query touches the workspace model; since IDEA 2026 this is invoked from a
        // background pooled thread (database reload) and requires read access, so run it in a read action
        final AtomicBoolean inTestFolder = new AtomicBoolean();
        ApplicationManager.getApplication().runReadAction(() ->
                inTestFolder.set(ProjectRootManager.getInstance(project).getFileIndex().isInTestSourceContent(virtualFile)));
        return inTestFolder.get();
    }

    @Override
    public boolean accept(HasMetrics hasMetrics) {
        if (hasMetrics instanceof ClassInfo) {
            final ClassInfo classInfo = (ClassInfo) hasMetrics;
            final File file = classInfo.getContainingFile().getPhysicalFile();
            if (isInTestFolder(file)) {
                // file is in test folder - definitely test class
                return !isInverted();
            }
        }
        // not a class in a test folder - apply default logic 
        return super.accept(hasMetrics);
    }
}
