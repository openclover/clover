package org.openclover.idea.report.jfc;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.idea.build.InclusionDetector;
import org.openclover.idea.build.ProjectInclusionDetector;

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
            return ApplicationManager.getApplication()
                    .runReadAction((Computable<InclusionDetector>) () ->
                            ProjectInclusionDetector.processFile(project, vf))
                    .isIncluded();
        }
        return true;
    }
}
