package org.openclover.core.reporters.pdf.api;

import java.awt.Color;
import java.util.Objects;
import java.util.Set;

/**
 * Everything about a {@link PdfCell} except what is inside it: borders, colours, padding,
 * alignment and line spacing.
 *
 * <p>Immutable, so a style can be shared by any number of cells. Styles are assembled with a
 * {@link PdfCellStyleBuilder}, which is also what {@link PdfTable#getDefaultStyle()} hands out as
 * the template for the cells added after it.
 */
public final class PdfCellStyle {

    private final int colspan;
    private final Set<PdfBorder> borders;
    private final Color borderColour;
    private final Color backgroundColour;
    private final double paddingTop;
    private final double paddingBottom;
    private final double paddingLeft;
    private final double paddingRight;
    private final double minimumHeight;
    private final double fixedLeading;
    private final double multipliedLeading;
    private final PdfAlign.Horizontal horizontalAlignment;
    private final PdfAlign.Vertical verticalAlignment;

    PdfCellStyle(PdfCellStyleBuilder builder) {
        this.colspan = builder.colspan;
        this.borders = builder.borders;
        this.borderColour = builder.borderColour;
        this.backgroundColour = builder.backgroundColour;
        this.paddingTop = builder.paddingTop;
        this.paddingBottom = builder.paddingBottom;
        this.paddingLeft = builder.paddingLeft;
        this.paddingRight = builder.paddingRight;
        this.minimumHeight = builder.minimumHeight;
        this.fixedLeading = builder.fixedLeading;
        this.multipliedLeading = builder.multipliedLeading;
        this.horizontalAlignment = builder.horizontalAlignment;
        this.verticalAlignment = builder.verticalAlignment;
    }

    public static PdfCellStyleBuilder builder() {
        return new PdfCellStyleBuilder();
    }

    /** @return a builder initialised with this style's values */
    public PdfCellStyleBuilder toBuilder() {
        return new PdfCellStyleBuilder(this);
    }

    public int getColspan() {
        return colspan;
    }

    public Set<PdfBorder> getBorders() {
        return borders;
    }

    public Color getBorderColour() {
        return borderColour;
    }

    /** @return the fill colour, or null when the cell is not filled */
    public Color getBackgroundColour() {
        return backgroundColour;
    }

    public double getPaddingTop() {
        return paddingTop;
    }

    public double getPaddingBottom() {
        return paddingBottom;
    }

    public double getPaddingLeft() {
        return paddingLeft;
    }

    public double getPaddingRight() {
        return paddingRight;
    }

    public double getMinimumHeight() {
        return minimumHeight;
    }

    public double getFixedLeading() {
        return fixedLeading;
    }

    public double getMultipliedLeading() {
        return multipliedLeading;
    }

    public PdfAlign.Horizontal getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public PdfAlign.Vertical getVerticalAlignment() {
        return verticalAlignment;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PdfCellStyle)) {
            return false;
        }
        final PdfCellStyle that = (PdfCellStyle) other;
        return colspan == that.colspan
                && Double.compare(paddingTop, that.paddingTop) == 0
                && Double.compare(paddingBottom, that.paddingBottom) == 0
                && Double.compare(paddingLeft, that.paddingLeft) == 0
                && Double.compare(paddingRight, that.paddingRight) == 0
                && Double.compare(minimumHeight, that.minimumHeight) == 0
                && Double.compare(fixedLeading, that.fixedLeading) == 0
                && Double.compare(multipliedLeading, that.multipliedLeading) == 0
                && borders.equals(that.borders)
                && Objects.equals(borderColour, that.borderColour)
                && Objects.equals(backgroundColour, that.backgroundColour)
                && horizontalAlignment == that.horizontalAlignment
                && verticalAlignment == that.verticalAlignment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(colspan, borders, borderColour, backgroundColour,
                paddingTop, paddingBottom, paddingLeft, paddingRight,
                minimumHeight, fixedLeading, multipliedLeading,
                horizontalAlignment, verticalAlignment);
    }
}
