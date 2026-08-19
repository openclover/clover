package org.openclover.core.reporters.pdf

import junit.framework.TestCase
import org.openclover.core.reporters.DonationMessageGenerator
import org.openclover.core.reporters.pdf.api.PdfAlign
import org.openclover.core.reporters.pdf.api.PdfCanvas
import org.openclover.core.reporters.pdf.api.PdfMargins
import org.openclover.core.reporters.pdf.api.PdfPageContext
import org.openclover.core.reporters.pdf.api.PdfPageSize
import org.openclover.core.reporters.pdf.api.PdfTable

/**
 * The footer drawn on every page. Because decorators run once the body is laid out, the total page
 * count is simply available here rather than being patched in afterwards.
 */
class PageFooterRendererTest extends TestCase {

    private static final double PAGE_MARGIN = 25d
    private static final PdfMargins MARGINS = new PdfMargins(PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN, 35d)
    private static final double LOGO_SIZE = 32d

    private RecordingCanvas canvas
    private FakePageContext context

    void setUp() {
        canvas = new RecordingCanvas()
        context = new FakePageContext(canvas, 2, 7)
    }

    private void decorate() {
        new PageFooterRenderer(System.currentTimeMillis(), PDFColours.COL_COLOURS).decoratePage(context)
    }

    void testTheLogoSitsInTheBottomLeftCorner() {
        decorate()

        assertEquals(1, canvas.images.size())
        RecordingCanvas.DrawnImage logo = canvas.images[0]
        assertEquals("pdf_res/logo1.png", logo.resourcePath)
        assertEquals(PAGE_MARGIN, logo.bounds.x, 0.001d)
        assertEquals(0d, logo.bounds.y, 0.001d)
        assertEquals(LOGO_SIZE, logo.bounds.width, 0.001d)
        assertEquals(LOGO_SIZE, logo.bounds.height, 0.001d)
    }

    void testTheFooterIsRuledOffAboveAndSplitBeforeThePageCounter() {
        decorate()

        assertEquals(2, canvas.lines.size())
        double footerWidth = PdfPageSize.A4.width - 2 * PAGE_MARGIN

        RecordingCanvas.DrawnLine divider = canvas.lines[0]
        assertEquals("the divider runs the width of the footer", footerWidth, divider.x2 - divider.x1, 0.001d)
        assertEquals(LOGO_SIZE, divider.y1, 0.001d)
        assertEquals(divider.y1, divider.y2, 0.001d)

        RecordingCanvas.DrawnLine separator = canvas.lines[1]
        assertEquals("the separator is vertical", separator.x1, separator.x2, 0.001d)
        assertEquals(PAGE_MARGIN + footerWidth * 0.85d, separator.x1, 0.001d)
        assertEquals(0d, separator.y1, 0.001d)
        assertEquals(LOGO_SIZE, separator.y2, 0.001d)
    }

    void testThePageCounterKnowsTheFinalPageCount() {
        decorate()

        assertEquals(1, canvas.texts.size())
        assertEquals("Page 2 of 7", canvas.texts[0].plainText())
    }

    void testThePageCounterIsCentredInItsOwnSectionOfTheFooter() {
        decorate()

        double footerWidth = PdfPageSize.A4.width - 2 * PAGE_MARGIN
        double textBlockWidth = footerWidth * 0.85d
        RecordingCanvas.DrawnText counter = canvas.texts[0]

        assertEquals(PdfAlign.Horizontal.CENTER, counter.horizontal)
        assertEquals(PAGE_MARGIN + textBlockWidth, counter.bounds.x, 0.001d)
        assertEquals(footerWidth - textBlockWidth, counter.bounds.width, 0.001d)
    }

    void testTheGenerationDetailsAreDrawnBesideTheLogo() {
        decorate()

        assertEquals(1, context.tables.size())
        assertEquals(PAGE_MARGIN + LOGO_SIZE + 2d, context.tableX[0], 0.001d)
        assertEquals(LOGO_SIZE - 2d, context.tableTopY[0], 0.001d)
        // the footer table is positioned absolutely, so it must carry an absolute width
        assertEquals(PdfTable.WidthMode.ABSOLUTE, context.tables[0].widthMode)
    }

    /**
     * The report font is a text face with no emoji coverage, so the donation link uses the plain
     * label rather than the decorated one the console and HTML reports use.
     */
    void testTheDonationLinkAvoidsTheEmojiLabel() {
        decorate()

        String footerText = context.tables[0].cells[0].content.runs.collect { it.text }.join("")

        assertTrue(footerText, footerText.contains(DonationMessageGenerator.DONATE_LABEL_PLAIN))
        assertFalse(footerText, footerText.contains(DonationMessageGenerator.DONATE_LABEL))
    }

    void testTheOpenCloverLinkIsAnAnchor() {
        decorate()

        def runs = context.tables[0].cells[0].content.runs
        def links = runs.findAll { it.anchor != null }

        assertEquals(2, links.size())
        assertTrue(links.any { it.anchor == DonationMessageGenerator.DONATE_URL })
        assertTrue(links.any { it.text.startsWith("OpenClover v") })
    }

    /** A page context that records what the footer asked to be drawn. */
    private static class FakePageContext implements PdfPageContext {

        final PdfCanvas canvas
        final int pageNumber
        final int totalPages

        List<PdfTable> tables = []
        List<Double> tableX = []
        List<Double> tableTopY = []

        FakePageContext(PdfCanvas canvas, int pageNumber, int totalPages) {
            this.canvas = canvas
            this.pageNumber = pageNumber
            this.totalPages = totalPages
        }

        @Override
        int getPageNumber() {
            return pageNumber
        }

        @Override
        int getTotalPages() {
            return totalPages
        }

        @Override
        double getPageWidth() {
            return PdfPageSize.A4.width
        }

        @Override
        double getPageHeight() {
            return PdfPageSize.A4.height
        }

        @Override
        PdfMargins getMargins() {
            return MARGINS
        }

        @Override
        PdfCanvas getCanvas() {
            return canvas
        }

        @Override
        void drawTable(PdfTable table, double x, double topY) {
            tables.add(table)
            tableX.add(x)
            tableTopY.add(topY)
        }
    }
}
