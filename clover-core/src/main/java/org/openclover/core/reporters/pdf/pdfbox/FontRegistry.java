package org.openclover.core.reporters.pdf.pdfbox;

import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfFontStyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Loads the bundled Liberation Sans faces and embeds them into a document as Identity-H composite
 * fonts, so that report text is not limited to a single-byte encoding the way the previous
 * WinAnsi/CP1252 setup was.
 */
class FontRegistry {

    private static final String FONT_RESOURCE_DIR = "pdf_res/fonts/";

    /** Substituted for characters the font has no glyph for, rather than failing the report. */
    private static final String MISSING_GLYPH_REPLACEMENT = "?";

    private static class Face {
        final PDType0Font font;
        final CmapLookup cmap;
        final double ascent;
        final double descent;

        Face(PDType0Font font, CmapLookup cmap, double ascent, double descent) {
            this.font = font;
            this.cmap = cmap;
            this.ascent = ascent;
            this.descent = descent;
        }
    }

    private final PDDocument document;
    private final Map<PdfFontStyle, Face> faces = new EnumMap<>(PdfFontStyle.class);

    FontRegistry(PDDocument document) {
        this.document = document;
    }

    PDFont getFont(PdfFontSpec spec) {
        return face(spec.getStyle()).font;
    }

    /**
     * @return width of the text in points at the spec's size
     */
    double stringWidth(String text, PdfFontSpec spec) {
        if (text.isEmpty()) {
            return 0;
        }
        final Face face = face(spec.getStyle());
        try {
            return face.font.getStringWidth(sanitise(text, spec)) / 1000 * spec.getSize();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to measure text in the PDF report", e);
        }
    }

    double ascent(PdfFontSpec spec) {
        return face(spec.getStyle()).ascent / 1000 * spec.getSize();
    }

    double descent(PdfFontSpec spec) {
        return face(spec.getStyle()).descent / 1000 * spec.getSize();
    }

    /**
     * Replaces characters the face has no glyph for, so that an exotic package name or report
     * title degrades to '?' instead of aborting the whole report.
     */
    String sanitise(String text, PdfFontSpec spec) {
        final CmapLookup cmap = face(spec.getStyle()).cmap;
        StringBuilder cleaned = null;
        int i = 0;
        while (i < text.length()) {
            final int codePoint = text.codePointAt(i);
            final int charCount = Character.charCount(codePoint);
            // control characters never reach the content stream, they are handled by the layouter
            final boolean supported = codePoint == '\n' || codePoint == '\r'
                    || cmap.getGlyphId(codePoint) != 0;
            if (!supported && cleaned == null) {
                cleaned = new StringBuilder(text.length()).append(text, 0, i);
            }
            if (cleaned != null) {
                cleaned.append(supported ? text.substring(i, i + charCount) : MISSING_GLYPH_REPLACEMENT);
            }
            i += charCount;
        }
        return cleaned == null ? text : cleaned.toString();
    }

    private Face face(PdfFontStyle style) {
        Face face = faces.get(style);
        if (face == null) {
            face = loadFace(style);
            faces.put(style, face);
        }
        return face;
    }

    private Face loadFace(PdfFontStyle style) {
        final String resource = FONT_RESOURCE_DIR + resourceNameOf(style);
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Bundled PDF font not found on the classpath: " + resource);
            }
            final TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(in));
            final PDType0Font font = PDType0Font.load(document, ttf, true);
            return new Face(font, ttf.getUnicodeCmapLookup(),
                    ttf.getHorizontalHeader().getAscender() * 1000 / ttf.getUnitsPerEm(),
                    ttf.getHorizontalHeader().getDescender() * 1000 / ttf.getUnitsPerEm());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load the bundled PDF font " + resource, e);
        }
    }

    private static String resourceNameOf(PdfFontStyle style) {
        switch (style) {
            case BOLD:
                return "LiberationSans-Bold.ttf";
            case ITALIC:
                return "LiberationSans-Italic.ttf";
            default:
                return "LiberationSans-Regular.ttf";
        }
    }
}
