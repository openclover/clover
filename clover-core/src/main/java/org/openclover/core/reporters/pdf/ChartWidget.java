package org.openclover.core.reporters.pdf;

import org.jfree.chart.JFreeChart;
import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfRect;
import org.openclover.core.reporters.pdf.api.PdfWidget;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * Embeds a JFreeChart graph into the historical report, by letting the chart paint itself into an
 * AWT drawing context that writes PDF drawing operations.
 */
public class ChartWidget implements PdfWidget {

    private final JFreeChart chart;
    private final float height;

    public ChartWidget(JFreeChart chart, float height) {
        this.chart = chart;
        this.height = height;
    }

    @Override
    public float preferredHeight() {
        return height;
    }

    @Override
    public void draw(PdfCanvas canvas, PdfRect bounds) {
        final Graphics2D graphics = canvas.beginGraphics(bounds);
        try {
            chart.draw(graphics, new Rectangle2D.Float(0, 0, bounds.getWidth(), bounds.getHeight()));
        } finally {
            canvas.endGraphics(graphics);
        }
    }
}
