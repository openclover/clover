package org.openclover.core.io.tags;

import org.openclover.runtime.Logger;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads tagged data from an input stream. Supports raw types, strings and
 * complex objects implementing TaggedPersistent.
 * Supports null strings and complex objects.
 */
public class TaggedInputReader implements TaggedDataInput {
    private final DataInput in;
    private final Tags tags;

    public TaggedInputReader(DataInput in, Tags tags) {
        this.in = in;
        this.tags = tags;
    }

    private boolean readObjectTagAndCheckNullity(int expectedTag) throws IOException {
        readTagOrFail(expectedTag);
        return in.readBoolean();
    }

    private void readTagOrFail(int expectedTag) throws IOException {
        final int actualTag = in.readByte();

        ///CLOVER:OFF
        if (TaggedIO.isDebug()) { Logger.getInstance().debug("Tag: " + actualTag); }
        ///CLOVER:ON

        if (expectedTag != actualTag) {
            throw new WrongTagException(expectedTag, actualTag);
        }
    }

    @Override
    public boolean readBoolean() throws IOException { readTagOrFail(Tags.BOOL_TAG); return in.readBoolean(); }
    @Override
    public byte readByte() throws IOException { readTagOrFail(Tags.BYTE_TAG); return in.readByte(); }
    @Override
    public short readShort() throws IOException { readTagOrFail(Tags.SHORT_TAG); return in.readShort(); }
    @Override
    public char readChar() throws IOException { readTagOrFail(Tags.CHAR_TAG); return in.readChar(); }
    @Override
    public int readInt() throws IOException { readTagOrFail(Tags.INT_TAG); return in.readInt(); }
    @Override
    public long readLong() throws IOException { readTagOrFail(Tags.LONG_TAG); return in.readLong(); }
    @Override
    public float readFloat() throws IOException { readTagOrFail(Tags.FLOAT_TAG); return in.readFloat(); }
    @Override
    public double readDouble() throws IOException { readTagOrFail(Tags.DOUBLE_TAG); return in.readDouble(); }
    @Override
    public String readUTF() throws IOException {
        return readObjectTagAndCheckNullity(Tags.STRING_TAG) ? null : in.readUTF();
    }

    /**
     * Reads a possibly null {@link TaggedPersistent} from the stream.
     * @param superType the type or supertype of the object to be read
     * @throws UnknownTagException if a tag is encountered that this reader doesn't know about
     * @throws TagTypeMismatchException if the read object is not the same or subtype of the supplied class 
     **/
    @Override
    public <T extends TaggedPersistent> T read(Class<T> superType) throws IOException {
        ///CLOVER:OFF
        if (TaggedIO.isDebug()) { Logger.getInstance().debug("Reading " + superType.getName()); }
        ///CLOVER:ON

        final int tag = in.readByte();

        ///CLOVER:OFF
        if (TaggedIO.isDebug()) { Logger.getInstance().debug("Tag: " + tag); }
        ///CLOVER:ON

        if (!tags.isDefined(tag)) {
            throw new UnknownTagException(tag);
        }
        final boolean isNull = in.readBoolean();
        final T result;
        if (!isNull) {
            result = tags.<T>invokeObjectReaderFor(tag, this);
            if (!superType.isAssignableFrom(result.getClass())) {
                throw new TagTypeMismatchException(tag, superType.getClass(), result.getClass());
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Read a list of objects, proceeded by their count and return them in a list.
     */
    @Override
    public <T extends TaggedPersistent, I> List<I> readList(Class<T> type) throws IOException {
        final int count = readInt();
        final List<I> entities = new ArrayList<>();
        for(int i = 0; i < count; i++) {
            final T entity = read(type);
            entities.add((I) entity);
        }
        return entities;
    }
}
