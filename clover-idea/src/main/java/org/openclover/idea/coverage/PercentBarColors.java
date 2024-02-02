package com.atlassian.clover.idea.coverage;

import java.awt.Color;

public enum PercentBarColors {
    GREEN_ON_RED(new Color(0, 220, 0), new Color(220, 0, 0)),
    GREEN_ON_BLACK(new Color(0, 220, 0), Color.BLACK),
    RED_ON_BLACK(new Color(220, 0, 0), Color.BLACK),
    LIGHTBLUE_ON_WHITE(new Color(0x87, 0xce, 0xfa), Color.WHITE);

    private final Color background;
    private final Color foreground;


    private PercentBarColors(Color foreground, Color background) {
        this.foreground = foreground;
        this.background = background;
    }

    public Color getBackground() {
        return background;
    }

    public Color getForeground() {
        return foreground;
    }
}
