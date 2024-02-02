package org.openclover.idea.util;

import java.util.List;

public class ComparatorUtil {

    private ComparatorUtil() {
    }

    /**
     * Compare nodes assuming that null is smaller then non-null
     *
     * @param arg1 first argument
     * @param arg2 second argument
     * @return Comparison result
     */
    public static <T> int compare(Comparable<T> arg1, T arg2) {
        if (arg1 == null) {
            return arg2 == null ? 0 : -1;
        } else {
            return arg2 == null ? 1 : arg1.compareTo(arg2);
        }
    }

    /**
     * Compare nodes assuming that null is equal to anything.<p>
     * Use when you don't want null values to be moved.
     *
     * @param arg1 first argument
     * @param arg2 second argument
     * @return Comparison result
     */
    public static <T> int compareNE(Comparable<T> arg1, T arg2) {
        return (arg1 == null || arg2 == null) ? 0 : arg1.compareTo(arg2);
    }

    /**
     * Compare long values without creating a Long instance.
     *
     * @param arg1 arg1
     * @param arg2 arg2
     * @return Comparison result
     */
    public static int compareLong(long arg1, long arg2) {
        return Long.compare(arg1, arg2);
    }

    public static boolean areEqual(Object obj, Object obj1) {
        if (obj == obj1) {
            return true;
        }
        if (obj == null || obj1 == null) {
            return false;
        }
        return obj.equals(obj1);
    }

    public static boolean areEqual(List a, List b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!areEqual(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }


    public static boolean areDifferent(Object obj, Object obj1) {
        return !areEqual(obj, obj1);
    }

}
