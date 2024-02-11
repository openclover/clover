package org.openclover.core.util.format

import com.atlassian.clover.util.format.HtmlFormatter
import org.junit.Test

import static org.junit.Assert.assertEquals

class HtmlFormatterTest {

    @Test
    void testAnchorFormat() {
        assertEquals("<a href=\"http://www\">http://www</a>",
                HtmlFormatter.format("<a>http://www</a>"))
        assertEquals("Some other text <a href=\"http://www\">http://www</a>",
                HtmlFormatter.format("Some other text <a>http://www</a>"))
        assertEquals("<a>www</ a>",
                HtmlFormatter.format("<a>www</ a>"))
    }

    @Test
    void testMixedFormat() {
        assertEquals("<b><a href=\"www\">www</a><i>hello</i></b> ",
                HtmlFormatter.format("<b><a>www</a><i>hello</i></b> "))
    }

    @Test
    void testNewline() {
        assertEquals("Hello<br/>World",
                HtmlFormatter.format("Hello\nWorld"))
        assertEquals("<i>pre-expiry</i><br/>text.<br/>Come visit ",
                HtmlFormatter.format("<i>pre-expiry</i>\ntext.\nCome visit "))
    }

    @Test
    void testHorizontalLine() {
        assertEquals("Hello<hr>World",
                HtmlFormatter.format("Hello***********************World"))
    }
}

