package org.openclover.core.context

import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 *
 */
class MethodRegexpContextTest {

    @Test
    void testMatches() throws Exception {
        MethodRegexpContext regexpContext = new MethodRegexpContext("test1",
                Pattern.compile('^(?!.*(getRunnable|getListener)).*$'))
        assertFalse(regexpContext.matches("public Runnable getRunnable() {"))
        assertFalse(regexpContext.matches("public Runnable getRunnableXyz() {"));
        assertFalse(regexpContext.matches("ActionListener getListener()"))
        assertFalse(regexpContext.matches("getListener"))
        assertTrue(regexpContext.matches("public int notFiltered() {"));
    }

    @Test
    void testIsEquivalent() throws Exception {
        Pattern pattern = Pattern.compile('^(?!.*(getRunnable|getListener)).*$')
        MethodRegexpContext regexpContext1 = new MethodRegexpContext("test1", pattern, 10, 20)

        // different pattern name but same attributes = equivalent
        MethodRegexpContext regexpContext2 = new MethodRegexpContext("test2", pattern, 10, 20)
        assertTrue(regexpContext1.isEquivalent(regexpContext2))

        // sub-class should also match
        MethodRegexpContext regexpSubClass = new MethodRegexpContext("regexpSubClass", pattern, 10, 20) {
            void foo() { }
        }
        assertTrue(regexpContext1.isEquivalent(regexpSubClass))

        // common interface is not enough
        MethodRegexpContext regexpContext3 = new MethodRegexpContext("test3", pattern)
        RegexpContext regexpCommonInterface = new RegexpContext(1, "regexpCommonInterface", pattern) {

        }
        assertFalse(regexpContext3.isEquivalent(regexpCommonInterface))

    }
}
