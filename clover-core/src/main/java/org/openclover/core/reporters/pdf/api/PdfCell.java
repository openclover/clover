package org.openclover.core.reporters.pdf.api;

import java.awt.Color;

/**
 * A single table cell. Mutable, because {@link PdfTable#getDefaultCell()} is used as a template
 * whose state is copied into every cell added afterwards.
 */
public class PdfCell {

    private PdfCellContent content;
    private int colspan = 1;
    private int borders = PdfBorder.BOX;
    private Color borderColour = Color.black;
    private Color backgroundColour;
    private double paddingTop = 2;
    private double paddingBottom = 2;
    private double paddingLeft = 2;
    private double paddingRight = 2;
    private double minimumHeight;
    private double fixedLeading;
    private double multipliedLeading = 1;
    private PdfAlign.Horizontal horizontalAlignment = PdfAlign.Horizontal.LEFT;
    private PdfAlign.Vertical verticalAlignment = PdfAlign.Vertical.TOP;

    public PdfCell() {
    }

    public PdfCell(PdfCell template) {
        this.colspan = template.colspan;
        this.borders = template.borders;
        this.borderColour = template.borderColour;
        this.backgroundColour = template.backgroundColour;
        this.paddingTop = template.paddingTop;
        this.paddingBottom = template.paddingBottom;
        this.paddingLeft = template.paddingLeft;
        this.paddingRight = template.paddingRight;
        this.minimumHeight = template.minimumHeight;
        this.fixedLeading = template.fixedLeading;
        this.multipliedLeading = template.multipliedLeading;
        this.horizontalAlignment = template.horizontalAlignment;
        this.verticalAlignment = template.verticalAlignment;
    }

    public PdfCellContent getContent() {
        return content;
    }

    public PdfCell setContent(PdfCellContent content) {
        this.content = content;
        return this;
    }

    public int getColspan() {
        return colspan;
    }

    public PdfCell setColspan(int colspan) {
        this.colspan = colspan;
        return this;
    }

    public int getBorders() {
        return borders;
    }

    public PdfCell setBorders(int borders) {
        this.borders = borders;
        return this;
    }

    public Color getBorderColour() {
        return borderColour;
    }

    public PdfCell setBorderColour(Color borderColour) {
        this.borderColour = borderColour;
        return this;
    }

    public Color getBackgroundColour() {
        return backgroundColour;
    }

    public PdfCell setBackgroundColour(Color backgroundColour) {
        this.backgroundColour = backgroundColour;
        return this;
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

    public PdfCell setPadding(double padding) {
        this.paddingTop = padding;
        this.paddingBottom = padding;
        this.paddingLeft = padding;
        this.paddingRight = padding;
        return this;
    }

    public PdfCell setPaddingLeft(double padding) {
        this.paddingLeft = padding;
        return this;
    }

    public double getMinimumHeight() {
        return minimumHeight;
    }

    public PdfCell setMinimumHeight(double minimumHeight) {
        this.minimumHeight = minimumHeight;
        return this;
    }

    public double getFixedLeading() {
        return fixedLeading;
    }

    public double getMultipliedLeading() {
        return multipliedLeading;
    }

    /**
     * Sets line spacing as {@code fixed + multiplied * fontSize}, matching the leading model the
     * PDF reports were originally written against.
     */
    public PdfCell setLeading(double fixed, double multiplied) {
        this.fixedLeading = fixed;
        this.multipliedLeading = multiplied;
        return this;
    }

    public PdfAlign.Horizontal getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public PdfCell setHorizontalAlignment(PdfAlign.Horizontal horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
        return this;
    }

    public PdfAlign.Vertical getVerticalAlignment() {
        return verticalAlignment;
    }

    public PdfCell setVerticalAlignment(PdfAlign.Vertical verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
        return this;
    }
}
