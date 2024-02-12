package org.openclover.core.context;

import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.runtime.util.CloverBitSet;

import java.io.IOException;
import java.util.Map;

/** Set of context filters - immutable */
public class ContextSetImpl implements org.openclover.core.api.registry.ContextSet, TaggedPersistent {
    public static ContextSetImpl remap(ContextSetImpl orig, Map<Integer, Integer> mapping) {
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
        return new ContextSetImpl(res);
    }

    private final CloverBitSet bitSet;

    public ContextSetImpl() {
        bitSet = new CloverBitSet(ContextStore.NEXT_INDEX);
    }

    public ContextSetImpl(int size) {
        bitSet = new CloverBitSet(size);
    }

    public ContextSetImpl(ContextSetImpl copy) {
        bitSet = (CloverBitSet)copy.bitSet.clone();
    }

    private ContextSetImpl(CloverBitSet bitSet) {
        this.bitSet = bitSet;
    }

    @Override
    public boolean get(int bitIndex) {
        return bitSet.member(bitIndex);
    }

    @Override
    public boolean intersects(org.openclover.core.api.registry.ContextSet other) {
        return bitSet.intersects(((ContextSetImpl)other).bitSet);
    }

    @Override
    public int nextSetBit(int fromIndex) {
        return bitSet.nextSetBit(fromIndex);
    }

    @Override
    public int size() {
        return bitSet.size();
    }

    @Override
    public ContextSetImpl and(ContextSet other) {
        return new ContextSetImpl(bitSet.and(((ContextSetImpl)other).bitSet));
    }

    @Override
    public ContextSetImpl clear(int bitIndex) {
        final CloverBitSet clone = (CloverBitSet)bitSet.clone();
        clone.clear(bitIndex);
        return new ContextSetImpl(clone);
    }

    @Override
    public ContextSetImpl or(ContextSet other) {
        return new ContextSetImpl(bitSet.or(((ContextSetImpl)other).bitSet));
    }

    @Override
    public ContextSetImpl set(int bitIndex) {
        final CloverBitSet clone = (CloverBitSet)bitSet.clone();
        clone.add(bitIndex);
        return new ContextSetImpl(clone);
    }

    @Override
    public ContextSetImpl set(int bitIndex, boolean value) {
        final CloverBitSet clone = (CloverBitSet)bitSet.clone();
        if (value) {
            clone.add(bitIndex);
        } else {
            clone.clear(bitIndex);
        }
        return new ContextSetImpl(clone);
    }

    @Override
    public ContextSetImpl flip(int startIdx, int endIdx) {
        return new ContextSetImpl(bitSet.flip(startIdx, endIdx));
    }

    @Override
    public ContextSetImpl copyOf() {
        return new ContextSetImpl(this);
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

        return bitSet.equals(((ContextSetImpl)o).bitSet);
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

    public static ContextSetImpl read(TaggedDataInput in) throws IOException {
        final int numLongs = in.readInt();
        long[] bitsAsLongs = new long[numLongs];
        for(int i = 0; i < numLongs; i++) {
            bitsAsLongs[i] = in.readLong();
        }
        return new ContextSetImpl(new CloverBitSet(bitsAsLongs));
    }
}
