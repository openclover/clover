package com.atlassian.clover.io.tags;

import java.io.IOException;
import java.util.Map;

import static clover.com.google.common.collect.Maps.newHashMap;

/** A register of raw tags and object tags */
public class Tags {
    public static final int BOOL_TAG = 0;
    public static final int BYTE_TAG = 1;
    public static final int SHORT_TAG = 2;
    public static final int CHAR_TAG = 3;
    public static final int INT_TAG = 4;
    public static final int LONG_TAG = 5;
    public static final int FLOAT_TAG = 6;
    public static final int DOUBLE_TAG = 7;
    public static final int STRING_TAG = 8;
    public static final int NEXT_TAG = 9;

    private Map<String, ObjectReader<? extends TaggedPersistent>> classNameToReader = newHashMap();
    private Map<String, Integer> classNameToTag = newHashMap();
    private Map<Integer, ObjectReader<? extends TaggedPersistent>> tagToReader = newHashMap();

    public Tags() {
    }

    /**
     * Registers a new tag, the class it corresponds to and the reader that knows how to read the object.
     * @return this for chaining
     */
    public Tags registerTag(String className, int tag, ObjectReader<? extends TaggedPersistent> reader) {
        if (tag < NEXT_TAG || tag > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Tag values must be between " + NEXT_TAG + " and " + Byte.MAX_VALUE);
        }
        classNameToReader.put(className, reader);
        classNameToTag.put(className, tag);
        tagToReader.put(tag, reader);
        return this;
    }

    int getTagFor(Class<? extends TaggedPersistent> clazz) throws UnknownTagException {
        Integer tag = classNameToTag.get(clazz.getName());
        if (tag == null) {
            throw new UnknownTagException(clazz.getName());
        }
        return tag;
    }

    @SuppressWarnings("unchecked")
    public <T extends TaggedPersistent> T invokeObjectReaderFor(int tag, TaggedDataInput in) throws IOException {
        final ObjectReader<? extends TaggedPersistent> builder = tagToReader.get(tag);
        if (builder == null) {
            throw new UnknownTagException(tag);
        }
        return (T)builder.read(in);
    }

    public boolean isDefined(int tag) {
        return tagToReader.containsKey(tag);
    }

    public interface ObjectReader<T extends TaggedPersistent> {
        T read(TaggedDataInput in) throws IOException;
    }
}
