package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.apache.pdfbox.pdmodel.PDDocument
import org.openclover.core.reporters.pdf.RecordingCanvas
import org.openclover.core.reporters.pdf.api.PdfBorder
import org.openclover.core.reporters.pdf.api.PdfFontSpec
import org.openclover.core.reporters.pdf.api.PdfTable
import org.openclover.core.reporters.pdf.api.PdfText

import java.awt.Color

/**
 * Geometry of the table layout engine: how column widths are resolved from their proportions,
 * how tall rows come out and how they stack down the page. Assertions are made against a canvas
 * that records what it was asked to draw, so no PDF is produced.
 */
class TableGeometryTest extends TestCase {

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
}
