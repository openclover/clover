package org.openclover.eclipse.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestHelloWorld {

    @Test
    public void testDescribe() {
        assertEquals("int:42", HelloWorld.describe(42));
        assertEquals("str:hi", HelloWorld.describe("hi"));
        assertEquals("point:(1,2)", HelloWorld.describe(new HelloWorld.Point(1, 2)));
        assertEquals("null", HelloWorld.describe(null));
        assertEquals("other", HelloWorld.describe(3.14));
    }
}
