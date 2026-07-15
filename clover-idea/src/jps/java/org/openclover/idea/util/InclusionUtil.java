package org.openclover.idea.util;

import org.openclover.core.util.FilterUtils;

import java.util.StringTokenizer;

/**
 * Helper methods to check a file against inclusion/exclusion patterns
 */
public class InclusionUtil {

    /**
     * Helper method, converting a delimited string into an array.
     */
    public static String[] toArray(String str, String del) {
        if (str == null) {
            return new String[0];
        }
        StringTokenizer tokens = new StringTokenizer(str, del, false);
        String[] array = new String[tokens.countTokens()];
        for (int i = 0; i < array.length; i++) {
            array[i] = tokens.nextToken();
        }
        return array;
    }

    /**
     *
     * @param source
     * @param excludes
     * @param includes
     * @param defaultValue
     * @return
     */
    public static boolean isIncluded(String source, String[] excludes, String[] includes, boolean defaultValue) {
        if ((excludes == null || excludes.length == 0) &&
                (includes == null || includes.length == 0)) {
            return defaultValue;
        }
        return included(source, includes) && !excluded(source, excludes);
    }

    /**
     *
     * @param source
     * @param excludes
     * @return
     */
    public static boolean excluded(String source, String[] excludes) {

        if (excludes == null || excludes.length == 0) {
            return false;
        }
        for (String exclude : excludes) {
            if (FilterUtils.matchPath(exclude, source, true)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param source
     * @param includes
     * @return
     */
    public static boolean included(String source, String[] includes) {
        if (includes == null || includes.length == 0) {
            return true;
        }
        for (String include : includes) {
            if (FilterUtils.matchPath(include, source, true)) {
                return true;
            }
        }
        return false;
    }
}
