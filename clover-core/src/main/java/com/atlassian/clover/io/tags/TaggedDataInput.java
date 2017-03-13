package com.atlassian.clover.io.tags;

import java.io.IOException;
import java.util.List;

public interface TaggedDataInput {
    boolean readBoolean() throws IOException;

    short readShort() throws IOException;

    byte readByte() throws IOException;

    char readChar() throws IOException;

    int readInt() throws IOException;

    long readLong() throws IOException;

    String readUTF() throws IOException;

    double readDouble() throws IOException;

    float readFloat() throws IOException;

    <T extends TaggedPersistent> T read(Class<T> t) throws IOException;

    <T extends TaggedPersistent> List<T> readList(Class<T> type) throws IOException;
}
