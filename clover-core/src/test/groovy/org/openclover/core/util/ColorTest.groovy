package org.openclover.core.util

import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test

class ColorTest extends TestCase {

    @Before
    void setUp() throws Exception {
        System.setProperty(Color.COLOR_PROPERTY, Boolean.TRUE.toString())
    }

    @After
    void tearDown() throws Exception {
        System.setProperty(Color.COLOR_PROPERTY, Boolean.FALSE.toString())
    }

    @Test
    void testColors() {
        Color boldRed = Color.make("Bold Red").b().red()
        assertEquals("\u001B[0;1m\u001B[31m" + boldRed.getMsg() + Color.RESET, boldRed.toString())

        final Color whiteOnBlack = Color.make("White on Black").bg().black().white()
        assertEquals("\u001B[40m\u001B[37m" + whiteOnBlack.getMsg() + Color.RESET, whiteOnBlack.toString())

        final Color greenOnRed = Color.make("Bold Green on Majenta").b().green().bg().majenta()
        assertEquals("\u001B[0;1m\u001B[32m\u001B[45m" + greenOnRed.getMsg() + Color.RESET, greenOnRed.toString())

        final Color blueUnderlined = Color.make("Underlined Blue").u().blue()
        assertEquals("\u001B[0;4m\u001B[34m" + blueUnderlined.getMsg() + Color.RESET, blueUnderlined.toString())

        final Color italCyan = Color.make("Italic Cyan").i().cyan()
        assertEquals("\u001B[0;3m\u001B[36m" + italCyan.getMsg() + Color.RESET, italCyan.toString())

        final Color yellow = Color.make("Yellow").yellow()
        assertEquals("\u001B[33m" + yellow.getMsg() + Color.RESET, yellow.toString())
    }

    @Test
    void testColorOff() {
        System.setProperty(Color.COLOR_PROPERTY, Boolean.FALSE.toString())
        Color color = Color.make("No ANSI Format").red()
        assertEquals(color.getMsg(), color.toString())
    }

    @Test
    void testNonsenseFormats() {
        // some formats don't make sense, ensure Color can handle these anyway
        Color bg = Color.make("bg").bg()
        assertEquals(bg.getMsg(), bg.toString())

        Color noFmt = Color.make("nothing")
        assertEquals(noFmt.getMsg(), noFmt.toString())
    }

    @Test
    void testColorCategories() {
        System.setProperty(Color.COLOR_PROPERTY, "clover")

        try {
            final Color color = Color.colorFor("clover").b().red()
            final String output = color.apply("Bold and red.")
            assertEquals("\u001B[0;1m\u001B[31m" + color.getMsg() + Color.RESET, output)

            Color noColor = Color.colorFor("plain.jane").b().red()
            final String noColOutput = noColor.apply("No formatting should be applied.")
            assertEquals(noColor.getMsg(), noColOutput)
        } finally {
            System.setProperty(Color.COLOR_PROPERTY, Boolean.FALSE.toString())
        }
    }

}
