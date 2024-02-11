package org.openclover.runtime.registry.format;

import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.io.IOException;
import java.util.zip.Adler32;

/** General ByteBuffer utility methods */
class BufferUtils {
    static void writeFully(FileChannel channel, ByteBuffer fileInfosBuffer) throws IOException {
        while (fileInfosBuffer.hasRemaining()) {
            channel.write(fileInfosBuffer);
        }
    }

    static ByteBuffer readFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        int read = 0;
        while (buffer.hasRemaining() && read != -1) {
            read = channel.read(buffer);
        }
        if (buffer.hasRemaining()) {
            throw new BufferUnderflowException();
        }
        buffer.flip();
        return buffer;
    }

    static void putWithChecksum(ByteBuffer buffer, int value, Adler32 checksum) {
        buffer.putInt(value);
        checksum.update(value);
    }

    static void putWithChecksum(ByteBuffer buffer, long value, Adler32 checksum) {
        buffer.putLong(value);
        checksum.update((int)(value & 0xFFFFL));
        checksum.update((int)(value >> 32));
    }

    static int getIntWithChecksum(ByteBuffer buffer, Adler32 checksum) {
        final int value = buffer.getInt();
        checksum.update(value);
        return value;
    }

    static long getLongWithChecksum(ByteBuffer buffer, Adler32 checksum) {
        final long value = buffer.getLong();
        checksum.update((int)(value & 0xFFFFL));
        checksum.update((int)(value >> 32));
        return value;
    }
}
