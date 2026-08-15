package org.openclover.core.reporters.pdf.api;

import java.awt.Color;

/**
 * An immutable description of the font a run of text is drawn with.
 */
public class PdfFontSpec {

    private final PdfFontFamily family;
    private final float size;
    private final PdfFontStyle style;
    private final Color colour;

    public static PdfFontSpec sans(float size) {
        return new PdfFontSpec(PdfFontFamily.SANS, size, PdfFontStyle.REGULAR, Color.black);
    }

    public static PdfFontSpec sans(float size, PdfFontStyle style) {
        return new PdfFontSpec(PdfFontFamily.SANS, size, style, Color.black);
    }

    public static PdfFontSpec sans(float size, PdfFontStyle style, Color colour) {
        return new PdfFontSpec(PdfFontFamily.SANS, size, style, colour);
    }

    public PdfFontSpec(PdfFontFamily family, float size, PdfFontStyle style, Color colour) {
        this.family = family;
        this.size = size;
        this.style = style;
        this.colour = colour;
    }

    public PdfFontFamily getFamily() {
        return family;
    }

    public float getSize() {
        return size;
    }

    public PdfFontStyle getStyle() {
        return style;
    }

    public Color getColour() {
        return colour;
    }
}
