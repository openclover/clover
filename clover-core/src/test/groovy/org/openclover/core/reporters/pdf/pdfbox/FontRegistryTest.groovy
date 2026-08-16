package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.apache.pdfbox.pdmodel.PDDocument
import org.openclover.core.reporters.pdf.api.PdfFontSpec
import org.openclover.core.reporters.pdf.api.PdfFontStyle

/**
 * Font loading, measurement and the glyph fallback. The bundled Liberation Sans faces are embedded
 * as Unicode composite fonts, which is what lets report text carry anything outside Latin-1.
 */
class FontRegistryTest extends TestCase {

    private PDDocument document
    private FontRegistry fonts

    void setUp() {
        document = new PDDocument()
        fonts = new FontRegistry(document)
    }

    void tearDown() {
        document.close()
    }

    void testEachStyleLoadsItsOwnFace() {
        def regular = fonts.getFont(PdfFontSpec.sans(10d))
        def bold = fonts.getFont(PdfFontSpec.sans(10d, PdfFontStyle.BOLD))
        def italic = fonts.getFont(PdfFontSpec.sans(10d, PdfFontStyle.ITALIC))

        assertNotNull(regular)
        assertTrue(!regular.is(bold) && !regular.is(italic) && !bold.is(italic))
    }

    /**
     * A face is embedded into the document once and then reused; loading it per cell would bloat
     * the report and slow it down.
     */
    void testFacesAreLoadedOnceAndCached() {
        def first = fonts.getFont(PdfFontSpec.sans(10d))
        def second = fonts.getFont(PdfFontSpec.sans(24d))

        assertSame("the size is applied when drawing, not when loading", first, second)
    }

    void testWidthScalesWithTheFontSize() {
        double atTen = fonts.stringWidth("coverage", PdfFontSpec.sans(10d))
        double atTwenty = fonts.stringWidth("coverage", PdfFontSpec.sans(20d))

        assertTrue("expected a positive width, got " + atTen, atTen > 0d)
        assertEquals(2 * atTen, atTwenty, 0.001d)
    }

    void testWidthGrowsWithTheText() {
        PdfFontSpec font = PdfFontSpec.sans(10d)

        assertEquals(0d, fonts.stringWidth("", font), 0.001d)
        assertTrue(fonts.stringWidth("mm", font) > fonts.stringWidth("m", font))
    }

    /**
     * Ascent is measured up from the baseline and descent down from it, so the descent is
     * negative - that sign is what lifts a baseline off the bottom of its line box.
     */
    void testAscentIsPositiveAndDescentNegative() {
        PdfFontSpec font = PdfFontSpec.sans(10d)

        assertTrue("ascent should be positive: " + fonts.ascent(font), fonts.ascent(font) > 0d)
        assertTrue("descent should be negative: " + fonts.descent(font), fonts.descent(font) < 0d)
        // together they stay in the region of the nominal size
        double lineHeight = fonts.ascent(font) - fonts.descent(font)
        assertTrue("implausible line height: " + lineHeight, lineHeight > 8d && lineHeight < 16d)
    }

    void testMetricsScaleWithTheFontSize() {
        assertEquals(2 * fonts.ascent(PdfFontSpec.sans(10d)),
                fonts.ascent(PdfFontSpec.sans(20d)), 0.001d)
        assertEquals(2 * fonts.descent(PdfFontSpec.sans(10d)),
                fonts.descent(PdfFontSpec.sans(20d)), 0.001d)
    }

    void testSupportedTextIsPassedThroughUntouched() {
        PdfFontSpec font = PdfFontSpec.sans(10d)
        String text = "org.openclover.core - Zażółć gęślą jaźń"

        assertEquals(text, fonts.sanitise(text, font))
    }

    /**
     * An exotic package name must degrade to a placeholder rather than failing the whole report.
     */
    void testUnsupportedGlyphsBecomeAPlaceholder() {
        String cleaned = fonts.sanitise("package 日本語 name", PdfFontSpec.sans(10d))

        assertEquals("package ??? name", cleaned)
    }

    /**
     * A character outside the basic plane is one code point but two chars, and must be replaced as
     * a single unit rather than leaving half a surrogate pair behind.
     */
    void testSupplementaryCharactersAreReplacedAsOneUnit() {
        String cleaned = fonts.sanitise("a😀b", PdfFontSpec.sans(10d))

        assertEquals("a?b", cleaned)
    }

    /**
     * Line breaks are the layouter's business and never reach the content stream, so they must
     * survive sanitising even though no glyph is drawn for them.
     */
    void testLineBreaksSurviveSanitising() {
        assertEquals("first\nsecond\r\nthird",
                fonts.sanitise("first\nsecond\r\nthird", PdfFontSpec.sans(10d)))
    }
}
