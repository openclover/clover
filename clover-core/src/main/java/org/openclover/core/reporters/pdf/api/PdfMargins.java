package org.openclover.core.reporters.pdf.api;

/**
 * Page margins in points.
 */
public class PdfMargins {

    private final float left;
    private final float right;
    private final float top;
    private final float bottom;

    public PdfMargins(float left, float right, float top, float bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public float getLeft() {
        return left;
    }

    public float getRight() {
        return right;
    }

    public float getTop() {
        return top;
    }

    public float getBottom() {
        return bottom;
    }
}
