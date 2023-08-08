package com.atlassian.clover.util;

import org.openclover.util.function.Function;

import java.lang.reflect.Array;
import java.util.Locale;

/**
 * Helper class to transform raw arrays.
 */
public class ArrayUtil {

    private ArrayUtil() {

    }

    /**
     * Convert <code>input</code> array into an array of strings by calling {@link Object#toString()} and {@link
     * String#toLowerCase()} methods on each array element.
     *
     * @param input input array
     * @return String[] output by calling toString().toLowerCase
     */
    public static String[] toLowerCaseStringArray(Object[] input) {
        return transformArray(input, new ToLowercaseStringFunction(), String.class);
    }

    /**
     * Convert <code>input</code> array into an array of strings by calling {@link Object#toString()} method on each
     * array element.
     *
     * @param input input array
     * @return String[] output by calling toString() on every element
     */
    public static String[] toStringArray(Object[] input) {
        return transformArray(input, new ToStringFunction(), String.class);
    }

    /**
     * Convert <code>input</code> array of type F into an output array of type T using the <code>transformer</code>
     * function.
     *
     * @param input       input array
     * @param transformer conversion
     * @param targetClass type of the target array
     * @param <F>         from type
     * @param <T>         to type
     * @return T[] an array of type targetClass
     */
    @SuppressWarnings("unchecked")
    public static <F, T> T[] transformArray(F[] input, Function<F, T> transformer, Class<T> targetClass) {
        final T[] output = (T[]) Array.newInstance(targetClass, input.length);
        for (int i = 0; i < input.length; i++) {
            output[i] = transformer.apply(input[i]);
        }
        return output;
    }

    private static class ToStringFunction implements Function<Object, String> {
        @Override
        public String apply(Object o) {
            return o.toString();
        }
    }

    private static class ToLowercaseStringFunction implements Function<Object, String> {
        @Override
        public String apply(Object o) {
            return o.toString().toLowerCase(Locale.ENGLISH);
        }
    }

}
