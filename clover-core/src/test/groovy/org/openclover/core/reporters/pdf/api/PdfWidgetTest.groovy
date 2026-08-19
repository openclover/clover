package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase
import org.openclover.core.reporters.pdf.RecordingCanvas

/**
 * A widget only has to say how tall it wants to be and how to paint itself; the
 * {@link PdfCellContent} contract the layout engine works against is satisfied by default.
 */
class PdfWidgetTest extends TestCase {

    private static class SizedWidget implements PdfWidget {

        PdfCanvas paintedOn
        PdfRect paintedIn

        @Override
        double preferredHeight() {
            return 12d
        }

        @Override
        void draw(PdfCanvas canvas, PdfRect bounds) {
            paintedOn = canvas
            paintedIn = bounds
        }
    }

    private static PdfMeasuredContent measure(PdfWidget widget, double width) {
        return widget.measure(null, PdfCellStyle.builder().build(), width)
    }

    void testHeightIsThePreferredHeightWhateverTheWidth() {
        SizedWidget widget = new SizedWidget()

        assertEquals(12d, measure(widget, 500d).height, 0.001d)
        assertEquals(12d, measure(widget, 1d).height, 0.001d)
    }

    void testDrawingIsForwardedToTheCanvasAndRectangle() {
        SizedWidget widget = new SizedWidget()
        RecordingCanvas canvas = new RecordingCanvas()
        PdfRect bounds = new PdfRect(1d, 2d, 3d, 4d)

        measure(widget, 3d).draw(canvas, bounds)

        assertSame(canvas, widget.paintedOn)
        assertSame(bounds, widget.paintedIn)
    }
}
