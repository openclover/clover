package com.atlassian.clover.context;

import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedDataOutput;
import com.atlassian.clover.io.tags.TaggedPersistent;
import com.atlassian.clover.util.CloverBitSet;

import java.io.IOException;
import java.util.Map;

/** Set of context filters - immutable */
public class ContextSet implements com.atlassian.clover.api.registry.ContextSet, TaggedPersistent {
    public static ContextSet remap(ContextSet orig, Map<Integer, Integer> mapping) {
        CloverBitSet res = new CloverBitSet();

        final CloverBitSet bs = orig.bitSet;
        for (int i = 0; i < bs.length(); i++) {
            if (bs.member(i)) {
                Integer mapValue = mapping.get(i);
                if (mapValue != null) {
                    res.add(mapValue);
                }
            }
        }
        return new ContextSet(res);
    }

    private final CloverBitSet bitSet;

    public ContextSet() {
        bitSet = new CloverBitSet(ContextStore.NEXT_INDEX);
    }

    public ContextSet(int size) {
        bitSet = new CloverBitSet(size);
    }

    public ContextSet(ContextSet copy) {
        bitSet = (CloverBitSet)copy.bitSet.clone();
    }

    private ContextSet(CloverBitSet bitSet) {
        this.bitSet = bitSet;
    }

    @Override
    public boolean get(int bitIndex) {
        return bitSet.member(bitIndex);
    }

    @Override
    public boolean intersects(com.atlassian.clover.api.registry.ContextSet other) {
        return bitSet.intersects(((ContextSet)other).bitSet);
    }

    @Override
    public int nextSetBit(int fromIndex) {
        return bitSet.nextSetBit(fromIndex);
    }

    public int size() {
        return bitSet.size();
    }

    @Override
    public ContextSet and(com.atlassian.clover.api.registry.ContextSet other) {
        return new ContextSet(bitSet.and(((ContextSet)other).bitSet));
    }

    public ContextSet clear(int bitIndex) {
        final CloverBitSet clone = (CloverBitSet)bitSet.clone();
        clone.clear(bitIndex);
        return new ContextSet(clone);
    }

    public ContextSet or(ContextSet set) {
        return new ContextSet(bitSet.or(set.bitSet));
    }

    @Override
    public ContextSet set(int bitIndex) {
        final CloverBitSet clone = (CloverBitSet)bitSet.clone();
        clone.add(bitIndex);
        return new ContextSet(clone);
    }
                              
    public ContextSet set(int bitIndex, boolean value) {
        final CloverBitSet clone = (CloverBitSet)bitSet.clone();
        if (value) {
            clone.add(bitIndex);
        } else {
            clone.clear(bitIndex);
        }
        return new ContextSet(clone);
    }

    public ContextSet flip(int startIdx, int endIdx) {
        return new ContextSet(bitSet.flip(startIdx, endIdx));
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "bitset{" + bitSet.toString() + "}";
    }
    ///CLOVER:ON

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return bitSet.equals(((ContextSet)o).bitSet);
    }

    @Override
    public int hashCode() {
        return bitSet.hashCode();
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        final long[] bitsAsLongs = bitSet.getBits();
        out.writeInt(bitsAsLongs.length);
        for (long bitsAsLong : bitsAsLongs) {
            out.writeLong(bitsAsLong);
        }
    }

    public static ContextSet read(TaggedDataInput in) throws IOException {
        final int numLongs = in.readInt();
        long[] bitsAsLongs = new long[numLongs];
        for(int i = 0; i < numLongs; i++) {
            bitsAsLongs[i] = in.readLong();
        }
        return new ContextSet(new CloverBitSet(bitsAsLongs));
    }
}
