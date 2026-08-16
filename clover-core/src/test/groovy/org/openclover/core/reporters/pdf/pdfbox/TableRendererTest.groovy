package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.apache.pdfbox.pdmodel.PDDocument
import org.openclover.core.reporters.pdf.api.PdfAlign
import org.openclover.core.reporters.pdf.api.PdfBorder
import org.openclover.core.reporters.pdf.api.PdfCanvas
import org.openclover.core.reporters.pdf.api.PdfCellStyle
import org.openclover.core.reporters.pdf.api.PdfFontSpec
import org.openclover.core.reporters.pdf.api.PdfLayout
import org.openclover.core.reporters.pdf.api.PdfRect
import org.openclover.core.reporters.pdf.api.PdfTable
import org.openclover.core.reporters.pdf.api.PdfText
import org.openclover.core.reporters.pdf.api.PdfWidget

import java.awt.Color

/**
 * Geometry of the table layout engine: how column widths are resolved, how tall rows come out and
 * where cells, borders and content end up. Assertions are made against a canvas that records what
 * it was asked to draw, so no PDF is produced.
 */
class TableRendererTest extends TestCase {

    private static final double CONTENT_WIDTH = 500d
    private static final double PADDING = 2d
    private static final PdfFontSpec FONT = PdfFontSpec.sans(10)

    /** A rectangle the renderer asked to be filled or stroked. */
    private static class DrawnRect {
        PdfRect rect
        Color colour
        boolean filled
    }

    private static class DrawnLine {
        double x1, y1, x2, y2
    }

    private static class DrawnText {
        PdfText text
        PdfRect bounds
        PdfAlign.Horizontal alignment
    }

    /**
     * Records drawing calls instead of writing them to a page. Only the methods the table renderer
     * uses need to do anything.
     */
    private static class RecordingCanvas implements PdfCanvas {
        List<DrawnRect> rects = []
        List<DrawnLine> lines = []
        List<DrawnText> texts = []

        @Override
        void setLineWidth(double width) {
        }

        @Override
        void fillRect(PdfRect rect, Color colour) {
            rects.add(new DrawnRect(rect: rect, colour: colour, filled: true))
        }

        @Override
        void strokeRect(PdfRect rect, Color colour) {
            rects.add(new DrawnRect(rect: rect, colour: colour, filled: false))
        }

        @Override
        void drawLine(double x1, double y1, double x2, double y2, Color colour) {
            lines.add(new DrawnLine(x1: x1, y1: y1, x2: x2, y2: y2))
        }

        @Override
        void drawImage(String resourcePath, PdfRect bounds) {
        }

        @Override
        void drawText(PdfText text, PdfRect bounds, PdfAlign.Horizontal alignment) {
            texts.add(new DrawnText(text: text, bounds: bounds, alignment: alignment))
        }

        @Override
        void drawText(PdfText text, PdfRect bounds, PdfAlign.Horizontal horizontal,
                      PdfAlign.Vertical vertical, double fixedLeading, double multipliedLeading) {
            texts.add(new DrawnText(text: text, bounds: bounds, alignment: horizontal))
        }

        @Override
        PdfCanvas.GraphicsScope beginGraphics(PdfRect bounds) {
            throw new UnsupportedOperationException()
        }
    }

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

    void testColumnWidthsFollowTheirRelativeProportions() {
        PdfTable table = newTable(3, [50, 30, 20] as int[])
        table.addCell(PdfText.of("a", FONT))
        table.addCell(PdfText.of("b", FONT))
        table.addCell(PdfText.of("c", FONT))

        double[] widths = renderer.layout(table, CONTENT_WIDTH, false).getColumnWidths()

        assertEquals(250d, widths[0], 0.001d)
        assertEquals(150d, widths[1], 0.001d)
        assertEquals(100d, widths[2], 0.001d)
    }

    /**
     * Proportions are relative, so they need not add up to any particular total.
     */
    void testColumnWidthsAreNormalised() {
        PdfTable table = newTable(2, [3, 1] as int[])
        table.addCell(PdfText.of("a", FONT))
        table.addCell(PdfText.of("b", FONT))

        double[] widths = renderer.layout(table, CONTENT_WIDTH, false).getColumnWidths()

        assertEquals(375d, widths[0], 0.001d)
        assertEquals(125d, widths[1], 0.001d)
    }

    void testWidthPercentageAppliesToTheAvailableWidth() {
        PdfTable table = new PdfTable(1)
        table.setWidthPercentage(50)
        table.addCell(PdfText.of("a", FONT))

        assertEquals(CONTENT_WIDTH / 2, renderer.layout(table, CONTENT_WIDTH, false).getColumnWidths()[0], 0.001d)
    }

    void testASpannedCellCoversTheWidthOfItsColumns() {
        PdfTable table = newTable(3, [50, 30, 20] as int[])
        table.getDefaultStyle().setColspan(2)
        table.addCell(PdfText.of("wide", FONT))
        table.getDefaultStyle().setColspan(1)
        table.addCell(PdfText.of("narrow", FONT))

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        // one row, two cells; the first spans the 250pt and 150pt columns
        assertEquals(2, canvas.texts.size())
        assertEquals(400d - 2 * PADDING, canvas.texts[0].bounds.width, 0.001d)
        assertEquals(100d - 2 * PADDING, canvas.texts[1].bounds.width, 0.001d)
        assertEquals(400d + PADDING, canvas.texts[1].bounds.x, 0.001d)
    }

    void testRowTakesTheHeightOfItsTallestCell() {
        PdfTable table = newTable(2, [50, 50] as int[])
        table.addCell(PdfText.of("one line", FONT))
        table.addCell(PdfText.of("first\nsecond\nthird", FONT))

        List<TableLayout.Row> rows = renderer.layout(table, CONTENT_WIDTH, false).getRows()

        assertEquals(1, rows.size())
        // three lines of 10pt leading, plus padding above and below
        assertEquals(30d + 2 * PADDING, rows[0].getHeight(), 0.001d)
    }

    void testMinimumHeightWinsOverContentHeight() {
        PdfTable table = newTable(1)
        table.getDefaultStyle().setMinimumHeight(40d)
        table.addCell(PdfText.of("short", FONT))

        assertEquals(40d, renderer.layout(table, CONTENT_WIDTH, false).getRows()[0].getHeight(), 0.001d)
    }

    void testTableHeightIsTheSumOfItsRows() {
        PdfTable table = newTable(1)
        3.times { table.addCell(PdfText.of("row", FONT)) }

        TableLayout layout = renderer.layout(table, CONTENT_WIDTH, false)

        assertEquals(3, layout.getRows().size())
        assertEquals(3 * (10d + 2 * PADDING), layout.getHeight(), 0.001d)
    }

    /**
     * A nested table is measured against the width of the cell holding it, and the row it sits in
     * grows to fit it.
     */
    void testNestedTableIsMeasuredAgainstItsCell() {
        PdfTable inner = new PdfTable(1)
        inner.getDefaultStyle().setBorders(PdfBorder.NONE).setPadding(PADDING)
        2.times { inner.addCell(PdfText.of("inner", FONT)) }

        PdfTable outer = newTable(2, [50, 50] as int[])
        outer.addCell(PdfText.of("outer", FONT))
        outer.addCell(inner)

        TableLayout layout = renderer.layout(outer, CONTENT_WIDTH, false)
        double innerHeight = 2 * (10d + 2 * PADDING)

        assertEquals(innerHeight + 2 * PADDING, layout.getRows()[0].getHeight(), 0.001d)

        renderer.drawTable(canvas, outer, 0d, 100d, CONTENT_WIDTH)
        // the nested table fills its cell, so its text is laid out across half the outer table
        // less the padding of both the outer and the inner cell
        assertEquals(250d - 4 * PADDING, canvas.texts[1].bounds.width, 0.001d)
    }

    void testWidgetsAreHandedTheirResolvedRectangle() {
        PdfRect drawn = null
        PdfWidget widget = new PdfWidget() {
            @Override
            double preferredHeight() {
                return 8d
            }

            @Override
            void draw(PdfCanvas canvas, PdfRect bounds) {
                drawn = bounds
            }
        }

        PdfTable table = newTable(2, [50, 50] as int[])
        table.addCell(PdfText.of("coverage", FONT))
        table.addCell(widget)

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        assertNotNull("the widget should have been drawn", drawn)
        assertEquals(8d, drawn.height, 0.001d)
        assertEquals(250d - 2 * PADDING, drawn.width, 0.001d)
        assertEquals(250d + PADDING, drawn.x, 0.001d)
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
        PdfRect widgetBounds = canvas.rects.find { it.filled }.rect
        // equal gaps above and below the 4pt widget inside the padded cell
        assertEquals(rowBottom + PADDING + (20d - 4d) / 2, widgetBounds.y, 0.001d)
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

        DrawnRect background = canvas.rects.find { it.filled }
        assertNotNull(background)
        assertEquals(Color.BLUE, background.colour)
        assertEquals(CONTENT_WIDTH, background.rect.width, 0.001d)
        assertEquals(10d + 2 * PADDING, background.rect.height, 0.001d)
    }

    void testRowsAreStackedDownwards() {
        PdfTable table = newTable(1)
        table.getDefaultStyle().setBackgroundColour(Color.WHITE)
        3.times { table.addCell(PdfText.of("row ${it}", FONT)) }

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        double rowHeight = 10d + 2 * PADDING
        assertEquals(3, canvas.rects.size())
        assertEquals(100d - rowHeight, canvas.rects[0].rect.y, 0.001d)
        assertEquals(100d - 2 * rowHeight, canvas.rects[1].rect.y, 0.001d)
        assertEquals(100d - 3 * rowHeight, canvas.rects[2].rect.y, 0.001d)
    }

    /**
     * An empty cell still occupies its column, so the cells after it stay in their own columns.
     */
    void testEmptyCellsStillTakeTheirColumn() {
        PdfTable table = newTable(3, [1, 1, 1] as int[])
        table.addCell(PdfText.of("first", FONT))
        table.addCell()
        table.addCell(PdfText.of("third", FONT))

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        assertEquals(2, canvas.texts.size())
        assertEquals(PADDING, canvas.texts[0].bounds.x, 0.001d)
        assertEquals(2 * CONTENT_WIDTH / 3 + PADDING, canvas.texts[1].bounds.x, 0.001d)
    }

    void testHorizontalAlignmentIsPassedOnToTheCanvas() {
        PdfTable table = newTable(1)
        table.getDefaultStyle().setHorizontalAlignment(PdfAlign.Horizontal.CENTER)
        table.addCell(PdfText.of("centred", FONT))

        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        assertEquals(PdfAlign.Horizontal.CENTER, canvas.texts[0].alignment)
    }

    /**
     * Cell content measures itself through the layout it is handed, so a content type the renderer
     * has never heard of lays out correctly all the same.
     */
    void testUnknownContentTypesMeasureAndDrawThemselves() {
        PdfRect drawn = null
        def custom = new org.openclover.core.reporters.pdf.api.PdfCellContent() {
            @Override
            double height(PdfLayout layout, PdfCellStyle style, double contentWidth) {
                return 25d
            }

            @Override
            void draw(PdfLayout layout, PdfCanvas canvas, PdfCellStyle style, PdfRect bounds) {
                drawn = bounds
            }
        }

        PdfTable table = newTable(1)
        table.addCell(custom)

        TableLayout layout = renderer.layout(table, CONTENT_WIDTH, false)
        renderer.drawTable(canvas, table, 0d, 100d, CONTENT_WIDTH)

        assertEquals(25d + 2 * PADDING, layout.getRows()[0].getHeight(), 0.001d)
        assertNotNull(drawn)
        assertEquals(25d, drawn.height, 0.001d)
    }
}
