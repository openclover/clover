package org.openclover.core.util

import org.junit.Test
import org.openclover.runtime.util.CloverBitSet

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

public class CloverBitSetTest {
    
    @Test
    public void testCanReadAndWrite() throws IOException {
        CloverBitSet bs = new CloverBitSet()
        Random random = new Random()
        for(int i = 0; i < 10000; i++) {
            if (random.nextBoolean()) {
                bs.add(i)
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        DataOutputStream daos = new DataOutputStream(baos)
        bs.write(daos)
        daos.flush()

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())
        DataInputStream dis = new DataInputStream(bais)
        CloverBitSet newBs = CloverBitSet.read(dis)

        assertEquals(bs, newBs)
    }

    @Test
    public void testCanApplyToJavaUtilBitset() {
        //> long # bits count
        final int length = 2 * 64
        CloverBitSet bitSet = (CloverBitSet)new CloverBitSet(length).not()

        BitSet javaUtilBitset = new BitSet(length)
        bitSet.applyTo(javaUtilBitset)

        for (int i = 0; i < length; i++) {
            assertEquals(bitSet.member(i), javaUtilBitset.get(i))
        }
    }

    @Test
    public void testNextSetBit() {
        CloverBitSet bs = new CloverBitSet()
        bs.add(1000)
        assertEquals(bs.nextSetBit(0), 1000)

        bs = new CloverBitSet()
        for(int i = 0; i < 1000; i += 2) {
            bs.add(i)
        }
        for(int i = 0; i < 1000 - 2; i += 2) {
            assertEquals(bs.nextSetBit(i), i)
            assertEquals(bs.nextSetBit(i + 1), i + 2)
        }

        bs = new CloverBitSet()
        assertEquals(bs.nextSetBit(0), -1)
    }

    @Test
    public void testNegativeNextSetBit() {
        try {
            new CloverBitSet().nextSetBit(-1)
            fail()
        } catch (IndexOutOfBoundsException e) {
            //pass
        }
    }

    @Test
    public void testLength() {
        CloverBitSet bs = new CloverBitSet()
        assertEquals(0, bs.length())
        bs.add(1)
        assertEquals(2, bs.length())
        bs.add(1000)
        assertEquals(1001, bs.length())
    }

    @Test
    public void testSingleRowHits() {
        final int[] hits = [0, 1, 0, 2, 2, 0]
        final CloverBitSet bs = CloverBitSet.forHits([ hits ] as int[][])

        assertFalse(bs.member(0))
        assertTrue(bs.member(1))
        assertFalse(bs.member(2))
        assertTrue(bs.member(3))
        assertTrue(bs.member(4))
        assertFalse(bs.member(5))
    }

    @Test
    public void testMultiRowHits() {
        final int[] hits = [0, 1, 0, 2, 2, 0]
        final int[] hits2 = [1, 1, 0, 2, 0, 0]
        final CloverBitSet bs = CloverBitSet.forHits([ hits, hits2 ] as int[][])

        assertFalse(bs.member(0))
        assertTrue(bs.member(1))
        assertFalse(bs.member(2))
        assertTrue(bs.member(3))
        assertTrue(bs.member(4))
        assertFalse(bs.member(5))

        assertTrue(bs.member(6))
        assertTrue(bs.member(7))
        assertFalse(bs.member(8))
        assertTrue(bs.member(9))
        assertFalse(bs.member(10))
        assertFalse(bs.member(11))
    }

    @Test
    public void testNoRowHits() {
        final CloverBitSet bs = CloverBitSet.forHits(new int[0][0])
        assertEquals(0, bs.degree())
    }

    @Test
    public void testTruncatedMultiRowHits() {
        final int[] hits = [0, 1, 0, 2, 2, 0]
        final int[] hits2 = [1, 1, 0, 2, 0, 3]
        final CloverBitSet bs = CloverBitSet.forHits([ hits, hits2 ] as int[][], 11)

        assertFalse(bs.member(0))
        assertTrue(bs.member(1))
        assertFalse(bs.member(2))
        assertTrue(bs.member(3))
        assertTrue(bs.member(4))
        assertFalse(bs.member(5))

        assertTrue(bs.member(6))
        assertTrue(bs.member(7))
        assertFalse(bs.member(8))
        assertTrue(bs.member(9))
        assertFalse(bs.member(10))
        assertFalse(bs.member(11))
    }

    @Test
    public void testSizeInCache() {
        final CloverBitSet bs = new CloverBitSet(1000)
        assertEquals((int)Math.ceil(1000f / 64f) * 8, bs.sizeInBytes())
    }
}
