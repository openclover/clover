package com.atlassian.clover.eclipse.core.views.dashboard;

import java.io.File;

import clover.org.apache.velocity.VelocityContext;

import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.html.RenderDashboardAction;
import com.atlassian.clover.reporters.util.CloverChartFactory.ChartInfo;

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
