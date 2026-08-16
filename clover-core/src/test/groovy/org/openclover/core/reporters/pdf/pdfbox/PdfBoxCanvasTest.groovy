package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.openclover.core.reporters.pdf.api.PdfAlign
import org.openclover.core.reporters.pdf.api.PdfCanvas
import org.openclover.core.reporters.pdf.api.PdfFontSpec
import org.openclover.core.reporters.pdf.api.PdfRect
import org.openclover.core.reporters.pdf.api.PdfText

import java.awt.Color

/**
 * The drawing primitives, exercised against a real page and then read back out of it. Text
 * positions are reported by PDFBox top-down, the opposite of the bottom-up user space the canvas
 * draws in, which is why the assertions below compare in that direction.
 */
class PdfBoxCanvasTest extends TestCase {

    private static final PdfFontSpec FONT = PdfFontSpec.sans(10d)
    private static final String LOGO = "pdf_res/logo1.png"

    private PDDocument document
    private PDPage page
    private PDPageContentStream stream
    private FontRegistry fonts
    private ImageRegistry images
    private PdfBoxCanvas canvas

    void setUp() {
        document = new PDDocument()
        page = new PDPage(PDRectangle.A4)
        document.addPage(page)
        stream = new PDPageContentStream(document, page)
        fonts = new FontRegistry(document)
        images = new ImageRegistry(document)
        canvas = new PdfBoxCanvas(document, page, stream, fonts, new TextLayouter(fonts), images)
    }

    void tearDown() {
        // a test that fails before closing the stream itself would otherwise leave PDFBox
        // complaining about it while the document closes, burying the real failure
        try {
            stream.close()
        } catch (IOException ignored) {
            // already closed by the test
        }
        document.close()
    }

    /** Closes the page and reads the positions of every glyph that ended up on it. */
    private List<TextPosition> renderAndReadPositions() {
        stream.close()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        document.save(out)

        List<TextPosition> positions = []
        PDDocument read = Loader.loadPDF(out.toByteArray())
        try {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> textPositions) {
                    positions.addAll(textPositions)
                }
            }
            stripper.getText(read)
        } finally {
            read.close()
        }
        return positions
    }

    private double leftmostX(List<TextPosition> positions, String glyph) {
        return positions.findAll { it.unicode == glyph }.collect { it.getXDirAdj() as double }.min()
    }

    private double topmostY(List<TextPosition> positions, String glyph) {
        return positions.findAll { it.unicode == glyph }.collect { it.getYDirAdj() as double }.min()
    }

    void testHorizontalAlignmentPlacesTextAcrossTheBox() {
        PdfRect bounds = new PdfRect(100d, 700d, 300d, 20d)
        canvas.drawText(PdfText.of("L", FONT), bounds, PdfAlign.Horizontal.LEFT)
        canvas.drawText(PdfText.of("C", FONT), bounds, PdfAlign.Horizontal.CENTER)
        canvas.drawText(PdfText.of("R", FONT), bounds, PdfAlign.Horizontal.RIGHT)

        List<TextPosition> positions = renderAndReadPositions()

        assertTrue(leftmostX(positions, "L") < leftmostX(positions, "C"))
        assertTrue(leftmostX(positions, "C") < leftmostX(positions, "R"))
        // the left-aligned glyph starts at the box's left edge, the right-aligned one ends at its
        // right edge
        assertEquals(100d, leftmostX(positions, "L"), 1d)
        assertTrue(leftmostX(positions, "R") > 380d)
    }

    void testVerticalAlignmentPlacesTextDownTheBox() {
        PdfRect bounds = new PdfRect(100d, 500d, 300d, 200d)
        canvas.drawText(PdfText.of("T", FONT), bounds, PdfAlign.Horizontal.LEFT,
                PdfAlign.Vertical.TOP, 0d, 1d)
        canvas.drawText(PdfText.of("M", FONT), bounds, PdfAlign.Horizontal.LEFT,
                PdfAlign.Vertical.MIDDLE, 0d, 1d)
        canvas.drawText(PdfText.of("B", FONT), bounds, PdfAlign.Horizontal.LEFT,
                PdfAlign.Vertical.BOTTOM, 0d, 1d)

        List<TextPosition> positions = renderAndReadPositions()

        // PDFBox reports y from the top of the page, so a higher glyph has a smaller y
        assertTrue(topmostY(positions, "T") < topmostY(positions, "M"))
        assertTrue(topmostY(positions, "M") < topmostY(positions, "B"))
    }

    /**
     * The baseline is lifted off the bottom of the line box by the font's descent, so descenders
     * stay inside the rectangle the text was given.
     */
    void testDescendersStayInsideTheBox() {
        double boxBottom = 500d
        PdfRect bounds = new PdfRect(100d, boxBottom, 300d, 20d)
        canvas.drawText(PdfText.of("gyp", FONT), bounds, PdfAlign.Horizontal.LEFT,
                PdfAlign.Vertical.BOTTOM, 0d, 1d)

        List<TextPosition> positions = renderAndReadPositions()
        double pageHeight = PDRectangle.A4.height as double

        // convert the reported top-down baseline back into user space
        double baseline = pageHeight - (positions.collect { it.getYDirAdj() as double }.max())
        double descent = fonts.descent(FONT)

        assertTrue("the baseline should sit above the box's bottom edge", baseline > boxBottom)
        assertTrue("descenders should not reach below the box",
                baseline + descent >= boxBottom - 0.01d)
    }

    void testLinkAnnotationCoversTheDrawnText() {
        canvas.drawText(PdfText.builder()
                .add("see ", FONT)
                .addLink("OpenClover", FONT, "https://openclover.org")
                .build(),
                new PdfRect(100d, 700d, 300d, 20d), PdfAlign.Horizontal.LEFT)
        stream.close()

        List<PDAnnotationLink> links = page.annotations.findAll { it instanceof PDAnnotationLink }
        assertEquals(1, links.size())

        def rect = links[0].rectangle
        // the annotation spans the linked run only, and is as tall as the font's ascent plus descent
        double expectedWidth = fonts.stringWidth("OpenClover", FONT)
        assertEquals(expectedWidth, (rect.upperRightX - rect.lowerLeftX) as double, 0.5d)
        assertEquals(fonts.ascent(FONT) - fonts.descent(FONT),
                (rect.upperRightY - rect.lowerLeftY) as double, 0.5d)
        // and it starts after the unlinked text preceding it
        assertTrue(rect.lowerLeftX > 100d + fonts.stringWidth("see", FONT))
    }

    void testBlankTextIsNotWrittenToThePage() {
        canvas.drawText(PdfText.of("   ", FONT), new PdfRect(100d, 700d, 300d, 20d),
                PdfAlign.Horizontal.LEFT)

        assertTrue("blank text produces no marks", renderAndReadPositions().isEmpty())
    }

    /**
     * A zero or negative extent would emit a rectangle operator that paints nothing, so those are
     * dropped before they reach the content stream.
     */
    void testDegenerateRectanglesAreNotDrawn() {
        canvas.fillRect(new PdfRect(10d, 10d, 0d, 20d), Color.RED)
        canvas.strokeRect(new PdfRect(10d, 10d, 20d, -5d), Color.RED)
        stream.close()

        ByteArrayOutputStream out = new ByteArrayOutputStream()
        document.save(out)

        PDDocument read = Loader.loadPDF(out.toByteArray())
        try {
            String content = new String(read.getPage(0).getContents().bytes, "ISO-8859-1")
            assertFalse("no rectangle should have been emitted, got:\n" + content,
                    content.contains(" re"))
        } finally {
            read.close()
        }
    }

    void testImagesAreLoadedOnceAndReused() {
        canvas.drawImage(LOGO, new PdfRect(10d, 10d, 32d, 32d))
        canvas.drawImage(LOGO, new PdfRect(50d, 10d, 32d, 32d))

        assertEquals(1, images.size())
    }

    /**
     * A bundled report resource that is not on the classpath is a packaging fault, not something
     * to silently render around.
     */
    void testAMissingImageFailsTheReport() {
        try {
            canvas.drawImage("pdf_res/there-is-no-such-image.png", new PdfRect(0d, 0d, 10d, 10d))
            fail("expected a missing image to fail the report")
        } catch (IllegalStateException expected) {
            assertTrue(expected.message, expected.message.contains("there-is-no-such-image.png"))
        }
    }

    void testGraphicsScopeCommitsOnCloseAndToleratesDoubleClosing() {
        PdfCanvas.GraphicsScope scope = canvas.beginGraphics(new PdfRect(100d, 100d, 200d, 100d))
        scope.graphics.setColor(Color.BLUE)
        scope.graphics.fillRect(0, 0, 50, 50)
        scope.close()
        scope.close()

        stream.close()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        document.save(out)

        PDDocument read = Loader.loadPDF(out.toByteArray())
        try {
            // the drawing was stamped onto the page as a form XObject
            assertTrue("expected a form XObject on the page",
                    read.getPage(0).resources.getXObjectNames().iterator().hasNext())
        } finally {
            read.close()
        }
    }
}
