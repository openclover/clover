package org.openclover.ci

import org.junit.Test
import org.openclover.core.util.function.Predicate

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Test for {@link org.openclover.ci.HasPropertyPredicate}
 */
class HasPropertyPredicateTest {

    @Test
    void testHasPropertyPredicateWithD() {
        Predicate<String> predicate = new HasPropertyPredicate("abc")
        assertFalse(predicate.test("abc"))       // does not have -D
        assertFalse(predicate.test("-Dabczzz"))  // it has a wrong suffix
        assertFalse(predicate.test("-Dabczzz=")) // it has a wrong suffix
        assertFalse(predicate.test("-Dzzzabc"))  // it has a wrong prefix
        assertFalse(predicate.test("-Dzzzabc=")) // it has a wrong prefix

        assertTrue(predicate.test("-Dabc")) // OK
        assertTrue(predicate.test("-Dabc=")) // OK
        assertTrue(predicate.test("-Dabc=123")) // OK
    }

    @Test
    void testHasPropertyPredicateWithDefine() {
        // not that the predicate is stateful
        Predicate<String> predicate = new HasPropertyPredicate("abc")
        assertFalse(predicate.test("abc"))       // no -D or --define before 'abc'
        assertFalse(predicate.test("--define"))  //
        assertTrue(predicate.test("abc"))        // --define before 'abc'
        assertFalse(predicate.test("abc"))       // --define not before 'abc'
        assertTrue(predicate.test("-Dabc"))      // -D before 'abc'
        assertFalse(predicate.test("-D"))
        assertTrue(predicate.test("abc"))       // -D abc
    }
}
