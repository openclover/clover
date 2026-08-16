package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.apache.pdfbox.pdmodel.PDDocument
import org.openclover.core.reporters.pdf.RecordingCanvas
import org.openclover.core.reporters.pdf.api.PdfAlign
import org.openclover.core.reporters.pdf.api.PdfBorder
import org.openclover.core.reporters.pdf.api.PdfCanvas
import org.openclover.core.reporters.pdf.api.PdfFontSpec
import org.openclover.core.reporters.pdf.api.PdfRect
import org.openclover.core.reporters.pdf.api.PdfTable
import org.openclover.core.reporters.pdf.api.PdfText
import org.openclover.core.reporters.pdf.api.PdfWidget

import java.awt.Color

/**
 * How a cell's style reaches the page: its borders, its background, and where the content ends up
 * inside it once alignment has had its say.
 */
class TableCellStyleRenderingTest extends TestCase {

    private static final double CONTENT_WIDTH = 500d
    private static final double PADDING = 2d
    private static final PdfFontSpec FONT = PdfFontSpec.sans(10)

    private PDDocument document
    private TableRenderer renderer
    private RecordingCanvas canvas

    void setUp() {
        document = new PDDocument()
        renderer = new TableRenderer(new TextLayouter(new FontRegistry(document)))
        canvas = new RecordingCanvas()
    }

    void tearDown() {
        document.close()
    }

    private static PdfTable newTable(int columns, int[] widths = null) {
        PdfTable table = new PdfTable(columns)
        table.setWidthPercentage(100)
        if (widths != null) {
            table.setWidths(widths)
        }
        table.getDefaultStyle().setBorders(PdfBorder.NONE).setPadding(PADDING)
        return table
    }

    void testBoxBordersAreStrokedAsOneRectangle() {
        PdfTable table = newTable(1)
        table.getDefaultStyle().setBorders(PdfBorder.BOX).setBorderColour(Color.RED)
        table.addCell(PdfText.of("boxed", FONT))

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        assertEquals(1, canvas.rects.size())
        assertFalse(canvas.rects[0].filled)
        assertEquals(Color.RED, canvas.rects[0].colour)
        assertTrue("a box should not be drawn edge by edge", canvas.lines.isEmpty())
    }

    void testPartialBordersAreDrawnEdgeByEdge() {
        PdfTable table = newTable(1)
        table.getDefaultStyle().setBorders(PdfBorder.TOP, PdfBorder.BOTTOM)
        table.addCell(PdfText.of("banded", FONT))

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        assertEquals(2, canvas.lines.size())
        assertTrue(canvas.rects.every { it.filled })
        // both edges span the full cell width, one at its top and one at its bottom
        canvas.lines.each { assertEquals(CONTENT_WIDTH, it.x2 - it.x1, 0.001d) }
        assertEquals(100d, canvas.lines[0].y1, 0.001d)
        assertEquals(100d - (10d + 2 * PADDING), canvas.lines[1].y1, 0.001d)
    }

    void testBorderlessCellsDrawNoBorderAtAll() {
        PdfTable table = newTable(1)
        table.addCell(PdfText.of("plain", FONT))

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        assertTrue(canvas.lines.isEmpty())
        assertTrue(canvas.rects.isEmpty())
    }

    void testBackgroundIsFilledBehindTheCell() {
        PdfTable table = newTable(1)
        table.getDefaultStyle().setBackgroundColour(Color.BLUE)
        table.addCell(PdfText.of("filled", FONT))

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        RecordingCanvas.DrawnRect background = canvas.filled()[0]
        assertNotNull(background)
        assertEquals(Color.BLUE, background.colour)
        assertEquals(CONTENT_WIDTH, background.rect.width, 0.001d)
        assertEquals(10d + 2 * PADDING, background.rect.height, 0.001d)
    }

    /**
     * The row is as tall as the text beside the widget, so a middle-aligned widget sits centred in
     * the space left over rather than at the top of it.
     */
    void testMiddleAlignmentCentresContentInItsCell() {
        PdfWidget shortWidget = new PdfWidget() {
            @Override
            double preferredHeight() {
                return 4d
            }

            @Override
            void draw(PdfCanvas canvas, PdfRect bounds) {
                canvas.fillRect(bounds, Color.GREEN)
            }
        }

        PdfTable table = newTable(2, [50, 50] as int[])
        table.getDefaultStyle().setVerticalAlignment(PdfAlign.Vertical.MIDDLE)
        table.addCell(PdfText.of("first\nsecond", FONT))
        table.addCell(shortWidget)

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        double rowHeight = 20d + 2 * PADDING
        double rowBottom = 100d - rowHeight
        PdfRect widgetBounds = canvas.filled()[0].rect
        // equal gaps above and below the 4pt widget inside the padded cell
        assertEquals(rowBottom + PADDING + (20d - 4d) / 2, widgetBounds.y, 0.001d)
    }

    /**
     * The cell is taller than its content, so the vertical alignment decides where in the cell the
     * content sits. All three settings are honoured; before the measured layout existed, BOTTOM
     * quietly behaved like TOP.
     */
    void testVerticalAlignmentPlacesContentInsideATallCell() {
        Closure<Double> topOfContent = { PdfAlign.Vertical alignment ->
            RecordingCanvas own = new RecordingCanvas()
            PdfTable table = newTable(1)
            table.getDefaultStyle().setMinimumHeight(100d).setVerticalAlignment(alignment)
            table.addCell(PdfText.of("x", FONT))

            renderer.drawTable(own, table, 0d, 100d, CONTENT_WIDTH)
            return own.texts[0].bounds.top
        }

        double top = topOfContent(PdfAlign.Vertical.TOP)
        double middle = topOfContent(PdfAlign.Vertical.MIDDLE)
        double bottom = topOfContent(PdfAlign.Vertical.BOTTOM)

        assertTrue("TOP should sit highest, got ${top} vs ${middle}", top > middle)
        assertTrue("BOTTOM should sit lowest, got ${middle} vs ${bottom}", middle > bottom)

        // the row is 100pt tall and drawn from y=100 down, so its bottom edge sits at y=0
        assertEquals("inset from the top edge by the padding", 100d - PADDING, top, 0.001d)
        assertEquals("inset from the bottom edge by the padding",
                PADDING + heightOfOneLine(), bottom, 0.001d)
    }

    /** @return the height one line of the test font occupies, as the renderer measures it */
    private double heightOfOneLine() {
        PdfTable table = newTable(1)
        table.addCell(PdfText.of("x", FONT))
        return renderer.layout(table, CONTENT_WIDTH, false).rows[0].cells[0].contentHeight
    }

    void testHorizontalAlignmentIsPassedOnToTheCanvas() {
        PdfTable table = newTable(1)
        table.getDefaultStyle().setHorizontalAlignment(PdfAlign.Horizontal.CENTER)
        table.addCell(PdfText.of("centred", FONT))

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        assertEquals(PdfAlign.Horizontal.CENTER, canvas.texts[0].horizontal)
    }
}
