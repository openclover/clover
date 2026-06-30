package org.openclover.eclipse.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestHelloWorld {

    @Test
    public void testGreet() {
        assertEquals("Hello, World!", HelloWorld.greet("World"));
    }

    @Test
    public void testArea() {
        assertEquals(Math.PI * 4, HelloWorld.area(new HelloWorld.Circle(2)), 1e-9);
        assertEquals(6.0, HelloWorld.area(new HelloWorld.Rectangle(2, 3)), 1e-9);
    }
}
