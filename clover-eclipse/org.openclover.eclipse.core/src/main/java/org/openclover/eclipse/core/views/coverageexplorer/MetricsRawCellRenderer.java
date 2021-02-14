package org.openclover.eclipse.core.views.coverageexplorer;

import org.openclover.eclipse.core.views.widgets.SelectionAwareCellRenderer;
import org.openclover.eclipse.core.views.ExplorerViewSettings;
import org.openclover.eclipse.core.views.ColumnCollectionSettings;
import org.openclover.eclipse.core.views.NumericColumnDefinition;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.graphics.GC;

public abstract class MetricsRawCellRenderer extends SelectionAwareCellRenderer {
    protected ExplorerViewSettings viewSettings;

    public MetricsRawCellRenderer(Tree tree, ExplorerViewSettings viewSettings, ColumnCollectionSettings columnSettings, NumericColumnDefinition column) {
        super(columnSettings, (ColumnDefinition) column, tree);
        this.viewSettings = viewSettings;
    }

    @Override
    protected void paint(Event event) {
        if (forThisColumn(event)) {
            Widget item = (Widget) event.item;
            GC gc = event.gc;
            Display display = rendered.getDisplay();

            String valueTest = formatValue(item == null ? null : item.getData());
            renderText(valueTest, gc, event, display, alignmentFor(item == null ? null : item.getData()));
        }
    }

    private int alignmentFor(Object data) {
        return ((ColumnDefinition)column).getAlignment();
    }

    protected abstract String formatValue(Object item);

    protected double valueFor(Object data) {
        return
            ((NumericColumnDefinition)column).getValue(
                viewSettings.getMetricsScope(),
                data).doubleValue();
    }
}
