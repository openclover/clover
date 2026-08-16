package org.openclover.core.reporters.pdf.api;

import java.awt.Color;
import java.util.Set;

/**
 * Everything about a {@link PdfCell} except what is inside it: borders, colours, padding,
 * alignment and line spacing.
 *
 * <p>Immutable, so a style can be shared by any number of cells. Styles are assembled with a
 * {@link Builder}, which is also what {@link PdfTable#getDefaultStyle()} hands out as the template
 * for the cells added after it.
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

    private PdfCellStyle(Builder builder) {
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

    public static Builder builder() {
        return new Builder();
    }

    /** @return a builder initialised with this style's values */
    public Builder toBuilder() {
        return new Builder(this);
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

    /**
     * Assembles {@link PdfCellStyle}s. A builder is mutable and reusable: {@link #build()} may be
     * called any number of times, each call taking an independent snapshot.
     */
    public static final class Builder {

        private int colspan = 1;
        private Set<PdfBorder> borders = PdfBorder.BOX;
        private Color borderColour = Color.black;
        private Color backgroundColour;
        private double paddingTop = 2.0;
        private double paddingBottom = 2.0;
        private double paddingLeft = 2.0;
        private double paddingRight = 2.0;
        private double minimumHeight;
        private double fixedLeading;
        private double multipliedLeading = 1.0;
        private PdfAlign.Horizontal horizontalAlignment = PdfAlign.Horizontal.LEFT;
        private PdfAlign.Vertical verticalAlignment = PdfAlign.Vertical.TOP;

        private Builder() {
        }

        private Builder(PdfCellStyle style) {
            this.colspan = style.colspan;
            this.borders = style.borders;
            this.borderColour = style.borderColour;
            this.backgroundColour = style.backgroundColour;
            this.paddingTop = style.paddingTop;
            this.paddingBottom = style.paddingBottom;
            this.paddingLeft = style.paddingLeft;
            this.paddingRight = style.paddingRight;
            this.minimumHeight = style.minimumHeight;
            this.fixedLeading = style.fixedLeading;
            this.multipliedLeading = style.multipliedLeading;
            this.horizontalAlignment = style.horizontalAlignment;
            this.verticalAlignment = style.verticalAlignment;
        }

        public PdfCellStyle build() {
            return new PdfCellStyle(this);
        }

        public Builder setColspan(int colspan) {
            this.colspan = colspan;
            return this;
        }

        public Builder setBorders(Set<PdfBorder> borders) {
            this.borders = borders;
            return this;
        }

        /** Convenience for {@code setBorders(PdfBorder.of(edges))}. */
        public Builder setBorders(PdfBorder... edges) {
            return setBorders(PdfBorder.of(edges));
        }

        public Builder setBorderColour(Color borderColour) {
            this.borderColour = borderColour;
            return this;
        }

        public Builder setBackgroundColour(Color backgroundColour) {
            this.backgroundColour = backgroundColour;
            return this;
        }

        public Builder setPadding(double padding) {
            this.paddingTop = padding;
            this.paddingBottom = padding;
            this.paddingLeft = padding;
            this.paddingRight = padding;
            return this;
        }

        public Builder setPaddingTop(double padding) {
            this.paddingTop = padding;
            return this;
        }

        public Builder setPaddingBottom(double padding) {
            this.paddingBottom = padding;
            return this;
        }

        public Builder setPaddingLeft(double padding) {
            this.paddingLeft = padding;
            return this;
        }

        public Builder setPaddingRight(double padding) {
            this.paddingRight = padding;
            return this;
        }

        public Builder setMinimumHeight(double minimumHeight) {
            this.minimumHeight = minimumHeight;
            return this;
        }

        /**
         * Sets line spacing as {@code fixed + multiplied * fontSize}, matching the leading model
         * the PDF reports were originally written against.
         */
        public Builder setLeading(double fixed, double multiplied) {
            this.fixedLeading = fixed;
            this.multipliedLeading = multiplied;
            return this;
        }

        public Builder setHorizontalAlignment(PdfAlign.Horizontal horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            return this;
        }

        public Builder setVerticalAlignment(PdfAlign.Vertical verticalAlignment) {
            this.verticalAlignment = verticalAlignment;
            return this;
        }
    }
}
