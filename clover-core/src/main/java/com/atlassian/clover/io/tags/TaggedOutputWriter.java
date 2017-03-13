package com.atlassian.clover.io.tags;

import com.atlassian.clover.Logger;

import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

/**
 * Writes tagged data from an input stream. Supports raw types, strings and complex objects implementing
 * TaggedPersistent. Supports null strings and complex objects.
 */
public class TaggedOutputWriter implements TaggedDataOutput {
    private final DataOutput out;
    private final Tags tags;

    public TaggedOutputWriter(DataOutput out, Tags tags) {
        this.out = out;
        this.tags = tags;
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        writeTag(Tags.BOOL_TAG);
        out.writeBoolean(v);
    }

    private void writeTagAndNullity(int tag, boolean isNull) throws IOException {
        writeTag(tag);
        out.writeBoolean(isNull);
    }

    private void writeTag(int tag) throws IOException {
        ///CLOVER:OFF
        if (TaggedIO.isDebug()) {
            Logger.getInstance().debug("Tag: " + tag);
        }
        ///CLOVER:ON
        out.writeByte(tag);
    }

    @Override
    public void writeByte(int v) throws IOException {
        writeTag(Tags.BYTE_TAG);
        out.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        writeTag(Tags.SHORT_TAG);
        out.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        writeTag(Tags.CHAR_TAG);
        out.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        writeTag(Tags.INT_TAG);
        out.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        writeTag(Tags.LONG_TAG);
        out.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeTag(Tags.FLOAT_TAG);
        out.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeTag(Tags.DOUBLE_TAG);
        out.writeDouble(v);
    }

    /**
     * Writes a string as UTF - accepts null strings
     */
    @Override
    public void writeUTF(String s) throws IOException {
        writeTagAndNullity(Tags.STRING_TAG, s == null);
        if (s != null) {
            out.writeUTF(s);
        }
    }

    /**
     * Writes a possibly null object by invoking its {@link TaggedPersistent#write(TaggedDataOutput)} method if
     * non-null.
     */
    @Override
    public <T extends TaggedPersistent> void write(Class<? extends T> t, T p) throws IOException {
        ///CLOVER:OFF
        if (TaggedIO.isDebug()) {
            Logger.getInstance().debug("Writing " + t.getName());
        }
        ///CLOVER:ON

        writeTagAndNullity(tags.getTagFor(t), p == null);
        if (p != null) {
            p.write(this);
        }
    }

    /**
     * Writes a list of objects, proceeded by their count.
     *
     * @param type     use T.class
     * @param elements list of objects
     * @throws IOException
     */
    @Override
    public <T extends TaggedPersistent> void writeList(Class<T> type, List<T> elements) throws IOException {
        writeInt(elements.size());
        for (T element : elements) {
            write(type, element);
        }
    }
}
