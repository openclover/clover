package com.atlassian.clover.io.tags;

import java.io.IOException;
import java.util.List;

public interface TaggedDataOutput {
    void writeUTF(String s) throws IOException;

    void writeDouble(double v) throws IOException;

    void writeFloat(float v) throws IOException;

    void writeLong(long v) throws IOException;

    void writeInt(int v) throws IOException;

    void writeChar(int v) throws IOException;

    void writeShort(int v) throws IOException;

    void writeByte(int v) throws IOException;

    void writeBoolean(boolean v) throws IOException;

    <T extends TaggedPersistent> void write(Class<? extends T> t, T p) throws IOException;

    <T extends TaggedPersistent> void writeList(Class<T> type, List<T> list) throws IOException;
}
