package org.openclover.runtime.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.BitSet;

public class CloverBitSet extends clover.antlr.collections.impl.BitSet implements ByteSized {
    private final static int BITS_PER_UNIT = 1 << LOG_BITS;
    private final static int BIT_INDEX_MASK = BITS_PER_UNIT - 1;
    private final static byte[] END_ZERO_TABLE = {
      -25, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        6, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        7, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        6, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
        4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
    };

    /**
     * Keeps track if any modification of the bit set was <b>possibly</b> made via any of {@link #add(int)}, {@link #remove(int)},
     * {@link #andInPlace(clover.antlr.collections.impl.BitSet)}, {@link #orInPlace(clover.antlr.collections.impl.BitSet)},
     * {@link #notInPlace()}, {@link #notInPlace(int)}, {@link #notInPlace(int, int)} methods. Note it does not check
     * if content was really changed, but whether the method was called.
     */
    protected volatile boolean modified = false;

    public CloverBitSet() {}

    public CloverBitSet(long[] bits) {
        super(bits);
    }

    public CloverBitSet(int nbits) {
        super(nbits);
    }

    public long[] getBits() {
        return bits;
    }

    public BitSet applyTo(BitSet to) {
        //Clover-centric optimisation (possibly naive?) that most per-test coverage data will be 0
        long[] masks = getBits();
        for (int i = 0; i < masks.length; i++) {
            long mask = masks[i];
            if (mask != 0) {
                int longOffset = 64 * i;
                for (int j = 0; j < 64; j++) {
                    long twiddle = 1L << j;
                    if ((mask & twiddle) == twiddle) {
                        to.set(longOffset + j);
                    }
                }
            }
        }
        return to;
    }

    public void write(DataOutput out) throws IOException {
        out.writeInt(bits.length);
        for (long bit : bits) {
            out.writeLong(bit);
        }
    }

    public static CloverBitSet read(DataInput in) throws IOException {
        final int numLongs = in.readInt();
        final long[] data = new long[numLongs];
        for (int i = 0; i < numLongs; i++) {
            data[i] = in.readLong();
        }
        return new CloverBitSet(data);
    }

    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex must be greater than or equal to 0: " + fromIndex);
        }

        int wordIndex = fromIndex >> LOG_BITS;
        if (wordIndex >= bits.length) {
            return -1;
        } else {
            int testIndex = (fromIndex & BIT_INDEX_MASK);
            long unit = bits[wordIndex] >> testIndex;

            //If unit is all 0 bits, start at first bit index
            if (unit == 0) {
                testIndex = 0;
            }

            //cycle through units until a non-zero one is found or end is reached
            while ((unit == 0) && (wordIndex < bits.length - 1)) {
                unit = bits[++wordIndex];
            }

            //end was reached
            if (unit == 0) {
                return -1;
            } else {
                //Find first non-zero in this final index
                testIndex += countZeros(unit);

                //bits-per-unit * #units + index in last unit
                return ((wordIndex * BITS_PER_UNIT) + testIndex);
            }
        }
    }

    private static int countZeros(long val) {
        int asByte = (int)val & 0xff;
        if (asByte != 0) {
            return END_ZERO_TABLE[asByte];
        }

        asByte = (int)(val >>> 8) & 0xff;
        if (asByte != 0) {
            return END_ZERO_TABLE[asByte] + 8;
        }

        asByte = (int)(val >>> 16) & 0xff;
        if (asByte != 0) {
            return END_ZERO_TABLE[asByte] + 16;
        }

        asByte = (int)(val >>> 24) & 0xff;
        if (asByte != 0) {
            return END_ZERO_TABLE[asByte] + 24;
        }

        asByte = (int)(val >>> 32) & 0xff;
        if (asByte != 0) {
            return END_ZERO_TABLE[asByte] + 32;
        }

        asByte = (int)(val >>> 40) & 0xff;
        if (asByte != 0) {
            return END_ZERO_TABLE[asByte] + 40;
        }

        asByte = (int)(val >>> 48) & 0xff;
        if (asByte != 0) {
            return END_ZERO_TABLE[asByte] + 48;
        }

        asByte = (int)(val >>> 56) & 0xff;
        return END_ZERO_TABLE[asByte] + 56;
    }

    public static CloverBitSet forHits(int[] elements) {
        return forHits(new int[][]{elements}, elements.length);
    }

    public static CloverBitSet forHits(int[][] elements) {
        return forHits(elements, Integer.MAX_VALUE);
    }

    public static CloverBitSet forHits(int[][] elements, int maxElements) {
        CloverBitSet result = new CloverBitSet(elements.length == 0 ? 0 : elements.length * elements[0].length);
        int idx = 0;
        enough:
        {
            for (final int[] sections : elements) {
                for (int section : sections) {
                    if (section != 0) {
                        result.add(idx);
                    }
                    if (++idx >= maxElements) {
                        break enough;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public long sizeInBytes() {
        //8 bytes in a long
        return bits.length << 3;
    }

    public boolean intersects(CloverBitSet other) {
        final long[] otherBits = other.bits;
        final long[] bits = this.bits;

        int reps = Math.min(bits.length, otherBits.length);
        for (int i = 0; i < reps; i++) {
            if ((bits[i] & otherBits[i]) != 0L) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void andInPlace(clover.antlr.collections.impl.BitSet bitSet) {
        modified = true;
        super.andInPlace(bitSet);
    }

    @Override
    public void add(int i) {
        modified = true;
        super.add(i);
    }

    @Override
    public void notInPlace() {
        modified = true;
        super.notInPlace();
    }

    @Override
    public void notInPlace(int i) {
        modified = true;
        super.notInPlace(i);
    }

    @Override
    public void notInPlace(int i, int i2) {
        modified = true;
        super.notInPlace(i, i2);
    }

    @Override
    public void orInPlace(clover.antlr.collections.impl.BitSet bitSet) {
        modified = true;
        super.orInPlace(bitSet);
    }

    @Override
    public void remove(int i) {
        modified = true;
        super.remove(i);
    }

    @Override
    public void subtractInPlace(clover.antlr.collections.impl.BitSet bitSet) {
        modified = true;
        super.subtractInPlace(bitSet);
    }

    public CloverBitSet and(CloverBitSet a) {
        CloverBitSet s = (CloverBitSet)this.clone();
        s.andInPlace(a);
        return s;
    }

    public CloverBitSet or(CloverBitSet a) {
        CloverBitSet s = (CloverBitSet)this.clone();
        s.orInPlace(a);
        return s;
    }

    public CloverBitSet flip(int startIdx, int endIdx) {
        CloverBitSet s = (CloverBitSet)this.clone();
        s.notInPlace(startIdx, endIdx - 1);
        return s;
    }

    public static BitSet fromIntArray(int[] data) {
        final BitSet bitSet = new BitSet(data.length);

        for (int i = 0; i < data.length; i++) {
            if (data[i] > 0) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    public int length() {

        for (int i = bits.length - 1; i >= 0; i--) {
            long word = bits[i];
            if (word != 0L) {
                for (int bit = BITS - 1; bit >= 0; bit--) {
                    if ((word & (1L << bit)) != 0) {
                        return (i << 6) + bit + 1;
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Returns true if any modification of the bit set was <b>possibly</b> made via any of {@link #add(int)}, {@link #remove(int)},
     * {@link #andInPlace(clover.antlr.collections.impl.BitSet)}, {@link #orInPlace(clover.antlr.collections.impl.BitSet)},
     * {@link #notInPlace()}, {@link #notInPlace(int)}, {@link #notInPlace(int, int)} method calls. Note it does not check
     * if content was really changed, but whether the method was called.
     *
     * @return boolean true if bitset was possibly modified, false otherwise
     */
    public boolean isModified() {
        return modified;
    }
}
