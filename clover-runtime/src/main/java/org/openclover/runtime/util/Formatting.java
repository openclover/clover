package org.openclover.runtime.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Formatting
{
    private static final String STRING_ABBREV = "...";
    private static final DecimalFormat PC_FORMAT = new DecimalFormat("#,###.#%");
    private static final NumberFormat PC_WIDTH_FORMAT = NumberFormat.getInstance(Locale.US);
    static {
        PC_WIDTH_FORMAT.setMaximumFractionDigits(1);
    }
    private static final DecimalFormat D1_FORMAT = new DecimalFormat("#,###.#");
    private static final DecimalFormat D2_FORMAT = new DecimalFormat("#,###.##");
    private static final DecimalFormat D3_FORMAT = new DecimalFormat("#,###.###");
    private static final DecimalFormat INT_FORMAT = new DecimalFormat("#,###");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d yyyy HH:mm:ss z");
    private static final DateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("d MMM HH:mm:ss");

    private Formatting()
    {
        // no instances of me
    }

    public synchronized static String getPercentStr(float aPc) {
        return aPc < 0 ? " - " : PC_FORMAT.format(aPc);
    }

    public synchronized static String getPcWidth(float aPc) {
        return PC_WIDTH_FORMAT.format(aPc) + "%";
    }

    public synchronized static String format1d(float val) {
        return D1_FORMAT.format(val);
    }

    public synchronized static String format2d(double val) {
        if (val < 0) {
            return "-";
        }
        return D2_FORMAT.format(val);
    }

    public synchronized static String format3d(double val) {
        if (val < 0) {
            return "-";
        }
        return D3_FORMAT.format(val);
    }

    public synchronized static String formatInt(int aVal)
    {
        return INT_FORMAT.format(aVal);
    }

    public synchronized static String formatDate(Date aDate)
    {
        return DATE_FORMAT.format(aDate);
    }

    public synchronized static String formatShortDate(Date aDate)
    {
        return SHORT_DATE_FORMAT.format(aDate);
    }

    public static String restrictLength(String str, int maxlength, boolean prefix) {
        if (str == null) {
            return "";
        }
        int abbrevlength = STRING_ABBREV.length();
        int strlength = str.length();
        // no limit, or string is within limit, or string can't be abbreviated
        if (maxlength < 0 || strlength <= maxlength || strlength <= abbrevlength) {
            return str;
        }

        if (prefix) {
            return STRING_ABBREV + str.substring((strlength - maxlength) + abbrevlength);
        }
        else {
            return str.substring(0, maxlength - abbrevlength) + STRING_ABBREV;
        }


    }

    public static String pluralizedVal(int value, String word) {
        return value + " " + pluralizedWord(value, word);
    }

    public static String pluralizedWord(int value, String word) {
        return word + (value != 1 ? (word.endsWith("s") ? "es" : "s") : "");
    }

    public static NumberFormat getPcFormat() {
        DecimalFormat format = new DecimalFormat("###%");
        format.setMultiplier(1);
        return format;
    }

}
