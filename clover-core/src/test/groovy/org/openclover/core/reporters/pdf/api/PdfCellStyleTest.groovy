package org.openclover.core.reporters.pdf.api

import junit.framework.TestCase

import java.awt.Color

class PdfCellStyleTest extends TestCase {

    void testDefaultsMatchTheLayoutTheReportsWereAuthoredAgainst() {
        PdfCellStyle style = PdfCellStyle.builder().build()

        assertEquals(1, style.colspan)
        assertEquals(PdfBorder.BOX, style.borders)
        assertEquals(Color.black, style.borderColour)
        assertNull("an unfilled cell has no background", style.backgroundColour)
        [style.paddingTop, style.paddingBottom, style.paddingLeft, style.paddingRight].each {
            assertEquals(2d, it, 0.001d)
        }
        assertEquals(0d, style.minimumHeight, 0.001d)
        assertEquals(0d, style.fixedLeading, 0.001d)
        assertEquals(1d, style.multipliedLeading, 0.001d)
        assertEquals(PdfAlign.Horizontal.LEFT, style.horizontalAlignment)
        assertEquals(PdfAlign.Vertical.TOP, style.verticalAlignment)
    }

    /**
     * The builder is a template that keeps being edited while cells are added, so each build must
     * take an independent snapshot - otherwise a later edit would reach back into cells already
     * added.
     */
    void testEachBuildIsAnIndependentSnapshot() {
        PdfCellStyle.Builder builder = PdfCellStyle.builder().setPadding(4d)
        PdfCellStyle first = builder.build()

        builder.setPadding(9d).setColspan(3)
        PdfCellStyle second = builder.build()

        assertEquals(4d, first.paddingLeft, 0.001d)
        assertEquals(1, first.colspan)
        assertEquals(9d, second.paddingLeft, 0.001d)
        assertEquals(3, second.colspan)
    }

    void testSetPaddingSetsEveryEdgeAndSingleEdgesOverrideIt() {
        PdfCellStyle style = PdfCellStyle.builder()
                .setPadding(5d)
                .setPaddingLeft(1d)
                .setPaddingRight(2d)
                .setPaddingTop(3d)
                .setPaddingBottom(4d)
                .build()

        assertEquals(1d, style.paddingLeft, 0.001d)
        assertEquals(2d, style.paddingRight, 0.001d)
        assertEquals(3d, style.paddingTop, 0.001d)
        assertEquals(4d, style.paddingBottom, 0.001d)
    }

    void testBordersCanBeGivenAsEdgesOrAsASet() {
        assertEquals(PdfBorder.of(PdfBorder.TOP, PdfBorder.LEFT),
                PdfCellStyle.builder().setBorders(PdfBorder.TOP, PdfBorder.LEFT).build().borders)
        assertEquals(PdfBorder.NONE,
                PdfCellStyle.builder().setBorders(PdfBorder.NONE).build().borders)
    }

    void testLeadingIsFixedPlusMultiplied() {
        PdfCellStyle style = PdfCellStyle.builder().setLeading(2d, 0.9d).build()

        assertEquals(2d, style.fixedLeading, 0.001d)
        assertEquals(0.9d, style.multipliedLeading, 0.001d)
    }

    /**
     * A style is shared between cells, so deriving a variant must not disturb the original.
     */
    void testToBuilderCopiesWithoutAliasing() {
        PdfCellStyle original = PdfCellStyle.builder()
                .setPadding(3d)
                .setBackgroundColour(Color.BLUE)
                .setVerticalAlignment(PdfAlign.Vertical.MIDDLE)
                .build()

        PdfCellStyle derived = original.toBuilder().setPadding(7d).build()

        assertEquals(3d, original.paddingTop, 0.001d)
        assertEquals(7d, derived.paddingTop, 0.001d)
        // everything not overridden is carried over
        assertEquals(Color.BLUE, derived.backgroundColour)
        assertEquals(PdfAlign.Vertical.MIDDLE, derived.verticalAlignment)
    }

    /**
     * The style is handed out to cells, so a caller that keeps its own mutable set of edges must
     * not be able to restyle cells through it afterwards.
     */
    void testTheBorderSetIsCopiedOnTheWayIn() {
        Set<PdfBorder> mutable = EnumSet.of(PdfBorder.TOP)
        PdfCellStyle style = PdfCellStyle.builder().setBorders(mutable).build()

        mutable.add(PdfBorder.BOTTOM)

        assertEquals(PdfBorder.of(PdfBorder.TOP), style.borders)
        try {
            style.borders.add(PdfBorder.LEFT)
            fail("expected the style's borders to be immutable")
        } catch (UnsupportedOperationException expected) {
            // as intended
        }
    }

    void testACellMustSpanAtLeastOneColumn() {
        [0, -1].each { colspan ->
            try {
                PdfCellStyle.builder().setColspan(colspan)
                fail("expected a colspan of ${colspan} to be rejected")
            } catch (IllegalArgumentException expected) {
                // as intended
            }
        }
        assertEquals(1, PdfCellStyle.builder().setColspan(1).build().colspan)
    }

    /**
     * A table of many identically styled cells should hold one style, not one per cell.
     */
    void testBuildingTwiceWithNothingChangedReturnsTheSameStyle() {
        PdfCellStyle.Builder builder = PdfCellStyle.builder().setPadding(4d)

        PdfCellStyle first = builder.build()
        assertSame(first, builder.build())

        builder.setPadding(5d)
        assertNotSame("a change must invalidate the cached style", first, builder.build())
        assertEquals(4d, first.paddingTop, 0.001d)
    }

    void testStylesWithTheSameValuesAreEqual() {
        Closure<PdfCellStyle.Builder> template = {
            PdfCellStyle.builder()
                    .setPadding(3d)
                    .setColspan(2)
                    .setBorders(PdfBorder.TOP, PdfBorder.BOTTOM)
                    .setBackgroundColour(Color.BLUE)
                    .setLeading(1d, 0.9d)
                    .setHorizontalAlignment(PdfAlign.Horizontal.CENTER)
        }

        PdfCellStyle one = template().build()
        PdfCellStyle other = template().build()

        assertEquals(one, other)
        assertEquals(one.hashCode(), other.hashCode())
        assertFalse(one == template().setPadding(4d).build())
        assertFalse(one == template().setBorders(PdfBorder.TOP).build())
        assertFalse(one == template().setBackgroundColour(Color.RED).build())
        assertFalse(one.equals(null))
    }
}
