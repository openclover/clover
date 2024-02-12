package org.openclover.eclipse.core.views.dashboard;

import clover.org.apache.velocity.VelocityContext;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.html.RenderDashboardAction;
import org.openclover.core.reporters.util.CloverChartFactory.ChartInfo;

import java.io.File;

public class RenderEclipseDashboardAction extends RenderDashboardAction {

    public RenderEclipseDashboardAction(VelocityContext ctx, File basePath,
            FullProjectInfo configured, FullProjectInfo full, ChartInfo histogram,
            ChartInfo scatter, Current currentConfig) {
        super(ctx, basePath, configured, full, histogram, scatter, currentConfig);
    }
    
    public void applyCtxChanges() throws Exception {
        super.insertDashboardProperties();
    }

}
