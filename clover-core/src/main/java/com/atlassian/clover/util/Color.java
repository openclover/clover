package com.atlassian.clover.util;

/**
 * A small Java implementation of http://en.wikipedia.org/wiki/ANSI_escape_code .
 *
 * To enable Color formatting, the ansi.color System property must be set to true.
 * To enable Color formatting for a specific category, the ansi.color System property must be set to the category name.
 * e.g. -Dansi.color=true
 * or   -Dansi.color=category.name
 *
 * This class uses the Builder Pattern, to allow combining formats.
 * The color should always be called after the format ({@link #bg}, {@link #u}, {@link #b} etc)
 * e.g. for a blue background and a bold, white foreground you would use:
 * <code>new Color("some important message").b().white().bg().blue();</code>
 */
public class Color {
    static final String CSI = "\u001B[";
    static final String COLOR_PROPERTY = "ansi.color";

    private static final int BLACK = 30;
    private static final int RED = 31;
    private static final int GREEN = 32;
    private static final int YELLOW = 33;
    private static final int BLUE = 34;
    private static final int MAJENTA = 35;
    private static final int CYAN = 36;
    private static final int WHITE = 37;
    private static final int DEFAULT = 39;

    static final String RESET = CSI + "1;0m";      //RESET; clears all colors and styles
    private static final String B = "0;1";          //B on
    private static final String I = "0;3";          //italics on
    private static final String U = "0;4";          //underline on

    private boolean bg = false;
    private String msg = "";
    private String category;
    private final StringBuffer formats = new StringBuffer();

    /**
     * Creates a color, with a specific appender name.
     *
     * This allows output of ansi formatting to be decided at runtime.
     * To enable color output just for the category 'categoryName' ensure the system property ansi.color is set to 'categoryName'.
     * e.g. -Dansi.color=categoryName
     *
     * @param categoryName the name of the category for this color.
     * @return a new instance of color for the given the given category name.
     */
    public static Color colorFor(String categoryName) {
        Color color = new Color();
        color.category = categoryName;
        return color;
    }

    /**
     * Creates a color instance with a given message.
     *
     * @param msg the message to use when formatting this color. NB: this may be overwritten by {@link #apply}
     * @return
     */
    public static Color make(String msg) {
        Color color = new Color();
        color.msg = msg;
        return color;
    }

    private Color() { }

    public Color b() {
        appendFmt(B);
        return this;
    }

    public Color i() {
        appendFmt(I);
        return this;
    }

    public Color u() {
        appendFmt(U);
        return this;
    }

    public Color black() {
        appendColor(BLACK);
        return this;
    }

    public Color yellow() {
        appendColor(YELLOW);
        return this;
    }

    public Color blue() {
        appendColor(BLUE);
        return this;
    }

    public Color majenta() {
        appendColor(MAJENTA);
        return this;
    }

    public Color cyan() {
        appendColor(CYAN);
        return this;
    }

    public Color white() {
        appendColor(WHITE);
        return this;
    }

    public Color red() {
        appendColor(RED);
        return this;
    }

    public Color green() {
        appendColor(GREEN);
        return this;
    }

    public Color bg() {
        bg = true;
        return this;
    }

    private void appendColor(int color) {
        color += bg ? 10 : 0;
        appendFmt(String.valueOf(color));
        bg = false;
    }

    private void appendFmt(String fmt) {
        formats.append(CSI).append(fmt).append("m");
    }

    public String toString() {
        if (colorOn() && formats.length() > 0) {
            return new StringBuffer(formats.toString()).append(msg).append(RESET).toString();
        } else {
            return msg;
        }
    }

    private boolean colorOn() {
        return Boolean.getBoolean(COLOR_PROPERTY) || // either all make is on
                (category != null && category.equalsIgnoreCase(System.getProperty(COLOR_PROPERTY))); // or just a specific category.
    }

    public String getMsg() {
        return msg;
    }

    public String apply(String msg) {
        this.msg = msg;
        return this.toString();
    }

}
