package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.openclover.core.reporters.pdf.api.PdfCanvas
import org.openclover.core.reporters.pdf.api.PdfCell
import org.openclover.core.reporters.pdf.api.PdfCellStyle
import org.openclover.core.reporters.pdf.api.PdfMeasuredContent
import org.openclover.core.reporters.pdf.api.PdfRect

/**
 * The measured form of a table, which both the page flow and the renderer read: the flow to decide
 * where a page breaks, the renderer to paint.
 */
class TableLayoutTest extends TestCase {

    private static PdfMeasuredContent content(double height) {
        return new PdfMeasuredContent() {

            @Override
            double getHeight() {
                return height
            }

            @Override
            void draw(PdfCanvas canvas, PdfRect bounds) {
                // nothing to paint in a measurement-only test
            }
        }
    }

    private static TableLayout.Cell cell(double width, double contentHeight) {
        return new TableLayout.Cell(new PdfCell(PdfCellStyle.builder().build(), null), 0d, width,
                content(contentHeight))
    }

    private static TableLayout.Row row(double height) {
        return new TableLayout.Row([cell(10d, height)], height)
    }

    void testHeightIsTheSumOfTheRowHeights() {
        TableLayout layout = new TableLayout(null, [10d, 20d] as double[], 30d,
                [row(5d), row(7d), row(11d)])

        assertEquals(23d, layout.height, 0.001d)
    }

    void testAnEmptyTableHasNoHeight() {
        assertEquals(0d, new TableLayout(null, [10d] as double[], 10d, []).height, 0.001d)
    }

    void testColumnWidthsAndRowsAreReportedAsMeasured() {
        TableLayout.Row single = row(9d)
        TableLayout layout = new TableLayout(null, [30d, 70d] as double[], 100d, [single])

        assertEquals(30d, layout.columnWidths[0], 0.001d)
        assertEquals(70d, layout.columnWidths[1], 0.001d)
        assertEquals(100d, layout.width, 0.001d)
        assertEquals(1, layout.rows.size())
        assertSame(single, layout.rows[0])
        assertEquals(9d, single.height, 0.001d)
        assertEquals(1, single.cells.size())
    }

    /**
     * A measured cell keeps its content's height alongside its own outer height, which is what the
     * renderer places the content with; an empty cell contributes only its padding.
     */
    void testACellReportsBothItsContentHeightAndItsOuterHeight() {
        PdfCellStyle style = PdfCellStyle.builder().setPadding(3d).build()
        TableLayout.Cell filled = new TableLayout.Cell(new PdfCell(style, null), 5d, 40d, content(12d))
        TableLayout.Cell empty = new TableLayout.Cell(new PdfCell(style, null), 45d, 40d, null)

        assertEquals(5d, filled.xOffset, 0.001d)
        assertEquals(40d, filled.width, 0.001d)
        assertEquals(12d, filled.contentHeight, 0.001d)
        assertEquals("padding on both sides", 18d, filled.height, 0.001d)

        assertEquals(0d, empty.contentHeight, 0.001d)
        assertEquals(6d, empty.height, 0.001d)
    }

    void testACellIsAtLeastItsMinimumHeight() {
        PdfCellStyle style = PdfCellStyle.builder().setPadding(0d).setMinimumHeight(50d).build()
        TableLayout.Cell small = new TableLayout.Cell(new PdfCell(style, null), 0d, 40d, content(12d))

        assertEquals(50d, small.height, 0.001d)
    }

    void testRowsAreNotModifiableFromTheOutside() {
        TableLayout layout = new TableLayout(null, [10d] as double[], 10d, [row(1d)])

        try {
            layout.rows.add(row(2d))
            fail("expected the measured rows to be immutable")
        } catch (UnsupportedOperationException expected) {
            // as intended
        }
    }

    /** A measured layout is read by two collaborators, so neither can disturb the other's view. */
    void testTheCellsOfARowAreNotModifiableFromTheOutside() {
        TableLayout layout = new TableLayout(null, [10d] as double[], 10d, [row(1d)])

        try {
            layout.rows[0].cells.add(cell(10d, 1d))
            fail("expected the cells of a measured row to be immutable")
        } catch (UnsupportedOperationException expected) {
            // as intended
        }
    }
}
