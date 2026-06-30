package org.openclover.eclipse.test;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestHelloWorld {

    @Test
    public void testGreet() {
        assertEquals("Hello, World!", HelloWorld.greet("  World  "));
    }

    @Test
    public void testFilterNonBlanks() {
        List<String> result = HelloWorld.filterNonBlanks(Arrays.asList("a", "  ", "b", ""));
        assertEquals(Arrays.asList("a", "b"), result);
    }
}
