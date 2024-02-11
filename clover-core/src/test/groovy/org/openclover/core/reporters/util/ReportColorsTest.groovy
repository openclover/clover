package org.openclover.core.reporters.util

import com.atlassian.clover.reporters.util.ReportColors
import org.junit.Test

import java.awt.*

class ReportColorsTest {

    def reportColors = ReportColors.ADG_COLORS

    @Test
    void testGetColorMatchesGetStringColor() {

        //when
        def stringColor = reportColors.getStringColor(0.5d)
        def color = reportColors.getColor(0.5d)

        def result = Color.decode("0x${stringColor}");

        //then
        assert color.equals(result)
    }

    @Test
    void testGetIndexForValueAboveMax() {
        //having
        def maxColor = reportColors.getColor(1.0d)

        //when
        def result = reportColors.getColor(100d);

        //then
        assert maxColor.equals(result)
    }

    @Test
    void testGetIndexForValueBelowMin() {
        //having
        def minColor = reportColors.getColor(0.0d)

        //when
        def result = reportColors.getColor(-100d);

        //then
        assert minColor.equals(result)
    }
}
