package org.openclover.core.util

import org.junit.Test

import java.util.function.Function

import static org.junit.Assert.assertArrayEquals

/**
 * Test for {@link ArrayUtil}
 */
class ArrayUtilTest {
    static enum MyEnum {
        ABC,
        DEF
    }

    @Test
    void testToStringArray() throws Exception {
        assertArrayEquals(["ABC", "DEF"] as String[], ArrayUtil.toStringArray(MyEnum.values()))
    }

    @Test
    void testTransformArray() throws Exception {
        String[] input = [ "Hello", "World!" ]
        Integer[] output = ArrayUtil.transformArray(input, new Function<String, Integer>() {
            Integer apply(String s) {
                return s.length()
            }

            @Override
            boolean equals(Object o) {
                false
            }
        }, Integer.class)
        Integer[] expectedOutput = [ Integer.valueOf(5), Integer.valueOf(6) ]

        assertArrayEquals(expectedOutput, output)
    }
}
