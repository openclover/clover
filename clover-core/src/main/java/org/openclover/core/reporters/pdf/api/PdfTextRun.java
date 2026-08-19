package org.openclover.core.reporters.pdf.api;

/**
 * A stretch of text sharing one font, optionally acting as an external link.
 */
public class PdfTextRun {

    private final String text;
    private final PdfFontSpec font;
    private final String anchor;

    public PdfTextRun(String text, PdfFontSpec font) {
        this(text, font, null);
    }

    public PdfTextRun(String text, PdfFontSpec font, String anchor) {
        this.text = text == null ? "" : text;
        this.font = font;
        this.anchor = anchor;
    }

    public String getText() {
        return text;
    }

    public PdfFontSpec getFont() {
        return font;
    }

    /**
     * @return URI this run links to, or null when the run is not a link
     */
    public String getAnchor() {
        return anchor;
    }
}
