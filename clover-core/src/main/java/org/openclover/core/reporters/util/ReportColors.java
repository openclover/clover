package org.openclover.core.reporters.util;

import java.awt.Color;

public abstract class ReportColors {

    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 1;

    public static final ReportColors ADG_COLORS = new ReportColors() {
        // A colour set for ADG report - red to light gray using 20% blending step
        private final String[] HEAT_COLORS_STRING =
                // 0-9       10-19     20-29     30-39     40-49     50-59     60-69     70-79     80-89     90-100
                // red 100% red90%   red80%     red70%   red60%     red 50%   yellow    yellow50%  green    green 50%
                {"d04437", "d5584c", "d9695f", "de7b72", "e28e87", "e7a19b", "f6c342", "fae1a0", "14892c", "89c495"};


        private final Color[] HEAT_COLORS = stringToColor(HEAT_COLORS_STRING);

        @Override
        public String getStringColor(double value) {
            return HEAT_COLORS_STRING[getIndexForValue(value, HEAT_COLORS_STRING.length)];
        }

        @Override
        public Color getColor(double value) {
            return HEAT_COLORS[getIndexForValue(value, HEAT_COLORS.length)];
        }
    };


    public abstract String getStringColor(double value);

    public abstract Color getColor(double value);

    private static Color[] stringToColor(String[] stringColors) {
        Color[] colors = new Color[stringColors.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.decode(hexColor(stringColors[i]));
        }
        return colors;
    }

    private static String hexColor(String hexColor) {
        return String.format("0x%s", hexColor);
    }


    protected int getIndexForValue(double value, int length) {
        final int index = (int) Math.round(value * (length - 1));
        if (index < MIN_VALUE) {
            return MIN_VALUE;
        }
        if (index >= length) {
            return length - 1;
        }
        return index;
    }
}
