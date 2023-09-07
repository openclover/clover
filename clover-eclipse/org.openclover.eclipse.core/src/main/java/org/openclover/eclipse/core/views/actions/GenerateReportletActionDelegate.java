package org.openclover.eclipse.core.views.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import com.atlassian.clover.api.registry.HasMetrics;
import org.openclover.eclipse.core.projects.model.MetricsScope;

/**
 *
 */
public class GenerateReportletActionDelegate extends SingleCloverProjectActionDelegate {
    @Override
    protected boolean enableFor(IProject project) throws CoreException {
        HasMetrics hasMetrics = MetricsScope.APP_ONLY.getHasMetricsFor(project);
        return
            super.enableFor(project)
            && hasMetrics != null;
    }
}
