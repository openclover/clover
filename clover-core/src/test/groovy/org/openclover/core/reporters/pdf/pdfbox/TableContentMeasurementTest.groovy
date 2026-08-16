package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.apache.pdfbox.pdmodel.PDDocument
import org.openclover.core.reporters.pdf.RecordingCanvas
import org.openclover.core.reporters.pdf.api.PdfBorder
import org.openclover.core.reporters.pdf.api.PdfCanvas
import org.openclover.core.reporters.pdf.api.PdfCellStyle
import org.openclover.core.reporters.pdf.api.PdfFontSpec
import org.openclover.core.reporters.pdf.api.PdfLayout
import org.openclover.core.reporters.pdf.api.PdfMeasuredContent
import org.openclover.core.reporters.pdf.api.PdfRect
import org.openclover.core.reporters.pdf.api.PdfTable
import org.openclover.core.reporters.pdf.api.PdfText
import org.openclover.core.reporters.pdf.api.PdfWidget


/**
 * How cell content is measured and drawn: widgets, nested tables and content types the renderer has
 * never heard of. Measuring produces something drawable, so nothing is measured twice.
 */
class TableContentMeasurementTest extends TestCase {

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
     * Cell content measures itself through the layout it is handed, so a content type the renderer
     * has never heard of lays out correctly all the same.
     */
    void testUnknownContentTypesMeasureAndDrawThemselves() {
        PdfRect drawn = null
        def custom = new org.openclover.core.reporters.pdf.api.PdfCellContent() {
            @Override
            PdfMeasuredContent measure(PdfLayout layout, PdfCellStyle style, double contentWidth) {
                return new PdfMeasuredContent() {

                    @Override
                    double getHeight() {
                        return 25d
                    }

                    @Override
                    void draw(PdfCanvas canvas, PdfRect bounds) {
                        drawn = bounds
                    }
                }
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

    /**
     * Measuring produces something drawable, so a nested table is laid out once however deeply it
     * is nested - the alternative costs 2^depth layouts of the innermost content.
     */
    void testNestedTablesAreLaidOutOnlyOnce() {
        int[] measured = [0]
        def counting = new org.openclover.core.reporters.pdf.api.PdfCellContent() {

            @Override
            PdfMeasuredContent measure(PdfLayout layout, PdfCellStyle style, double contentWidth) {
                measured[0]++
                return new PdfMeasuredContent() {

                    @Override
                    double getHeight() {
                        return 10d
                    }

                    @Override
                    void draw(PdfCanvas canvas, PdfRect bounds) {
                        // nothing to paint
                    }
                }
            }
        }

        PdfTable innermost = newTable(1)
        innermost.addCell(counting)
        PdfTable middle = newTable(1)
        middle.addCell(innermost)
        PdfTable outer = newTable(1)
        outer.addCell(middle)

        renderer.drawTable(canvas, outer, 0d, 500d, CONTENT_WIDTH)

        assertEquals("the innermost content should be measured exactly once", 1, measured[0])
    }

    /**
     * Drawing an already-measured table reuses the measurement rather than starting over, which is
     * what lets the document flow break a table across pages row by row.
     */
    void testAMeasuredTableIsDrawnWithoutRemeasuring() {
        PdfTable table = newTable(2, [50, 50] as int[])
        table.addCell(PdfText.of("left", FONT))
        table.addCell(PdfText.of("right", FONT))

        TableLayout layout = renderer.layout(table, CONTENT_WIDTH, false)
        renderer.draw(canvas, layout, 10d, 400d)

        assertEquals(2, canvas.texts.size())
        assertEquals(10d + PADDING, canvas.texts[0].bounds.x, 0.001d)
        assertEquals(10d + CONTENT_WIDTH / 2 + PADDING, canvas.texts[1].bounds.x, 0.001d)
        assertEquals(400d - PADDING, canvas.texts[0].bounds.top, 0.001d)
    }
}
