package org.openclover.core.reporters.pdf

import junit.framework.TestCase
import org.jfree.chart.ChartFactory
import org.jfree.chart.JFreeChart
import org.jfree.data.category.DefaultCategoryDataset
import org.openclover.core.reporters.pdf.api.PdfRect

/**
 * The historical charts, which paint themselves through an AWT context mapped onto the page.
 */
class ChartWidgetTest extends TestCase {

    private static JFreeChart newChart() {
        DefaultCategoryDataset data = new DefaultCategoryDataset()
        data.addValue(50d, "coverage", "build 1")
        data.addValue(75d, "coverage", "build 2")
        return ChartFactory.createLineChart("Coverage", "build", "%", data)
    }

    void testHeightIsWhateverTheChartWasConfiguredWith() {
        assertEquals(180d, new ChartWidget(newChart(), 180d).preferredHeight(), 0.001d)
    }

    void testTheChartIsPaintedIntoTheRectangleItWasGiven() {
        RecordingCanvas canvas = new RecordingCanvas()
        PdfRect bounds = new PdfRect(30d, 400d, 500d, 180d)

        new ChartWidget(newChart(), 180d).draw(canvas, bounds)

        assertEquals(1, canvas.graphicsScopes.size())
        assertSame("the AWT context is mapped onto the widget's rectangle",
                bounds, canvas.graphicsScopes[0].bounds)
    }

    /**
     * The drawing is only stamped onto the page when the scope closes, so the widget must close it
     * however the chart's own painting turns out.
     */
    void testTheGraphicsScopeIsAlwaysClosed() {
        RecordingCanvas canvas = new RecordingCanvas()

        new ChartWidget(newChart(), 180d).draw(canvas, new PdfRect(0d, 0d, 500d, 180d))

        assertTrue(canvas.graphicsScopes[0].closed)
    }

    void testTheScopeIsClosedEvenWhenTheChartFails() {
        RecordingCanvas canvas = new RecordingCanvas()
        JFreeChart exploding = new JFreeChart(newChart().getPlot()) {
            @Override
            void draw(java.awt.Graphics2D g2, java.awt.geom.Rectangle2D area) {
                throw new IllegalStateException("chart rendering failed")
            }
        }

        try {
            new ChartWidget(exploding, 180d).draw(canvas, new PdfRect(0d, 0d, 500d, 180d))
            fail("expected the failure to propagate")
        } catch (IllegalStateException expected) {
            assertEquals("chart rendering failed", expected.message)
        }
        assertTrue("the scope must not be left open", canvas.graphicsScopes[0].closed)
    }
}
