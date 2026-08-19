package org.openclover.core.reporters.pdf;

import org.jfree.chart.JFreeChart;
import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfRect;
import org.openclover.core.reporters.pdf.api.PdfWidget;

import java.awt.geom.Rectangle2D;

/**
 * Embeds a JFreeChart graph into the historical report, by letting the chart paint itself into an
 * AWT drawing context that writes PDF drawing operations.
 */
public class ChartWidget implements PdfWidget {

    private final JFreeChart chart;
    private final double height;

    public ChartWidget(JFreeChart chart, double height) {
        this.chart = chart;
        this.height = height;
    }

    @Override
    public double preferredHeight() {
        return height;
    }

    @Override
    public void draw(PdfCanvas canvas, PdfRect bounds) {
        // the chart paints in its own coordinates, starting at the origin of the scope's context
        canvas.inGraphicsScope(bounds, graphics -> chart.draw(graphics,
                new Rectangle2D.Double(0, 0, bounds.getWidth(), bounds.getHeight())));
    }
}
