package org.openclover.core.reporters.pdf.pdfbox

import junit.framework.TestCase
import org.apache.pdfbox.pdmodel.PDDocument
import org.openclover.core.reporters.pdf.api.PdfFontSpec
import org.openclover.core.reporters.pdf.api.PdfText

/**
 * Line breaking, exercised directly rather than through a rendered document so that the widths
 * can be asserted on exactly.
 */
class TextLayouterTest extends TestCase {

    private static final PdfFontSpec FONT = PdfFontSpec.sans(10)

    private PDDocument document
    private FontRegistry fonts
    private TextLayouter layouter

    void setUp() {
        document = new PDDocument()
        fonts = new FontRegistry(document)
        layouter = new TextLayouter(fonts)
    }

    void tearDown() {
        document.close()
    }

    private List<String> textOf(List<TextLayouter.Line> lines) {
        return lines.collect { line -> line.pieces.collect { it.text }.join("") }
    }

    void testWrapsOnWhitespace() {
        List<TextLayouter.Line> lines = layouter.layout(PdfText.of("alpha beta gamma", FONT), 40d)
        assertTrue("expected wrapping, got " + textOf(lines), lines.size() > 1)
        assertEquals("alpha beta gamma", textOf(lines).join("").replaceAll(" +\$", " ").trim() + "")
    }

    /**
     * A single word wider than the available width used to be emitted whole, spilling out of its
     * cell — the previous engine broke it mid-word instead, and long column headers rely on that.
     */
    void testBreaksWordWiderThanTheLine() {
        List<TextLayouter.Line> lines = layouter.layout(PdfText.of("CoveredBranches", FONT), 30d)

        assertTrue("expected the word to be split, got " + textOf(lines), lines.size() > 1)
        assertEquals("CoveredBranches", textOf(lines).join(""))
        lines.each { line ->
            assertTrue("line wider than the limit: '" + textOf([line])[0] + "' = " + line.width,
                    line.width <= 30d + 0.001d)
        }
    }

    void testBreaksWordWiderThanTheLineMidParagraph() {
        List<TextLayouter.Line> lines =
                layouter.layout(PdfText.of("ok Supercalifragilistic ok", FONT), 40d)

        assertEquals("ok Supercalifragilistic ok", textOf(lines).join(""))
        lines.each { line ->
            assertTrue("line wider than the limit: " + line.width, line.width <= 40d + 0.001d)
        }
    }

    void testNoGlyphNarrowerThanTheLineStillMakesProgress() {
        // an impossibly narrow column must terminate rather than loop, even though the text
        // cannot possibly fit
        List<TextLayouter.Line> lines = layouter.layout(PdfText.of("wide", FONT), 0.5d)

        assertEquals("wide", textOf(lines).join(""))
        assertEquals(4, lines.size())
    }

    void testExplicitNewlinesStartNewLines() {
        List<TextLayouter.Line> lines = layouter.layout(PdfText.of("first\nsecond", FONT), 500d)

        assertEquals(2, lines.size())
        assertEquals(["first", "second"], textOf(lines))
    }

    void testWhitespaceIsNeitherLostNorDuplicated() {
        List<TextLayouter.Line> lines = layouter.layout(PdfText.of("a  b   c", FONT), 500d)

        assertEquals(1, lines.size())
        assertEquals("a  b   c", textOf(lines).join(""))
    }

    /**
     * A carriage return has no glyph in the report font, so one that reached the content stream
     * would fail the whole document. Every line ending convention is folded into a single break
     * before the text is measured.
     */
    void testEveryLineEndingConventionBreaksExactlyOneLine() {
        assertEquals(["a", "b"], textOf(layouter.layout(PdfText.of("a\nb", FONT), 500d)))
        assertEquals(["a", "b"], textOf(layouter.layout(PdfText.of("a\r\nb", FONT), 500d)))
        assertEquals(["a", "b"], textOf(layouter.layout(PdfText.of("a\rb", FONT), 500d)))
    }

    void testNoCarriageReturnSurvivesIntoAPiece() {
        List<TextLayouter.Line> lines = layouter.layout(PdfText.of("first\r\nsecond\rthird", FONT), 500d)

        String drawn = lines.collect { line -> line.pieces.collect { it.text }.join("") }.join("")
        assertFalse(drawn, drawn.contains("\r"))
        assertFalse("a replaced CR would show up as a question mark", drawn.contains("?"))
    }
}
