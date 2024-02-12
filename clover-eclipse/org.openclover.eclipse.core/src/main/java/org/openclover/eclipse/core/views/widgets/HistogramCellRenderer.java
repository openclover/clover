package org.openclover.eclipse.core.views.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;
import org.openclover.core.util.MetricsFormatUtils;
import org.openclover.eclipse.core.views.ColumnCollectionSettings;
import org.openclover.eclipse.core.views.ColumnDefinition;

public abstract class HistogramCellRenderer extends SelectionAwareCellRenderer {
    /** No less than 20 pixels wide */
    protected static final int MIN_BAR_WIDTH = 20;
    /** No less than 20 pixels wide */
    protected static final int PREFERRED_BAR_WIDTH = 35;

    public HistogramCellRenderer(Composite rendered, ColumnCollectionSettings columnSettings, ColumnDefinition column) {
        super(columnSettings, column, rendered);
    }

    @Override
    protected void paint(Event event) {
        if (forThisColumn(event)) {
            Widget item = (Widget) event.item;
            GC gc = event.gc;
            Display display = rendered.getDisplay();

            Color foreground = gc.getForeground();
            Color background = gc.getBackground();
            try {
                double percentage = percentValueFor(item == null ? null : item.getData());
                String text = percentLabelFor(item == null ? null : item.getData());
                if (barCanFitNextToLargestLabel(gc, event)) {
                    if (percentage != getEmptyValue()) {
                        renderBarAndText(
                            text,
                            sanitizePercentage(percentage),
                            gc,
                            event,
                            display);
                    } else {
                        renderEmptyBarAndText(text, gc, event, display);
                    }
                } else {
                    renderText(text, gc, event, display, SWT.RIGHT);
                }
            } finally {
                gc.setForeground(background);
                gc.setBackground(foreground);
            }
        }
    }

    private boolean barCanFitNextToLargestLabel(GC gc, Event event) {
        final int width = calcTargetColumnCurrentWidth(event);
        final int histogramWidth = calculateBarWidth(gc, width);
        final int textWidth = gc.textExtent(getLargestExampleLabel()).x;
        return (width - ((3 * MARGIN) + textWidth + histogramWidth)) >= 0;
    }

    private double sanitizePercentage(double percentage) {
        return Math.max(0.0d, Math.min(1.0d, percentage));
    }

    protected abstract double getEmptyValue();

    protected abstract String percentLabelFor(Object data);

    protected abstract double percentValueFor(Object data);

    protected String getLargestExampleLabel() {
        return MetricsFormatUtils.format100PcCoverage();
    }

    protected int calculateBarWidth(GC gc, int width) {
        Point maxTextSize = gc.textExtent(getLargestExampleLabel());

        return Math.max(
            width - maxTextSize.x - (3 * MARGIN),
            MIN_BAR_WIDTH);
    }

    protected int calculateBarHeight(int height) {
        //Use event height as clipping height may be less than one full cell if cell is only partially visible
        return height - (2 * MARGIN);
    }

    public static int getDefaultWidth() {
        //Without a GC, who knows...
        return (5 * PREFERRED_BAR_WIDTH);
    }

    protected void renderBarAndText(String text, double percent, GC gc, Event event, Display display) {
        int columnWidth = calcTargetColumnCurrentWidth(event);
        int barWidth = calculateBarWidth(gc, columnWidth);
        int barHeight = calculateBarHeight(lastRowHeight);

        Color fullBarColour = allocateFullBarColour(display);
        try {
            Color emptyBarColour = allocateEmptyBarColour(display);
            try {
                gc.setBackground(fullBarColour);
                int width = (int) (barWidth * percent);
                gc.fillRectangle(lastRowX + MARGIN, lastRowY + MARGIN, width, barHeight);

                gc.setBackground(emptyBarColour);
                int uwidth = barWidth - width;
                gc.fillRectangle(lastRowX + MARGIN + width, lastRowY + MARGIN, uwidth, barHeight);

                drawBarOutline(gc, display, barWidth, barHeight);

                if (text != null && text.trim().length() > 0) {
                    drawText(text, gc, display, event, columnWidth, SWT.RIGHT);
                }
            } finally {
                disposeEmptyBarColour(emptyBarColour);
            }
        } finally {
            disposeFullBarColour(fullBarColour);
        }
    }

    private void drawBarOutline(GC gc, Display display, int barWidth, int barHeight) {
        gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
        gc.drawRectangle(lastRowX + MARGIN, lastRowY + MARGIN, barWidth, barHeight);
    }

    protected void renderEmptyBarAndText(String text, GC gc, Event event, Display display) {

        int columnWidth = calcTargetColumnCurrentWidth(event);
        int barWidth = calculateBarWidth(gc, calcTargetColumnCurrentWidth(event));
        int barHeight = calculateBarHeight(lastRowHeight);

        gc.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
        gc.fillRectangle(lastRowX + MARGIN, lastRowY + MARGIN, barWidth, barHeight);

        drawBarOutline(gc, display, barWidth, barHeight);

        drawText(text, gc, display, event, columnWidth, SWT.RIGHT);
    }

    protected abstract Color allocateEmptyBarColour(Display display);

    protected abstract Color allocateFullBarColour(Display display);

    protected void disposeEmptyBarColour(Color color) {
    }

    protected void disposeFullBarColour(Color color) {
    }

}
