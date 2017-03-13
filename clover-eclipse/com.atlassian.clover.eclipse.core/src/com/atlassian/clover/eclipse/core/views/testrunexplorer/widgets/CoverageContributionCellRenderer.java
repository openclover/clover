package com.atlassian.clover.eclipse.core.views.testrunexplorer.widgets;

import com.atlassian.clover.util.MetricsFormatUtils;
import com.atlassian.clover.eclipse.core.views.testrunexplorer.nodes.CoverageContributionNode;
import com.atlassian.clover.eclipse.core.views.widgets.HistogramCellRenderer;
import com.atlassian.clover.eclipse.core.views.ColumnCollectionSettings;
import com.atlassian.clover.eclipse.core.views.ColumnDefinition;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

public class CoverageContributionCellRenderer extends HistogramCellRenderer {
    public CoverageContributionCellRenderer(Tree tree, ColumnCollectionSettings settings, ColumnDefinition column) {
        super(tree, settings, column);
    }

    protected String getEmptyLabel() {
        return MetricsFormatUtils.NO_METRICS_LABEL;
    }

    @Override
    protected String percentLabelFor(Object data) {
        if (data instanceof CoverageContributionNode) {
            return MetricsFormatUtils.formatMetricsPercent(selectValue((CoverageContributionNode)data));
        } else {
            return getEmptyLabel();
        }
    }

    protected float selectValue(CoverageContributionNode classCoverageByTest) {
        return classCoverageByTest.getCoverage();
    }

    @Override
    protected double percentValueFor(Object data) {
        if (data instanceof CoverageContributionNode) {
            return selectValue((CoverageContributionNode)data);
        } else {
            return getEmptyValue();
        }
    }

    @Override
    protected Color allocateEmptyBarColour(Display display) {
        return display.getSystemColor(SWT.COLOR_WHITE);
    }

    @Override
    protected Color allocateFullBarColour(Display display) {
        return new Color(display, 0x87, 0xce, 0xfa);
    }

    @Override
    protected void disposeFullBarColour(Color color) {
        color.dispose();
    }

    @Override
    protected double getEmptyValue() {
        return -1.0d;
    }
}
