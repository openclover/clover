package org.openclover.eclipse.core.views.coverageexplorer;

import org.openclover.eclipse.core.views.ColumnCollectionSettings;
import org.openclover.eclipse.core.views.ExplorerViewSettings;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.openclover.eclipse.core.views.NumericColumnDefinition;
import org.openclover.eclipse.core.views.widgets.HistogramCellRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;

/**
 * Renders the coverage cell in the {@link CoverageView}. Will draw a nice green/red or grey
 * bar on the left (left justified) and add label text on the right (right justified).
 *
 * Minimising the column is handled by letting the coverage bar shrink to the minimum
 * width but no smaller, and ensuring the label is always to the right of the bar.
 */
public class MetricsPcCellRenderer extends HistogramCellRenderer {
    private ExplorerViewSettings viewSettings;

    public MetricsPcCellRenderer(Tree tree, ExplorerViewSettings viewSettings, ColumnCollectionSettings columnSettings, NumericColumnDefinition column) {
        super(tree, columnSettings, (ColumnDefinition) column);
        this.viewSettings = viewSettings;
    }

    @Override
    protected String percentLabelFor(Object data) {
        return
            column.getLabel(
                viewSettings,
                viewSettings.getMetricsScope(),
                null,
                data);
    }

    @Override
    protected double getEmptyValue() {
        return NumericColumnDefinition.NOT_AVAILABLE_DOUBLE.doubleValue();
    }

    @Override
    protected double percentValueFor(Object data) {
        return
            ((NumericColumnDefinition)column).getValue(
                viewSettings.getMetricsScope(),
                data).doubleValue();
    }

    @Override
    protected Color allocateEmptyBarColour(Display display) {
        return display.getSystemColor(SWT.COLOR_RED);
    }

    @Override
    protected Color allocateFullBarColour(Display display) {
        return display.getSystemColor(SWT.COLOR_GREEN);
    }
}
