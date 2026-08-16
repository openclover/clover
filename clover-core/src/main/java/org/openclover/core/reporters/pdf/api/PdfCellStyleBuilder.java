package org.openclover.core.reporters.pdf.api;

import java.awt.Color;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Assembles {@link PdfCellStyle}s. A builder is mutable and reusable: {@link #build()} may be
 * called any number of times, each call taking an independent snapshot.
 *
 * <p>Consecutive builds with nothing changed in between return the same instance, so a table
 * of many identically styled cells holds one style rather than one per cell.
 */
public final class PdfCellStyleBuilder {

    /** The style last built, discarded as soon as any setter runs. */
    private PdfCellStyle cached;

    int colspan = 1;
    Set<PdfBorder> borders = PdfBorder.BOX;
    Color borderColour = Color.black;
    Color backgroundColour;
    double paddingTop = 2.0;
    double paddingBottom = 2.0;
    double paddingLeft = 2.0;
    double paddingRight = 2.0;
    double minimumHeight;
    double fixedLeading;
    double multipliedLeading = 1.0;
    PdfAlign.Horizontal horizontalAlignment = PdfAlign.Horizontal.LEFT;
    PdfAlign.Vertical verticalAlignment = PdfAlign.Vertical.TOP;

    PdfCellStyleBuilder() {
    }

    PdfCellStyleBuilder(PdfCellStyle style) {
        this.colspan = style.getColspan();
        this.borders = style.getBorders();
        this.borderColour = style.getBorderColour();
        this.backgroundColour = style.getBackgroundColour();
        this.paddingTop = style.getPaddingTop();
        this.paddingBottom = style.getPaddingBottom();
        this.paddingLeft = style.getPaddingLeft();
        this.paddingRight = style.getPaddingRight();
        this.minimumHeight = style.getMinimumHeight();
        this.fixedLeading = style.getFixedLeading();
        this.multipliedLeading = style.getMultipliedLeading();
        this.horizontalAlignment = style.getHorizontalAlignment();
        this.verticalAlignment = style.getVerticalAlignment();
    }

    public PdfCellStyle build() {
        if (cached == null) {
            cached = new PdfCellStyle(this);
        }
        return cached;
    }

    /** Invalidates the last build; every setter goes through here. */
    private PdfCellStyleBuilder changed() {
        cached = null;
        return this;
    }

    /**
     * @throws IllegalArgumentException if the cell would span fewer than one column, which
     *                                  would leave the row unable to fill its columns
     */
    public PdfCellStyleBuilder setColspan(int colspan) {
        if (colspan < 1) {
            throw new IllegalArgumentException("a cell must span at least one column: " + colspan);
        }
        this.colspan = colspan;
        return changed();
    }

    /** The edges are copied, so the style cannot be altered through the caller's set. */
    public PdfCellStyleBuilder setBorders(Set<PdfBorder> borders) {
        this.borders = borders.isEmpty()
                ? PdfBorder.NONE
                : Collections.unmodifiableSet(EnumSet.copyOf(borders));
        return changed();
    }

    /** Convenience for {@code setBorders(PdfBorder.of(edges))}. */
    public PdfCellStyleBuilder setBorders(PdfBorder... edges) {
        return setBorders(PdfBorder.of(edges));
    }

    public PdfCellStyleBuilder setBorderColour(Color borderColour) {
        this.borderColour = borderColour;
        return changed();
    }

    public PdfCellStyleBuilder setBackgroundColour(Color backgroundColour) {
        this.backgroundColour = backgroundColour;
        return changed();
    }

    public PdfCellStyleBuilder setPadding(double padding) {
        this.paddingTop = padding;
        this.paddingBottom = padding;
        this.paddingLeft = padding;
        this.paddingRight = padding;
        return changed();
    }

    public PdfCellStyleBuilder setPaddingTop(double padding) {
        this.paddingTop = padding;
        return changed();
    }

    public PdfCellStyleBuilder setPaddingBottom(double padding) {
        this.paddingBottom = padding;
        return changed();
    }

    public PdfCellStyleBuilder setPaddingLeft(double padding) {
        this.paddingLeft = padding;
        return changed();
    }

    public PdfCellStyleBuilder setPaddingRight(double padding) {
        this.paddingRight = padding;
        return changed();
    }

    public PdfCellStyleBuilder setMinimumHeight(double minimumHeight) {
        this.minimumHeight = minimumHeight;
        return changed();
    }

    /**
     * Sets line spacing as {@code fixed + multiplied * fontSize}, matching the leading model
     * the PDF reports were originally written against.
     */
    public PdfCellStyleBuilder setLeading(double fixed, double multiplied) {
        this.fixedLeading = fixed;
        this.multipliedLeading = multiplied;
        return changed();
    }

    public PdfCellStyleBuilder setHorizontalAlignment(PdfAlign.Horizontal horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
        return changed();
    }

    public PdfCellStyleBuilder setVerticalAlignment(PdfAlign.Vertical verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
        return changed();
    }
}
