package org.openclover.eclipse.core.views.widgets;

import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.views.ColumnCollectionSettings;
import org.openclover.eclipse.core.views.ColumnDefinition;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

public abstract class SelectionAwareCellRenderer extends BaseListeningRenderer {
    public SelectionAwareCellRenderer(ColumnCollectionSettings columnSettings, ColumnDefinition column, Composite rendered) {
        super(rendered, columnSettings, column);
    }

    protected void renderText(String text, GC gc, Event event, Display display, int alignment) {
        drawText(text, gc, display, event, calcTargetColumnCurrentWidth(event), alignment);
    }

    @Override
    protected void paint(Event event) {}

    @Override
    protected void erase(Event event) {
        captureDetailsEvent(event);

        if (forThisColumn(event)) {
            if ((event.detail & SWT.SELECTED) != 0 || forSelection(event)) {
                SwtUtils.renderSelectionBackground(event);
                event.detail &= ~SWT.SELECTED;
                event.detail &= ~SwtUtils.SWT_HOT;
            }

            event.detail &= ~SWT.BACKGROUND;
        }
    }

    protected void captureDetailsEvent(Event event) {
        lastRowHeight = event.height;
        lastRowX = event.x;
        lastRowY = event.y;
    }

    protected void drawText(String text, GC gc, Display display, Event event, int columnWidth, int alignment, Color foreground) {
        SwtUtils.drawText(text, gc, foreground, lastRowX, lastRowY, columnWidth, lastRowHeight, MARGIN, alignment);
    }

    protected void drawText(String text, GC gc, Display display, Event event, int columnWidth, int alignment) {
        final Color foreground =
            ((event.detail & SWT.SELECTED) != 0 || forSelection(event))
                ? display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT)
                : display.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        drawText(text, gc, display, event, columnWidth, alignment, foreground);
    }
}
