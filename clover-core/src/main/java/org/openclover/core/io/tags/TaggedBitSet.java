package org.openclover.core.io.tags;

import java.io.IOException;
import java.util.BitSet;

/**
 * A {@link TaggedPersistent} wrapper around the JDK {@link BitSet} so that bit
 * sets can be written and read as first-class tagged values. {@code BitSet} is a
 * universal JDK type, so this wrapper lives in the {@code io.tags} package and is
 * meant to be registered once in any {@link Tags} table that needs it.
 */
public final class TaggedBitSet implements TaggedPersistent {

    private final BitSet bitSet;

    public TaggedBitSet(BitSet bitSet) {
        this.bitSet = bitSet;
    }

    public BitSet getBitSet() {
        return bitSet;
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        final long[] words = bitSet.toLongArray();
        out.writeInt(words.length);
        for (final long word : words) {
            out.writeLong(word);
        }
    }

    public static TaggedBitSet read(TaggedDataInput in) throws IOException {
        final int length = in.readInt();
        final long[] words = new long[length];
        for (int i = 0; i < length; i++) {
            words[i] = in.readLong();
        }
        return new TaggedBitSet(BitSet.valueOf(words));
    }
}
