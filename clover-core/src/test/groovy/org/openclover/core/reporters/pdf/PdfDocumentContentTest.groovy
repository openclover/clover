package org.openclover.core.reporters.pdf

import junit.framework.TestCase
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.text.PDFTextStripper
import org.openclover.core.reporters.pdf.api.PdfBorder
import org.openclover.core.reporters.pdf.api.PdfCanvas
import org.openclover.core.reporters.pdf.api.PdfDocument
import org.openclover.core.reporters.pdf.api.PdfFontSpec
import org.openclover.core.reporters.pdf.api.PdfFontStyle
import org.openclover.core.reporters.pdf.api.PdfMargins
import org.openclover.core.reporters.pdf.api.PdfPageDecorator
import org.openclover.core.reporters.pdf.api.PdfPageSize
import org.openclover.core.reporters.pdf.api.PdfRect
import org.openclover.core.reporters.pdf.api.PdfTable
import org.openclover.core.reporters.pdf.api.PdfText
import org.openclover.core.reporters.pdf.api.PdfWidget
import org.openclover.core.reporters.pdf.pdfbox.PdfBoxDocumentFactory

/**
 * What ends up on the page: table text, embedded fonts, wrapped headers, links and widgets, read
 * back out of the generated document so the assertions describe what a reader actually sees.
 */
class PdfDocumentContentTest extends TestCase {

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

    void testRendersTableTextAndHeaders() {
        PdfDocument doc = newDocument()
        PdfTable table = new PdfTable(6)
        table.setWidthPercentage(100f)
        table.setWidths([50, 10, 10, 10, 7, 13] as int[])
        table.addCell(PdfText.of("Packages", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        table.addCell(PdfText.of("Branch", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        table.addCell(PdfText.of("Stmt", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        table.addCell(PdfText.of("Method", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        table.addCell(PdfText.of("Total", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        table.addCell(PdfText.of("Bar", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        table.addCell(PdfText.of("com.acme.model", PdfFontSpec.sans(10)))
        table.addCell(PdfText.of("64.3%", PdfFontSpec.sans(10)))
        table.addCell(PdfText.of("81%", PdfFontSpec.sans(10)))
        table.addCell(PdfText.of("100%", PdfFontSpec.sans(10)))
        table.addCell(PdfText.of("77.5%", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        table.addCell()
        doc.add(table)
        doc.close()

        String text = extractText()
        ["Packages", "Branch", "Stmt", "Method", "Total",
         "com.acme.model", "64.3%", "81%", "100%", "77.5%"].each {
            assertTrue("expected '${it}' in the generated PDF, got:\n${text}", text.contains(it))
        }
    }

    /**
     * The previous implementation encoded text as CP1252, which silently mangled anything outside
     * Latin-1. Text now round-trips through the embedded Unicode font.
     */
    void testUnicodeTextRoundTrips() {
        String title = "Zażółć gęślą jaźń — Отчёт — Ελληνικά"

        PdfDocument doc = newDocument()
        PdfTable table = new PdfTable(1)
        table.setWidthPercentage(100f)
        table.addCell(PdfText.of(title, PdfFontSpec.sans(12, PdfFontStyle.BOLD)))
        doc.add(table)
        doc.close()

        String text = extractText()
        assertTrue("expected the Unicode title to survive, got:\n${text}", text.contains(title))
    }

    /**
     * Characters the bundled font has no glyph for must degrade to a placeholder rather than
     * failing the whole report.
     */
    void testUnsupportedGlyphsDoNotFailTheReport() {
        PdfDocument doc = newDocument()
        PdfTable table = new PdfTable(1)
        table.setWidthPercentage(100f)
        table.addCell(PdfText.of("package 日本語 name", PdfFontSpec.sans(10)))
        doc.add(table)
        doc.close()

        String text = extractText()
        assertTrue("expected the surrounding text to survive, got:\n${text}", text.contains("package"))
        assertTrue("expected the surrounding text to survive, got:\n${text}", text.contains("name"))
    }

    /**
     * A word wider than its column is broken mid-word rather than allowed to spill out of the
     * cell. Long user-defined column headers such as "CoveredBranches" depend on this.
     */
    void testOverlongHeaderWrapsInsideItsColumn() {
        PdfDocument doc = newDocument()
        PdfTable table = new PdfTable(4)
        table.setWidthPercentage(100f)
        table.setWidths([50, 17, 17, 16] as int[])
        table.addCell(PdfText.of("Packages", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        table.addCell(PdfText.of("CoveredBranches", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        table.addCell(PdfText.of("CoveredStatements", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        table.addCell(PdfText.of("CoveredMethods", PdfFontSpec.sans(10, PdfFontStyle.BOLD)))
        doc.add(table)
        doc.close()

        assertEquals(1, pageCount())
        // the header survives; PDFTextStripper reports the wrapped fragments
        String text = extractText().replaceAll("\\s+", "")
        assertTrue("expected the wrapped header text, got:\n${extractText()}",
                text.contains("CoveredBranches"))
    }

    /**
     * A carriage return has no glyph in the report font. One reaching the content stream would
     * fail the whole document, so line endings are folded before the text is drawn.
     */
    void testWindowsLineEndingsRenderAsLineBreaks() {
        PdfDocument doc = newDocument()
        PdfTable table = new PdfTable(1)
        table.setWidthPercentage(100)
        table.addCell(PdfText.of("first line\r\nsecond line\rthird line", PdfFontSpec.sans(10)))

        doc.add(table)
        doc.close()

        String text = extractText()
        assertTrue(text, text.contains("first line"))
        assertTrue(text, text.contains("second line"))
        assertTrue(text, text.contains("third line"))
        assertFalse("a carriage return must not survive as a missing glyph", text.contains("?"))
    }

    void testLinksBecomeAnnotations() {
        PdfDocument doc = newDocument()
        PdfTable table = new PdfTable(1)
        table.setWidthPercentage(100f)
        table.addCell(PdfText.builder()
                .add("see ", PdfFontSpec.sans(10))
                .addLink("OpenClover", PdfFontSpec.sans(10, PdfFontStyle.BOLD), "https://openclover.org")
                .build())
        doc.add(table)
        doc.close()

        PDDocument read = Loader.loadPDF(out.toByteArray())
        try {
            List<PDAnnotationLink> links = read.getPage(0).getAnnotations().findAll {
                it instanceof PDAnnotationLink
            }
            assertEquals(1, links.size())
            assertEquals("https://openclover.org", ((PDActionURI) links[0].getAction()).getURI())
        } finally {
            read.close()
        }
    }

    void testWidgetsAreGivenTheResolvedCellRectangle() {
        PdfRect drawn = null
        PdfWidget widget = new PdfWidget() {
            @Override
            double preferredHeight() {
                return 8d
            }

            @Override
            void draw(PdfCanvas canvas, PdfRect bounds) {
                drawn = bounds
                canvas.fillRect(bounds, java.awt.Color.GREEN)
            }
        }

        PdfDocument doc = newDocument()
        PdfTable table = new PdfTable(2)
        table.setWidthPercentage(100f)
        table.setWidths([50, 50] as int[])
        table.getDefaultStyle().setBorders(PdfBorder.NONE)
        table.addCell(PdfText.of("coverage", PdfFontSpec.sans(10)))
        table.addCell(widget)
        doc.add(table)
        doc.close()

        assertNotNull("the widget should have been drawn", drawn)
        assertEquals(8d, drawn.height, 0.01d)
        // half the content width, less the cell padding on both sides
        double expectedWidth = (PdfPageSize.A4.width - 50) / 2 - 4
        assertEquals(expectedWidth, drawn.width, 0.5d)
    }
}
