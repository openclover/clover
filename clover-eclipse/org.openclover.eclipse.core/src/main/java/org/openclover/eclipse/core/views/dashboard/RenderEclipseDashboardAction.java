package org.openclover.eclipse.core.views.dashboard;

import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.html.RenderDashboardAction;
import org.openclover.core.reporters.html.VelocityContextBuilder;
import org.openclover.core.reporters.util.CloverChartFactory.ChartInfo;

import java.io.File;

public class RenderEclipseDashboardAction extends RenderDashboardAction {

    public RenderEclipseDashboardAction(VelocityContextBuilder contextFactory, File basePath,
                                        ProjectInfo configured, ProjectInfo full, ChartInfo histogram,
                                        ChartInfo scatter, Current currentConfig) {
        super(contextFactory, basePath, configured, full, histogram, scatter, currentConfig);
    }
    
    public void applyCtxChanges() throws Exception {
        super.insertDashboardProperties();
    }

}
