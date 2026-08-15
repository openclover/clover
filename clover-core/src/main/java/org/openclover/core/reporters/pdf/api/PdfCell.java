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
    private float paddingTop = 2f;
    private float paddingBottom = 2f;
    private float paddingLeft = 2f;
    private float paddingRight = 2f;
    private float minimumHeight;
    private float fixedLeading;
    private float multipliedLeading = 1f;
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

    public float getPaddingTop() {
        return paddingTop;
    }

    public float getPaddingBottom() {
        return paddingBottom;
    }

    public float getPaddingLeft() {
        return paddingLeft;
    }

    public float getPaddingRight() {
        return paddingRight;
    }

    public PdfCell setPadding(float padding) {
        this.paddingTop = padding;
        this.paddingBottom = padding;
        this.paddingLeft = padding;
        this.paddingRight = padding;
        return this;
    }

    public PdfCell setPaddingLeft(float padding) {
        this.paddingLeft = padding;
        return this;
    }

    public float getMinimumHeight() {
        return minimumHeight;
    }

    public PdfCell setMinimumHeight(float minimumHeight) {
        this.minimumHeight = minimumHeight;
        return this;
    }

    public float getFixedLeading() {
        return fixedLeading;
    }

    public float getMultipliedLeading() {
        return multipliedLeading;
    }

    /**
     * Sets line spacing as {@code fixed + multiplied * fontSize}, matching the leading model the
     * PDF reports were originally written against.
     */
    public PdfCell setLeading(float fixed, float multiplied) {
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
