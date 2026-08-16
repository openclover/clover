package org.openclover.core.reporters.pdf;

import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfRect;
import org.openclover.core.reporters.pdf.api.PdfWidget;

/**
 * The covered/uncovered coverage bar shown in the last column of the coverage tables.
 */
public class CoverageBarWidget implements PdfWidget {

    private static final double BAR_LINE_WIDTH = 0.5;

    /** The bar is drawn slightly shorter than the font height it is sized from. */
    private static final double HEIGHT_ADJUSTMENT = 2.0;

    /**
     * A coverage below this means there is nothing to report - the bar is then drawn in the "not
     * applicable" colour instead of being split into a covered and an uncovered part.
     */
    private static final double NOT_APPLICABLE = 0.0;

    private final double coveredPc;
    private final double height;
    private final double horizontalPaddingRatio;
    private final PDFColours colours;

    /**
     * @param coveredPc              fraction covered in the range [0..1], or a negative value when
     *                               there is nothing to report
     * @param fontHeight             height of the surrounding text, which the bar is sized against
     * @param horizontalPaddingRatio fraction of the available width left blank on each side
     */
    public CoverageBarWidget(double coveredPc, double fontHeight, double horizontalPaddingRatio,
                             PDFColours colours) {
        // prevent rendering nasties when a metric overshoots
        this.coveredPc = Math.min(coveredPc, 1.0);
        this.height = fontHeight - HEIGHT_ADJUSTMENT;
        this.horizontalPaddingRatio = horizontalPaddingRatio;
        this.colours = colours;
    }

    @Override
    public double preferredHeight() {
        return height;
    }

    @Override
    public void draw(PdfCanvas canvas, PdfRect bounds) {
        final double hMargin = bounds.getWidth() * horizontalPaddingRatio;
        final double barX = bounds.getX() + hMargin;
        final double barWidth = bounds.getWidth() - 2.0 * hMargin;
        final double barY = bounds.getY() + (bounds.getHeight() - height) / 2.0;
        final PdfRect bar = new PdfRect(barX, barY, barWidth, height);

        canvas.setLineWidth(BAR_LINE_WIDTH);
        if (coveredPc >= NOT_APPLICABLE) {
            canvas.fillRect(bar, colours.COL_BAR_UNCOVERED);

            final PdfRect covered = new PdfRect(barX, barY, barWidth * coveredPc, height);
            canvas.fillRect(covered, colours.COL_BAR_COVERED);
            canvas.strokeRect(covered, colours.COL_BAR_BORDER);
        } else {
            canvas.fillRect(bar, colours.COL_BAR_NA);
        }
        canvas.strokeRect(bar, colours.COL_BAR_BORDER);
    }
}
