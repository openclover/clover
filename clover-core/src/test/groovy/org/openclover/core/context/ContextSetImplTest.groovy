package org.openclover.core.context

import org.junit.Test
import org.openclover.core.api.registry.ContextSet

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.openclover.core.util.Maps.newHashMap

class ContextSetImplTest {

    @Test
    void testImmutable() {
        ContextSet context = new ContextSetImpl().set(0).set(1).set(2)
        context.set(3)

        assertTrue(context.get(0))
        assertTrue(context.get(1))
        assertTrue(context.get(2))
        assertFalse(context.get(3))

        context.and(new ContextSetImpl().set(2).set(4))

        assertTrue(context.get(0))
        assertTrue(context.get(1))
        assertTrue(context.get(2))
        assertFalse(context.get(3))
    }

    @Test
    void testSetGet() { 
        assertTrue(new ContextSetImpl().set(0).get(0))
        assertFalse(new ContextSetImpl().set(1).get(0))
    }

    @Test
    void testCopyCtor() {
        assertTrue(new ContextSetImpl(new ContextSetImpl().set(0).set(92)).get(0))
        assertTrue(new ContextSetImpl(new ContextSetImpl().set(0).set(92)).get(92))
    }

    @Test
    void testIntersects() {
        assertTrue(new ContextSetImpl().set(0).intersects(new ContextSetImpl().set(0).set(92)))
        assertTrue(new ContextSetImpl().set(92).intersects(new ContextSetImpl().set(0).set(92)))
        assertFalse(new ContextSetImpl().set(1).intersects(new ContextSetImpl().set(0).set(92)))
    }

    @Test
    void testNextBitSet() {
        assertEquals(92, new ContextSetImpl().set(92).nextSetBit(0))
        assertEquals(0, new ContextSetImpl().set(0).set(92).nextSetBit(0))
        assertEquals(-1, new ContextSetImpl().nextSetBit(0))
    }

    @Test
    void testClear() {
        assertFalse(new ContextSetImpl().set(0).clear(0).get(0))
    }

    @Test
    void testAndOr() {
        assertFalse(new ContextSetImpl().set(0).set(1).and(new ContextSetImpl().set(1).set(2)).get(0))
        assertTrue(new ContextSetImpl().set(0).set(1).and(new ContextSetImpl().set(1).set(2)).get(1))
        assertFalse(new ContextSetImpl().set(0).set(1).and(new ContextSetImpl().set(1).set(2)).get(2))

        assertTrue(new ContextSetImpl().set(0).set(1).or(new ContextSetImpl().set(1).set(2)).get(0))
        assertTrue(new ContextSetImpl().set(0).set(1).or(new ContextSetImpl().set(1).set(2)).get(1))
        assertTrue(new ContextSetImpl().set(0).set(1).or(new ContextSetImpl().set(1).set(2)).get(2))
        assertFalse(new ContextSetImpl().set(0).set(1).or(new ContextSetImpl().set(1).set(2)).get(3))
    }

    @Test
    void testValueSet() {
        assertTrue(new ContextSetImpl().set(0, true).get(0))
        assertFalse(new ContextSetImpl().set(0, false).get(0))
    }

    @Test
    void testFlip() {
        assertFalse(new ContextSetImpl().set(0).flip(0, 3).get(0))
        assertTrue(new ContextSetImpl().set(0).flip(0, 3).get(1))
        assertTrue(new ContextSetImpl().set(0).flip(0, 3).get(2))
        assertFalse(new ContextSetImpl().set(0).flip(0, 3).get(3))
    }

    @Test
    void testEquality() {
        final ContextSet set = new ContextSetImpl()
        assertEquals(set, set)

        assertEquals(new ContextSetImpl(), new ContextSetImpl())

        assertEquals(new ContextSetImpl().set(0), new ContextSetImpl().set(0))
        assertFalse(new ContextSetImpl().set(0).equals(new ContextSetImpl().set(1)))
        assertFalse(new ContextSetImpl().equals(new Object()))
    }

    @Test
    void testMappedBitset() {
        ContextSet src = new ContextSetImpl().set(1).set(2).set(3)
        Map<Integer, Integer> mapping = newHashMap()
        mapping.put(new Integer(1), new Integer(10))

        ContextSet dest = ContextSetImpl.remap(src, mapping)

        assertFalse(dest.get(1))
        assertFalse(dest.get(2))
        assertFalse(dest.get(3))
        assertTrue(dest.get(10))
    }
}
