package com.atlassian.clover.idea.report.jfc;

import com.atlassian.clover.idea.build.InclusionDetector;
import com.atlassian.clover.idea.build.ProjectInclusionDetector;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;

import java.io.File;

public class FileFilter implements HasMetricsFilter {
    private final Project project;

    public FileFilter(Project project) {
        this.project = project;
    }

    @Override
    public boolean accept(HasMetrics hm) {
        if (hm instanceof FullFileInfo) {
            final File ioFile = ((FullFileInfo)hm).getPhysicalFile();
            if (ioFile == null) {
                return true;
            }
            final VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(ioFile);
            if (vf == null) {
                return false;
            }
            return ApplicationManager.getApplication().runReadAction(new Computable<InclusionDetector>() {
                @Override
                public InclusionDetector compute() {
                    return ProjectInclusionDetector.processFile(project, vf);
                }
            }).isIncluded();
        }
        return true;
    }
}
