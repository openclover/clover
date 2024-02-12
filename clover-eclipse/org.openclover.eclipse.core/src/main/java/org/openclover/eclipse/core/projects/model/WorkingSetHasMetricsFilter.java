package org.openclover.eclipse.core.projects.model;

import org.eclipse.core.runtime.IAdaptable;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.metrics.HasMetricsFilter;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

import java.io.File;

public class WorkingSetHasMetricsFilter implements HasMetricsFilter {
    private CloverProject project;
    private File projectLocation;

    public WorkingSetHasMetricsFilter(CloverProject project) {
        this.project = project;
        this.projectLocation = project.getProject().getLocation().toFile();
    }

    @Override
    public boolean accept(HasMetrics metrics) {
        IAdaptable resource = null;

        if (metrics instanceof FullProjectInfo) {
            return CloverPlugin.getInstance().getCloverWorkingSet().includesJavaProject(project.getProject());
        } else if (metrics instanceof FullFileInfo) {
            return
                CloverPlugin.getInstance().getCloverWorkingSet().includesFile(
                    ((FullFileInfo)metrics).getPhysicalFile());
        } else {
            return true;
        }
    }

    private String getPackageName(HasMetrics hasMetrics) {
        return
            PackageInfo.DEFAULT_PACKAGE_NAME.equals(hasMetrics.getName())
                ? ""
                : (hasMetrics.getName() + ".");
    }
}
