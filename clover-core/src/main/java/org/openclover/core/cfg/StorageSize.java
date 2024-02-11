package org.openclover.core.cfg;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;

import static org.openclover.core.util.Maps.newHashMap;

public class StorageSize {
    private static Pattern SIZE_REGEX = Pattern.compile("(0|([1-9][0-9]*))([bkmg])?",Pattern.CASE_INSENSITIVE);
    private static Map<String, Long> MULTIPLIERS = newHashMap();
    public static final StorageSize ZERO = new StorageSize(0);
    public static final StorageSize MAX = new StorageSize(Long.MAX_VALUE);

    static {
        MULTIPLIERS.put("b", 1L);
        MULTIPLIERS.put("k", 1000L);
        MULTIPLIERS.put("m", 1000000L);
        MULTIPLIERS.put("g", 1000000000L);
    }

    private long sizeInBytes;

    public StorageSize(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public static StorageSize fromString(String s) {
        Matcher m = SIZE_REGEX.matcher(s);
        if (m.matches()) {
            try {
                long value = Long.parseLong(m.group(1));
                String unit = m.group(3);
                if (unit != null) {
                    Long mult = MULTIPLIERS.get(unit.toLowerCase());
                    if (mult != null) {
                        return new StorageSize(value * mult);
                    }
                } else {
                    return new StorageSize(value);
                }
            }
            catch (NumberFormatException e) {
                // technically shouldn't happen, fall thru to throw below
            }

        }
        throw new IllegalArgumentException("Couldn't parse size string \""+s+"\"");
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorageSize that = (StorageSize) o;
        return sizeInBytes == that.sizeInBytes;
    }

    @Override
    public int hashCode() {
        return (int) (sizeInBytes ^ (sizeInBytes >>> 32));
    }
}
