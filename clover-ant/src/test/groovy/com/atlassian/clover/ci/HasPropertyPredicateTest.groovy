package com.atlassian.clover.ci

import clover.com.google.common.base.Predicate
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Test for {@link HasPropertyPredicate}
 */
class HasPropertyPredicateTest {

    @Test
    public void testHasPropertyPredicateWithD() {
        Predicate<String> predicate = new HasPropertyPredicate("abc")
        assertFalse(predicate.apply("abc"))       // does not have -D
        assertFalse(predicate.apply("-Dabczzz"))  // it has a wrong suffix
        assertFalse(predicate.apply("-Dabczzz=")) // it has a wrong suffix
        assertFalse(predicate.apply("-Dzzzabc"))  // it has a wrong prefix
        assertFalse(predicate.apply("-Dzzzabc=")) // it has a wrong prefix

        assertTrue(predicate.apply("-Dabc")) // OK
        assertTrue(predicate.apply("-Dabc=")) // OK
        assertTrue(predicate.apply("-Dabc=123")) // OK
    }

    @Test
    public void testHasPropertyPredicateWithDefine() {
        // not that the predicate is stateful
        Predicate<String> predicate = new HasPropertyPredicate("abc")
        assertFalse(predicate.apply("abc"))       // no -D or --define before 'abc'
        assertFalse(predicate.apply("--define"))  //
        assertTrue(predicate.apply("abc"))        // --define before 'abc'
        assertFalse(predicate.apply("abc"))       // --define not before 'abc'
        assertTrue(predicate.apply("-Dabc"))      // -D before 'abc'
        assertFalse(predicate.apply("-D"))
        assertTrue(predicate.apply("abc"))       // -D abc
    }
}
