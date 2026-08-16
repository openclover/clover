package org.openclover.core.reporters.pdf.api;

/**
 * Line widths shared by everything the reports draw.
 */
public final class PdfStroke {

    /**
     * The hairline the reports rule everything with: cell borders, the footer dividers and the
     * outlines of the coverage bars.
     */
    public static final double THIN_BORDER_WIDTH = 0.5;

    private PdfStroke() {
    }
}
