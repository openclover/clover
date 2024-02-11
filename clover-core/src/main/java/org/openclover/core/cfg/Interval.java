package org.openclover.core.cfg;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.StringCharacterIterator;

/**
 * An interval represents a period of time.
 * <p/>
 * An interval has two components - The magnitude of the interval
 * and the units of the interval.
 * <p/>
 * The class must be configured through a static method to define
 * the acceptable units, the base unit and the relative size of the
 * units.
 */
public class Interval {
    /** BigDecimal value of 0.5 used to decide when to change units */
    static private final BigDecimal POINT_FIVE = new BigDecimal(0.5);

    /** Unit for Seconds */
    static public final int UNIT_SECOND = 0;

    /** Unit for Minutes */
    static public final int UNIT_MINUTE = 1;

    /** Unit for Hours */
    static public final int UNIT_HOUR = 2;

    /** Unit for Days */
    static public final int UNIT_DAY = 3;

    /** Unit for Weeks */
    static public final int UNIT_WEEK = 4;

    /** Unit for Months */
    static public final int UNIT_MONTH = 5;

    /** Unit for Years */
    static public final int UNIT_YEAR = 6;

    static public final Interval ZERO_SECONDS = new Interval(0, Interval.UNIT_SECOND);

    static public final Interval DEFAULT_SPAN = new Interval(Integer.MAX_VALUE, Interval.UNIT_SECOND);

    private static final int [] UNITS = {UNIT_SECOND, UNIT_MINUTE, UNIT_HOUR, UNIT_DAY,
                                        UNIT_WEEK, UNIT_MONTH, UNIT_YEAR};


    /** A Zero interval */
    public static final Interval ZERO_INTERVAL = new Interval(0, UNIT_DAY);

    /** A set of multipliers to convert units to the next size unit */
    static private final BigDecimal[] MULTIPLIERS = {BigDecimal.valueOf(1),
                                                     BigDecimal.valueOf(60),
                                                     BigDecimal.valueOf(60),
                                                     BigDecimal.valueOf(24),
                                                     BigDecimal.valueOf(7),
                                                     new BigDecimal(4.348),
                                                     BigDecimal.valueOf(12)};

    /** The names of all the units understood by intervals. */
    static private final String[] NAMES = {"second", "minute", "hour", "day", "week", "month", "year"};

    /** The magnitude of this interval */
    private BigDecimal magnitude = null;

    /** The unit for this interval - defaults to 'days'*/
    private int unit = UNIT_DAY;

    /**
     * Create an interval from a magnitude and a unit.
     *
     * @param magnitude the interval's desired magnitude.
     * @param unit the desired unit for this interval.
     */
    public Interval(BigDecimal magnitude, int unit) {
        this.magnitude = magnitude;
        this.unit = unit;
    }

    /**
     * Create an interval from a magnitude and a unit.
     *
     * @param magnitude the interval's desired magnitude.
     * @param unit the desired unit for this interval.
     */
    public Interval(long magnitude, int unit) {
        this(BigDecimal.valueOf(magnitude), unit);
    }

    /**
     * Create an interval from a magnitude and a unit.
     *
     * @param magnitude the interval's desired magnitude.
     * @param unit the desired unit for this interval.
     */
    public Interval(double magnitude, int unit) {
        this(new BigDecimal(magnitude), unit);
    }

    /**
     * Constructor for an Interval that takes a default time unit.
     * @param timeDescription a string rep of the time interval
     * @param defaultUnit the unit to default to if <code>timeDescription</code>
     * contains no unit
     * @throws NumberFormatException if not a valid time
     */
    public Interval(String timeDescription, int defaultUnit) {
        unit = defaultUnit;
        initialise(timeDescription);
    }

    /**
     * Create an interval from a string description.
     *
     * @param timeDescription The interval description consisting of a number, the magnitude
     *                        of the interval, and a word describing the unit.
     * @throws NumberFormatException if not a valid time
     */
    public Interval(String timeDescription) {
        initialise(timeDescription);
    }

    /**
     * Initialises an interval from a string description.
     *
     * @param timeDescription The interval description consisting of a number, the magnitude
     *                        of the interval, and a word describing the unit.
     * @throws NumberFormatException if not a valid time
     */
    private void initialise(String timeDescription) {
        // find the first non-space, non digit character.
        int unitIndex = -1;
        boolean digitSeen = false;
        StringCharacterIterator sci = new StringCharacterIterator(timeDescription.toLowerCase());
        for (char c = sci.first(); c != StringCharacterIterator.DONE; c = sci.next()) {
            if (Character.isDigit(c)) {
                digitSeen = true;
            }
            else if (Character.isWhitespace(c) || c == '.'
                     || (c == '-' && !digitSeen)) {
                continue;
            }
            else {
                switch (c) {
                    case 's':
                        unit = UNIT_SECOND;
                        break;
                    case 'm':
                        unit = UNIT_MINUTE;
                        int index = sci.getIndex() + 1;
                        if (index != timeDescription.length() &&
                                timeDescription.charAt(index) == 'o') {
                            unit = UNIT_MONTH;
                        }
                        break;
                    case 'h':
                        unit = UNIT_HOUR;
                        break;
                    case 'd':
                        unit = UNIT_DAY;
                        break;
                    case 'w':
                        unit = UNIT_WEEK;
                        break;
                    case 'y':
                        unit = UNIT_YEAR;
                        break;
                }
                unitIndex = sci.getIndex();
                break;
            }
        }

        String amountString = unitIndex == -1 ? timeDescription.trim()
                                              : timeDescription.substring(0, unitIndex).trim();


        if (amountString.length() == 0) {
            throw new NumberFormatException("Intervals must have a magnitude - not found in '" + timeDescription + "'");
        }

        magnitude = new BigDecimal(amountString.trim());
    }

    /**
     * Get the magnitude of this interval in the given unit.
     *
     * @param desiredUnit the unit in which the interval's magnitude is desired.
     * @return the magnitude of the interval in the given unit.
     */
    public BigDecimal getValueInUnits(int desiredUnit) {
        BigDecimal adjustedMagnitude = magnitude;
        if (desiredUnit < unit) {
            // we will be multiplying
            for (int i = unit; i != desiredUnit; --i) {
                adjustedMagnitude = adjustedMagnitude.multiply(MULTIPLIERS[i]);
            }
        }
        else if (desiredUnit > unit) {
            // dividing
            for (int i = desiredUnit; i != unit; --i) {
                adjustedMagnitude = adjustedMagnitude.divide(MULTIPLIERS[i],
                                                            2, RoundingMode.HALF_UP);
            }
        }

        return adjustedMagnitude;
    }

    /**
     * @return the interval in milliseconds. No overflow checks are done.
     */
    public long getValueInMillis()
    {
        BigDecimal secs = getValueInUnits(UNIT_SECOND);
        return secs.longValue() * 1000L;
    }


    /**
     * Convert the interval to a string in the given unit
     *
     * @param desiredUnit the unit in which the intervale is to be expressed.
     *
     * @return the string representation of the interval in the given unit.
     */
    public String toString(int desiredUnit) {
        BigDecimal adjustedMag = getValueInUnits(desiredUnit);
        String unitName = NAMES[desiredUnit];
        String description = adjustedMag.toString() + " "
                             + unitName + (adjustedMag.compareTo(BigDecimal.valueOf(1)) == 0 ? "" : "s");
        return description;
    }

    public String toIntString(int desiredUnit) {
        BigDecimal adjustedMag = getValueInUnits(desiredUnit);
        String unitName = NAMES[desiredUnit];
        String description = adjustedMag.longValue() + " "
                             + unitName + (adjustedMag.compareTo(BigDecimal.valueOf(1)) == 0 ? "" : "s");
        return description;
    }

    /**
     * Convert this interval to its string representation.
     *
     * @see Object#toString
     */
    public String toString() {
        return toString(unit);
    }

    /**
     * Convert to an string representation, choosing a sensible
     * unit depending on the value of the interval
     */
    public String toSensibleString()
    {
        for (int i = UNITS.length-1; i >= 0; i--) {
            BigDecimal val = getValueInUnits(UNITS[i]);
            val.setScale(0, RoundingMode.HALF_UP);
            if (val.longValue() >= 1) {
                String description = val.longValue() + " "
                    + NAMES[i] + (val.intValue() == 1 ? "" : "s");
                return description;
            }
        }
        return toString();
    }

    /**
     * Get the unit for this interval.
     *
     * @return the int representing the unit.
     */
    public int getUnit() {
        return unit;
    }

    /**
     * Get the magnitude of this interval
     *
     * @return the magnitude of this interval - regardless of units
     */
    public BigDecimal getMagnitude() {
        return magnitude;
    }

    /**
     * Compare two intervals - This will normalize the intervals to the same base unit and
     * compare them
     *
     * @param rhs the interval to be compared to this one
     *
     * @return -1, 0 or 1 indicating whether this intervale is less than, equal or greater than the
     *         given interval
     */
    public int compareTo(Interval rhs) {
        int baseUnit = unit;
        if (baseUnit > rhs.unit) {
            baseUnit = rhs.unit;
        }

        // convert to a common base
        BigDecimal rhsMagnitude = rhs.getValueInUnits(baseUnit);
        BigDecimal lhsMagnitude = getValueInUnits(baseUnit);
        return lhsMagnitude.compareTo(rhsMagnitude);
    }

    /**
     * Check this interval for equality to another object.
     * This will only return true if the two intervals have the same magntiude
     * and units. This includes the scales of the interval magnitudes.
     *
     * @see Object#equals
     */
    public boolean equals(Object rhs) {
        if (!(rhs instanceof Interval)) {
            return false;
        }
        Interval rhsInterval = (Interval)rhs;

        return magnitude.equals(rhsInterval.magnitude) &&
               unit == rhsInterval.unit;
    }

    /**
     * Generate the hash code for this interval
     *
     * @see Object#hashCode
     */
    public int hashCode() {
        return magnitude.hashCode() + unit;
    }

    /**
     * Subtract the given interval from this interval to produce a new interval.
     * The units of this interval are preferred to that of the rhs. If the magnitude
     * of the result drops below 0.5, the smaller unit is then used.
     *
     * @param rhs the interval to be subracted from this interval.
     *
     * @return the result of subtracting the rhs from this interval.
     */
    public Interval subtract(Interval rhs) {
        // we firstly convert the values to the smallest unit -> bigger magnitudes
        int smallUnit = unit;
        int bigUnit = rhs.unit;
        if (bigUnit < smallUnit) {
            bigUnit = unit;
            smallUnit = rhs.unit;
        }

        BigDecimal lhsMagnitude = getValueInUnits(smallUnit);
        BigDecimal rhsMagnitude = rhs.getValueInUnits(smallUnit);

        Interval result
            = new Interval(lhsMagnitude.subtract(rhsMagnitude), smallUnit);
        // we prefer to the units of the lhs
        BigDecimal lhsUnitMagnitude
            = result.getValueInUnits(unit);
        if (lhsUnitMagnitude.abs().compareTo(POINT_FIVE) >= 0) {
            result = new Interval(lhsUnitMagnitude, unit);
        }

        return result;
    }

    /**
     * Add the given interval to this interval producing a new interval.
     * The time unit's of this interval is retained in the result.
     *
     * @param rhs the Right Hand Side of the addtion
     *
     * @return an Intervale object being the addition of this interval and the rhs interval.
     */
    public Interval add(Interval rhs) {
        // we firstly convert the values to the smallest unit -> bigger magnitudes
        int smallUnit = unit;
        int bigUnit = rhs.unit;
        if (bigUnit < smallUnit) {
            bigUnit = unit;
            smallUnit = rhs.unit;
        }

        BigDecimal lhsMagnitude = getValueInUnits(smallUnit);
        BigDecimal rhsMagnitude = rhs.getValueInUnits(smallUnit);

        Interval result
            = new Interval(lhsMagnitude.add(rhsMagnitude), smallUnit);
        // we prefer to the units of the lhs
        BigDecimal lhsUnitMagnitude
            = result.getValueInUnits(unit);
        result = new Interval(lhsUnitMagnitude, unit);

        return result;
    }

    /**
     * Indicate the sign of the Interval' magnitude component
     *
     * @return -1, 0, 1 indicating the sign of the magnitude
     */
    public int signum() {
        return magnitude.signum();
    }
}
