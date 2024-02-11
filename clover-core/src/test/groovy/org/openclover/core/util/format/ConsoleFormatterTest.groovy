package org.openclover.core.util.format

import com.atlassian.clover.util.format.ConsoleFormatter
import org.junit.Test

import static org.junit.Assert.assertEquals

class ConsoleFormatterTest {

    @Test
    void testFormat() {
        assertEquals("Hello there, how are you.",
                ConsoleFormatter.format("<b>Hello <i>there</i></b>, how are you."))
        assertEquals("Hello ******** World",
                ConsoleFormatter.format("<b>Hello <a>********</a> World</i>"))
    }
}
