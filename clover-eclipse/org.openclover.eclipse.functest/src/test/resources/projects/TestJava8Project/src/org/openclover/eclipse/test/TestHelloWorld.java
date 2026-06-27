package org.openclover.eclipse.test;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestHelloWorld {

    @Test
    public void testGreet() {
        List<String> result = HelloWorld.greet(Arrays.asList("World", "", "Java"));
        assertEquals(Arrays.asList("Hello, World!", "Hello, Java!"), result);
    }
}
