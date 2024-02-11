package org.openclover.core.context

import com.atlassian.clover.context.ContextSet
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.openclover.util.Maps.newHashMap

class ContextSetTest {

    @Test
    void testImmutable() {
        ContextSet context = new ContextSet().set(0).set(1).set(2)
        context.set(3)

        assertTrue(context.get(0))
        assertTrue(context.get(1))
        assertTrue(context.get(2))
        assertFalse(context.get(3))

        context.and(new ContextSet().set(2).set(4))

        assertTrue(context.get(0))
        assertTrue(context.get(1))
        assertTrue(context.get(2))
        assertFalse(context.get(3))
    }

    @Test
    void testSetGet() { 
        assertTrue(new ContextSet().set(0).get(0))
        assertFalse(new ContextSet().set(1).get(0))
    }

    @Test
    void testCopyCtor() {
        assertTrue(new ContextSet(new ContextSet().set(0).set(92)).get(0))
        assertTrue(new ContextSet(new ContextSet().set(0).set(92)).get(92))
    }

    @Test
    void testIntersects() {
        assertTrue(new ContextSet().set(0).intersects(new ContextSet().set(0).set(92)))
        assertTrue(new ContextSet().set(92).intersects(new ContextSet().set(0).set(92)))
        assertFalse(new ContextSet().set(1).intersects(new ContextSet().set(0).set(92)))
    }

    @Test
    void testNextBitSet() {
        assertEquals(92, new ContextSet().set(92).nextSetBit(0))
        assertEquals(0, new ContextSet().set(0).set(92).nextSetBit(0))
        assertEquals(-1, new ContextSet().nextSetBit(0))
    }

    @Test
    void testClear() {
        assertFalse(new ContextSet().set(0).clear(0).get(0))
    }

    @Test
    void testAndOr() {
        assertFalse(new ContextSet().set(0).set(1).and(new ContextSet().set(1).set(2)).get(0))
        assertTrue(new ContextSet().set(0).set(1).and(new ContextSet().set(1).set(2)).get(1))
        assertFalse(new ContextSet().set(0).set(1).and(new ContextSet().set(1).set(2)).get(2))

        assertTrue(new ContextSet().set(0).set(1).or(new ContextSet().set(1).set(2)).get(0))
        assertTrue(new ContextSet().set(0).set(1).or(new ContextSet().set(1).set(2)).get(1))
        assertTrue(new ContextSet().set(0).set(1).or(new ContextSet().set(1).set(2)).get(2))
        assertFalse(new ContextSet().set(0).set(1).or(new ContextSet().set(1).set(2)).get(3))
    }

    @Test
    void testValueSet() {
        assertTrue(new ContextSet().set(0, true).get(0))
        assertFalse(new ContextSet().set(0, false).get(0))
    }

    @Test
    void testFlip() {
        assertFalse(new ContextSet().set(0).flip(0, 3).get(0))
        assertTrue(new ContextSet().set(0).flip(0, 3).get(1))
        assertTrue(new ContextSet().set(0).flip(0, 3).get(2))
        assertFalse(new ContextSet().set(0).flip(0, 3).get(3))
    }

    @Test
    void testEquality() {
        final ContextSet set = new ContextSet()
        assertEquals(set, set)

        assertEquals(new ContextSet(), new ContextSet())

        assertEquals(new ContextSet().set(0), new ContextSet().set(0))
        assertFalse(new ContextSet().set(0).equals(new ContextSet().set(1)))
        assertFalse(new ContextSet().equals(new Object()))
    }

    @Test
    void testMappedBitset() {
        ContextSet src = new ContextSet().set(1).set(2).set(3)
        Map<Integer, Integer> mapping = newHashMap()
        mapping.put(new Integer(1), new Integer(10))

        ContextSet dest = ContextSet.remap(src, mapping)

        assertFalse(dest.get(1))
        assertFalse(dest.get(2))
        assertFalse(dest.get(3))
        assertTrue(dest.get(10))
    }
}
