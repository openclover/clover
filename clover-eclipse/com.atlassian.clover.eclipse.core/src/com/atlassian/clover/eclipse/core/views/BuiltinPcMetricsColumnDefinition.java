package com.atlassian.clover.eclipse.core.views;

import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.util.MetricsFormatUtils;
import com.atlassian.clover.eclipse.core.views.widgets.ListeningRenderer;
import com.atlassian.clover.eclipse.core.views.coverageexplorer.MetricsPcCellRenderer;
import com.atlassian.clover.eclipse.core.views.coverageexplorer.CoverageViewSettings;
import com.atlassian.clover.eclipse.core.projects.model.MetricsScope;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

public class BuiltinPcMetricsColumnDefinition extends BuiltinMetricsColumnDefinition {
    public BuiltinPcMetricsColumnDefinition(Column prototype, String abbreviatedTitle, int requiredIndex, int style) {
        super(prototype, abbreviatedTitle, requiredIndex, style);
    }

    @Override
    protected String format(Number value) {
        return MetricsFormatUtils.formatMetricsPercent(Math.max(0.0d, Math.min(1.0d, value.doubleValue()/100)));
    }

    @Override
    public Number getValue(MetricsScope scope, Object object) {
        Number value = super.getValue(scope, object);
        return
            value.equals(NOT_AVAILABLE_DOUBLE)
                ? NOT_AVAILABLE_DOUBLE
                : Double.valueOf(value.doubleValue()/100);
    }

    @Override
    public ListeningRenderer newRenderer(Composite composite, ExplorerViewSettings settings) {
        return new MetricsPcCellRenderer((Tree)composite, (CoverageViewSettings)settings, settings.getTreeColumnSettings(), this);
    }

    @Override
    public boolean displaysSimpleLabel() {
        return false;
    }
}
