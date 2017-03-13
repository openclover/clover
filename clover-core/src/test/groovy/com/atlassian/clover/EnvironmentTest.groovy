package com.atlassian.clover

import junit.framework.TestCase

class EnvironmentTest extends TestCase {
    void testSystemPropertySubstitution() {
        System.setProperty("clover.test.foo", "bar")
        assertEquals("bar", Environment.substituteSysPropRefs('${clover.test.foo}'))
        assertEquals("foobarbaz", Environment.substituteSysPropRefs('foo${clover.test.foo}baz'))
    }

    void testUnsetSystemPropertySubstitution() {
        assertEquals('${clover.test.foo1}', Environment.substituteSysPropRefs('${clover.test.foo1}'))
    }

    void testNoSystemProperty() {
        assertEquals("bar", Environment.substituteSysPropRefs("bar"))
    }

    void testUnclosedSystemPropertySubstitution() {
        assertEquals('${clover.test.foo1', Environment.substituteSysPropRefs('${clover.test.foo1'))
    }

    void testDollarButNoRef() {
        assertEquals('$', Environment.substituteSysPropRefs('$'))
        assertEquals('$foo', Environment.substituteSysPropRefs('$foo'))
        assertEquals('foo$', Environment.substituteSysPropRefs('foo$'))
    }

    void testNullValue() {
        assertNull(Environment.substituteSysPropRefs(null))
    }
}
