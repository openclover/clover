package org.openclover.core.cfg;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Percentage {

    private BigDecimal value;
    private final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public BigDecimal getValue() {
        return value;
    }

    public String toString() {
        return value.toString();
    }

    public float getAsFloatFraction() {
        return value.movePointLeft(2).floatValue();
    }

    public int getScale() {
        return value.scale();
    }

    public void setScale(int newScale) {
        value = value.setScale(newScale, ROUNDING);
    }

    public Percentage(String valueString) {
        int percentIndex = valueString.indexOf("%");
        String numericPortion = valueString;
        if (percentIndex != -1) {
            numericPortion = valueString.substring(0, percentIndex);
        }
        value = new BigDecimal(numericPortion);
    }

    public int compare(float fractionalpc) {
       BigDecimal cmp =  new BigDecimal(""+fractionalpc*100).setScale(getScale(), ROUNDING);
       return value.compareTo(cmp);
    }
}
