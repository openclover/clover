package com.atlassian.clover

import junit.framework.TestCase
import org.openclover.runtime.RuntimeType

class RuntimeTypeTest extends TestCase {
    void testIdentityEquals() {
        RuntimeType type = new RuntimeType(Object.class.getName())
        assertEquals(type, type)
    }

    void testIdentityHashcodeEquals() {
        RuntimeType type = new RuntimeType(Object.class.getName())
        assertEquals(type.hashCode(), type.hashCode())
    }

    void testInequality() {
        RuntimeType type1 = new RuntimeType(Object.class.getName())
        RuntimeType type2 = new RuntimeType(String.class.getName())
        assertFalse(type1.equals(type2))
    }

    void testMatchingOriginalClass() {
        RuntimeType type = new RuntimeType(Object.class.getName())
        assertTrue(type.matches(Object.class.getName()))
    }
}
