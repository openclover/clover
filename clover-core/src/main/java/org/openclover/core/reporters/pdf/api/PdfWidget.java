package org.openclover.core.reporters.pdf.api;

/**
 * Self-drawing cell content, used for the coverage bars and the historical charts. The layout
 * engine asks for the height the widget wants, then hands it the rectangle it ended up with.
 */
public interface PdfWidget extends PdfCellContent {

    /**
     * @return height in points this widget needs, excluding cell padding
     */
    double preferredHeight();

    void draw(PdfCanvas canvas, PdfRect bounds);

    /** A widget's size does not depend on the width it is given, so there is nothing to keep. */
    @Override
    default PdfMeasuredContent measure(PdfLayout layout, PdfCellStyle style, double contentWidth) {
        return new PdfMeasuredContent() {

            @Override
            public double getHeight() {
                return preferredHeight();
            }

            @Override
            public void draw(PdfCanvas canvas, PdfRect bounds) {
                PdfWidget.this.draw(canvas, bounds);
            }
        };
    }
}
