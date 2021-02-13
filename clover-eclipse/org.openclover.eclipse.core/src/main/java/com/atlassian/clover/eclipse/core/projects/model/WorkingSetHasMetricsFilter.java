package com.atlassian.clover.eclipse.core.projects.model;

import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import org.eclipse.core.runtime.IAdaptable;

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
