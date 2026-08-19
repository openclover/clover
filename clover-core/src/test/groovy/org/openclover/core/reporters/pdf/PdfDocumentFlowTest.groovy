package org.openclover.core.reporters.pdf

import junit.framework.TestCase
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.openclover.core.reporters.pdf.api.PdfAlign
import org.openclover.core.reporters.pdf.api.PdfDocument
import org.openclover.core.reporters.pdf.api.PdfFontSpec
import org.openclover.core.reporters.pdf.api.PdfMargins
import org.openclover.core.reporters.pdf.api.PdfPageContext
import org.openclover.core.reporters.pdf.api.PdfPageDecorator
import org.openclover.core.reporters.pdf.api.PdfPageSize
import org.openclover.core.reporters.pdf.api.PdfRect
import org.openclover.core.reporters.pdf.api.PdfTable
import org.openclover.core.reporters.pdf.api.PdfText
import org.openclover.core.reporters.pdf.pdfbox.PdfBoxDocumentFactory

/**
 * How content flows down and across pages: where a table breaks, what a page break does, the page
 * size and width model, and what the page decorator is told once the body is complete.
 */
class PdfDocumentFlowTest extends TestCase {

    private static final PdfMargins MARGINS = new PdfMargins(25, 25, 25, 35)

    private ByteArrayOutputStream out
    private PdfDocument document

    void setUp() {
        out = new ByteArrayOutputStream()
    }

    private PdfDocument newDocument(PdfPageDecorator decorator = null,
                                    PdfPageSize pageSize = PdfPageSize.A4) {
        document = new PdfBoxDocumentFactory().create(out, pageSize, MARGINS, decorator)
        return document
    }

    private String extractText() {
        PDDocument read = Loader.loadPDF(out.toByteArray())
        try {
            return new PDFTextStripper().getText(read)
        } finally {
            read.close()
        }
    }

    private int pageCount() {
        PDDocument read = Loader.loadPDF(out.toByteArray())
        try {
            return read.getNumberOfPages()
        } finally {
            read.close()
        }
    }

    void testLongTableFlowsOntoFurtherPages() {
        PdfDocument doc = newDocument()
        PdfTable table = new PdfTable(1)
        table.setWidthPercentage(100f)
        200.times { table.addCell(PdfText.of("row ${it}", PdfFontSpec.sans(10))) }
        doc.add(table)
        doc.close()

        assertTrue("expected the table to span several pages", pageCount() > 1)
        String text = extractText()
        assertTrue(text.contains("row 0"))
        assertTrue(text.contains("row 199"))
    }

    /**
     * A row taller than a whole page cannot be broken, so it is moved to a fresh page and allowed
     * to run over rather than being dropped or looping forever looking for room.
     */
    void testARowTallerThanAPageIsDrawnOnceOnAPageOfItsOwn() {
        PdfDocument doc = newDocument()

        PdfTable first = new PdfTable(1)
        first.setWidthPercentage(100)
        first.addCell(PdfText.of("before the giant", PdfFontSpec.sans(10)))
        doc.add(first)

        PdfTable giant = new PdfTable(1)
        giant.setWidthPercentage(100)
        giant.getDefaultStyle().setMinimumHeight(PdfPageSize.A4.height * 2)
        giant.addCell(PdfText.of("the giant row", PdfFontSpec.sans(10)))
        doc.add(giant)

        PdfTable last = new PdfTable(1)
        last.setWidthPercentage(100)
        last.addCell(PdfText.of("after the giant", PdfFontSpec.sans(10)))
        doc.add(last)

        doc.close()

        String text = extractText()
        assertEquals("the giant row must be drawn exactly once",
                1, text.count("the giant row"))
        assertTrue(text, text.contains("before the giant"))
        assertTrue("content after an over-tall row must still be drawn", text.contains("after the giant"))
        // the giant starts a page of its own, and the row after it starts another
        assertEquals(3, pageCount())
    }

    /**
     * The report flow ends every section with a page break; that must not leave a blank page
     * behind, so pages are only materialised once something is actually drawn on them.
     */
    void testTrailingPageBreakLeavesNoBlankPage() {
        PdfDocument doc = newDocument()
        PdfTable table = new PdfTable(1)
        table.addCell(PdfText.of("only page", PdfFontSpec.sans(10)))
        doc.add(table)
        doc.newPage()
        doc.close()

        assertEquals(1, pageCount())
    }

    void testRepeatedPageBreaksDoNotAccumulateBlankPages() {
        PdfDocument doc = newDocument()
        3.times {
            PdfTable table = new PdfTable(1)
            table.addCell(PdfText.of("section ${it}", PdfFontSpec.sans(10)))
            doc.add(table)
            doc.newPage()
            doc.newPage()
        }
        doc.close()

        assertEquals(3, pageCount())
    }

    /**
     * The footer needs the final page count, which is why decorators run after the body has been
     * laid out rather than while each page is being written.
     */
    void testDecoratorSeesFinalPageCount() {
        List<String> footers = []
        PdfDocument doc = newDocument(new PdfPageDecorator() {
            @Override
            void decoratePage(PdfPageContext context) {
                String footer = "Page ${context.pageNumber} of ${context.totalPages}"
                footers.add(footer)
                context.canvas.drawText(PdfText.of(footer, PdfFontSpec.sans(8)),
                        new PdfRect(25, 10, 200, 10), PdfAlign.Horizontal.LEFT)
            }
        })
        PdfTable table = new PdfTable(1)
        table.setWidthPercentage(100f)
        200.times { table.addCell(PdfText.of("row ${it}", PdfFontSpec.sans(10))) }
        doc.add(table)
        doc.close()

        int pages = pageCount()
        assertEquals(pages, footers.size())
        assertEquals("Page 1 of ${pages}".toString(), footers[0])

        String text = extractText()
        assertTrue("expected the page counter in the footer, got:\n${text}",
                text.contains("Page 1 of ${pages}"))
    }

    void testLetterPageSizeIsHonoured() {
        PdfDocument doc = newDocument(null, PdfPageSize.LETTER)
        PdfTable table = new PdfTable(1)
        table.addCell(PdfText.of("hello", PdfFontSpec.sans(10)))
        doc.add(table)
        doc.close()

        PDDocument read = Loader.loadPDF(out.toByteArray())
        try {
            assertEquals(612f, read.getPage(0).getMediaBox().getWidth(), 0.01f)
            assertEquals(792f, read.getPage(0).getMediaBox().getHeight(), 0.01f)
        } finally {
            read.close()
        }
    }

    /**
     * A top-level table that was not given a width takes 80% of the available width, which
     * several report elements rely on without setting a width explicitly, while the same table
     * nested inside a cell fills that cell.
     */
    void testAutoWidthDependsOnNesting() {
        PdfTable table = new PdfTable(1)

        assertEquals(PdfTable.WidthMode.AUTO, table.getWidthMode())
        assertEquals(80d, table.resolveWidth(100d, false), 0.001d)
        assertEquals(100d, table.resolveWidth(100d, true), 0.001d)
    }

    void testExplicitWidthsOverrideNesting() {
        assertEquals(50d, new PdfTable(1).setWidthPercentage(50).resolveWidth(100d, true), 0.001d)
        assertEquals(123d, new PdfTable(1).setTotalWidth(123).resolveWidth(100d, true), 0.001d)
    }

    void testNonPositiveWidthsAreRejected() {
        [-1d, 0d].each { width ->
            try {
                new PdfTable(1).setTotalWidth(width)
                fail("expected a total width of ${width} to be rejected")
            } catch (IllegalArgumentException expected) {
                // as intended
            }
        }
    }
}
