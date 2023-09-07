package com.atlassian.clover.idea;

import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.reporters.filters.DefaultTestFilter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.MalformedURLException;

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
        return virtualFile != null && ProjectRootManager.getInstance(project).getFileIndex().isInTestSourceContent(virtualFile);
    }

    @Override
    public boolean accept(HasMetrics hasMetrics) {
        if (hasMetrics instanceof FullClassInfo) {
            final FullClassInfo classInfo = (FullClassInfo) hasMetrics;
            final File file = ((FullFileInfo) classInfo.getContainingFile()).getPhysicalFile();
            if (isInTestFolder(file)) {
                // file is in test folder - definitely test class
                return !isInverted();
            }
        }
        // not a class in a test folder - apply default logic 
        return super.accept(hasMetrics);
    }
}
