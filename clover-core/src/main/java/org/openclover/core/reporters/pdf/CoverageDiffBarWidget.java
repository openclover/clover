package org.openclover.core.reporters.pdf;

import org.openclover.core.reporters.pdf.api.PdfAlign;
import org.openclover.core.reporters.pdf.api.PdfCanvas;
import org.openclover.core.reporters.pdf.api.PdfFontSpec;
import org.openclover.core.reporters.pdf.api.PdfRect;
import org.openclover.core.reporters.pdf.api.PdfText;
import org.openclover.core.reporters.pdf.api.PdfWidget;
import org.openclover.runtime.util.Formatting;

/**
 * The bar shown next to a class in the "top movers" and "classes added" tables, encoding how much
 * coverage moved and in which direction.
 *
 * <p>The bar used to be a two column table whose column widths carried the magnitude of the
 * change, with the bar painted over it by a table event; the same geometry is produced here
 * directly, which is why the proportions below look the way they do.
 */
public class CoverageDiffBarWidget implements PdfWidget {

    private static final double BAR_LINE_WIDTH = 0.5;
    private static final double HEIGHT_ADJUSTMENT = 2.0;

    /** Fraction of the width the bar always occupies, before the change is added to it. */
    private static final double BASE_WIDTH_PC = 20.0;
    private static final double VARIABLE_WIDTH_PC = 80.0;

    /**
     * Vertical padding the table this widget replaces used to contribute, kept so that the movers
     * rows keep the height they have always had.
     */
    private static final double ROW_PADDING = 4.0;

    private final double pcDiff;
    private final double barHeight;
    private final PdfFontSpec labelFont;
    private final String label;
    private final PDFColours colours;

    /**
     * @param pcDiff  change in coverage, in percentage points
     * @param pcNow   coverage after the change, in the range [0..1]
     * @param fontSize size of the label next to the bar
     */
    public CoverageDiffBarWidget(double pcDiff, double pcNow, double fontSize, PDFColours colours) {
        this.pcDiff = pcDiff / 100.0;
        this.barHeight = fontSize - HEIGHT_ADJUSTMENT;
        this.labelFont = PdfFontSpec.sans(fontSize);
        this.colours = colours;
        // Formatting still works in float; narrow only when handing the value over
        final String now = Formatting.getPercentStr((float) pcNow);
        final String delta = Formatting.format1d((float) (this.pcDiff * 100.0));
        this.label = this.pcDiff < 0.0
                ? "(" + now + ") " + delta
                : "+" + delta + " (" + now + ")";
    }

    @Override
    public double preferredHeight() {
        return barHeight + HEIGHT_ADJUSTMENT + ROW_PADDING;
    }

    @Override
    public void draw(PdfCanvas canvas, PdfRect bounds) {
        final boolean lostCoverage = pcDiff < 0.0;
        final double magnitude = Math.abs(pcDiff);

        // a loss puts the label first and grows the bar leftwards from the right edge; a gain
        // grows the bar from the left edge and puts the label after it
        final double labelFraction = lostCoverage
                ? (BASE_WIDTH_PC + VARIABLE_WIDTH_PC * (1.0 - magnitude)) / 100.0
                : VARIABLE_WIDTH_PC * magnitude / 100.0;
        final double split = bounds.getWidth() * labelFraction;

        final double barY = bounds.getY() + (bounds.getHeight() - barHeight) / 2.0;
        final PdfRect left = new PdfRect(bounds.getX(), barY, split, barHeight);
        final PdfRect right = new PdfRect(bounds.getX() + split, barY,
                bounds.getWidth() - split, barHeight);

        final PdfRect bar = lostCoverage ? right : left;
        final PdfRect labelBounds = lostCoverage ? left : right;

        canvas.setLineWidth(BAR_LINE_WIDTH);
        canvas.fillRect(bar, lostCoverage ? colours.COL_BAR_UNCOVERED : colours.COL_BAR_COVERED);
        canvas.strokeRect(bar, colours.COL_BAR_BORDER);

        canvas.drawText(PdfText.of(label, labelFont), labelBounds,
                lostCoverage ? PdfAlign.Horizontal.RIGHT : PdfAlign.Horizontal.LEFT);
    }
}
