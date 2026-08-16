package org.openclover.core.reporters.pdf.api;

import java.awt.Color;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
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

    /**
     * Assembles {@link PdfCellStyle}s. A builder is mutable and reusable: {@link #build()} may be
     * called any number of times, each call taking an independent snapshot.
     *
     * <p>Consecutive builds with nothing changed in between return the same instance, so a table
     * of many identically styled cells holds one style rather than one per cell.
     */
    public static final class Builder {

        /** The style last built, discarded as soon as any setter runs. */
        private PdfCellStyle cached;

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
            if (cached == null) {
                cached = new PdfCellStyle(this);
            }
            return cached;
        }

        /** Invalidates the last build; every setter goes through here. */
        private Builder changed() {
            cached = null;
            return this;
        }

        /**
         * @throws IllegalArgumentException if the cell would span fewer than one column, which
         *                                  would leave the row unable to fill its columns
         */
        public Builder setColspan(int colspan) {
            if (colspan < 1) {
                throw new IllegalArgumentException("a cell must span at least one column: " + colspan);
            }
            this.colspan = colspan;
            return changed();
        }

        /** The edges are copied, so the style cannot be altered through the caller's set. */
        public Builder setBorders(Set<PdfBorder> borders) {
            this.borders = borders.isEmpty()
                    ? PdfBorder.NONE
                    : Collections.unmodifiableSet(EnumSet.copyOf(borders));
            return changed();
        }

        /** Convenience for {@code setBorders(PdfBorder.of(edges))}. */
        public Builder setBorders(PdfBorder... edges) {
            return setBorders(PdfBorder.of(edges));
        }

        public Builder setBorderColour(Color borderColour) {
            this.borderColour = borderColour;
            return changed();
        }

        public Builder setBackgroundColour(Color backgroundColour) {
            this.backgroundColour = backgroundColour;
            return changed();
        }

        public Builder setPadding(double padding) {
            this.paddingTop = padding;
            this.paddingBottom = padding;
            this.paddingLeft = padding;
            this.paddingRight = padding;
            return changed();
        }

        public Builder setPaddingTop(double padding) {
            this.paddingTop = padding;
            return changed();
        }

        public Builder setPaddingBottom(double padding) {
            this.paddingBottom = padding;
            return changed();
        }

        public Builder setPaddingLeft(double padding) {
            this.paddingLeft = padding;
            return changed();
        }

        public Builder setPaddingRight(double padding) {
            this.paddingRight = padding;
            return changed();
        }

        public Builder setMinimumHeight(double minimumHeight) {
            this.minimumHeight = minimumHeight;
            return changed();
        }

        /**
         * Sets line spacing as {@code fixed + multiplied * fontSize}, matching the leading model
         * the PDF reports were originally written against.
         */
        public Builder setLeading(double fixed, double multiplied) {
            this.fixedLeading = fixed;
            this.multipliedLeading = multiplied;
            return changed();
        }

        public Builder setHorizontalAlignment(PdfAlign.Horizontal horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            return changed();
        }

        public Builder setVerticalAlignment(PdfAlign.Vertical verticalAlignment) {
            this.verticalAlignment = verticalAlignment;
            return changed();
        }
    }
}
