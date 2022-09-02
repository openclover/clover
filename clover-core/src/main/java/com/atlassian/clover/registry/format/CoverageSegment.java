package com.atlassian.clover.registry.format;

import com.atlassian.clover.registry.CorruptedRegistryException;
import com.atlassian.clover.registry.RegistryFormatException;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.recorder.InMemPerTestCoverage;
import com.atlassian.clover.Logger;
import com.atlassian.clover.recorder.PerTestCoverage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

public class CoverageSegment {
    public static final long NONE_IDX = -1L;

    private final LazyProxy<int[]> hitCounts;
    private final LazyProxy<InMemPerTestCoverage> perTestCoverage;

    private static class Footer {
        public static final int SIZE = 20;
        public static final int MARKER = 0xb4b00;

        public final long covByteLen;
        public final long perTestCovByteLen;

        public Footer(long covByteLen, long perTestCovByteLen) {
            this.covByteLen = covByteLen;
            this.perTestCovByteLen = perTestCovByteLen;
        }

        public void write(FileChannel channel) throws IOException {
            final ByteBuffer headerBuffer = ByteBuffer.allocate(Footer.SIZE);
            headerBuffer.putLong(covByteLen);                               //8 +
            headerBuffer.putLong(perTestCovByteLen);                        //8 +
            headerBuffer.putInt(Footer.MARKER);                             //4 = 20
            headerBuffer.flip();
            BufferUtils.writeFully(channel, headerBuffer);
            Logger.getInstance().debug("Wrote coverage segment: " + toString());
        }

        public static Footer load(FileChannel channel, long endOfSegment) throws IOException {
            //Position channel at start of header
            channel.position(endOfSegment - Footer.SIZE + 1);

            final ByteBuffer footerBuffer = BufferUtils.readFully(channel, ByteBuffer.allocate(Footer.SIZE));

            final Footer footer = new Footer(footerBuffer.getLong(), footerBuffer.getLong());
            if (footerBuffer.getInt() != Footer.MARKER) {
                throw new IOException("CoverageSegment did not start with marker 0x" + Integer.toHexString(Footer.MARKER));
            }
            return footer;
        }
    }

    public CoverageSegment(CoverageData coverageData) {
        this.hitCounts = new LazyLoader.Preloaded<>(coverageData.getHitCounts());
        this.perTestCoverage = new LazyLoader.Preloaded<>((InMemPerTestCoverage) coverageData.getPerTestCoverage());
    }

    public CoverageSegment(FileChannel channel) throws IOException {
        final long endOfSegment = channel.position();

        final Footer footer = Footer.load(channel, endOfSegment);

        this.hitCounts = new LazyLoader<int[]>(channel, endOfSegment - Footer.SIZE - footer.perTestCovByteLen - footer.covByteLen + 1) {
            @Override
            protected int[] getImpl(FileChannel channel) throws IOException, RegistryFormatException, RegistryFormatException {
                return loadHitCounts(channel, footer.covByteLen);
            }
        };

        this.perTestCoverage = new LazyLoader<InMemPerTestCoverage>(channel, endOfSegment - Footer.SIZE - footer.perTestCovByteLen + 1) {
            @Override
            protected InMemPerTestCoverage getImpl(FileChannel channel) throws IOException, RegistryFormatException {
                return loadPerTestCoverage(channel);
            }
        };
    }

    public int[] getHitCounts() {
        return hitCounts.get();
    }

    public PerTestCoverage getPerTestCoverage() {
        return perTestCoverage.get();
    }

    private InMemPerTestCoverage loadPerTestCoverage(FileChannel channel) throws IOException {
        ObjectInputStream miis = new ObjectInputStream(new BufferedInputStream(Channels.newInputStream(channel)));
        try {
             return (InMemPerTestCoverage)miis.readObject();
        } catch (ClassNotFoundException e) {
            final IOException exception = new IOException("Failed to read PerTestCoverage from stream");
            exception.initCause(e);
            throw exception;
        }
    }

    private int[] loadHitCounts(FileChannel channel, long covByteLen) throws IOException, RegistryFormatException {
        if (covByteLen % 4 != 0) {
            throw new CorruptedRegistryException("Cannot load coverage segment: hit byte count should be a multiple of four: " + covByteLen);
        }
        if (covByteLen / 4 > (long) Integer.MAX_VALUE) {
            throw new CorruptedRegistryException("Cannot load coverage segment: the data will not fit in an int[]. Length in bytes: " + covByteLen);
        }

        int[] hitCounts = new int[(int)(covByteLen / 4)];

        int hitCountBufferSize = (int) Math.min(Integer.MAX_VALUE, covByteLen);
        ByteBuffer hitCountsBuffer = ByteBuffer.allocate(hitCountBufferSize);
        //Read each page of bytes
        for (long curByteCount = 0; curByteCount < covByteLen; curByteCount += Integer.MAX_VALUE) {
            //Handle the case where the last page is smaller than Integer.MAX_VALUE
            int bytesToRead = Math.min(hitCountBufferSize, (int) (covByteLen - curByteCount));
            hitCountsBuffer.limit(bytesToRead);
            BufferUtils.readFully(channel, hitCountsBuffer);
            hitCountsBuffer.asIntBuffer().get(hitCounts, (int)(curByteCount / 4), bytesToRead / 4);
            hitCountsBuffer.clear();
        }
        return hitCounts;
    }

    public void write(FileChannel channel) throws IOException {
        final long startPos = channel.position();

        int[] hitCountsVal = hitCounts.get();
        int hitCountByteBufferSize = (int)Math.min(Integer.MAX_VALUE, ((long)hitCountsVal.length) * 4);
        final ByteBuffer hitCountByteBuffer = ByteBuffer.allocate(hitCountByteBufferSize);
        final IntBuffer hitCountIntBuffer = hitCountByteBuffer.asIntBuffer();
        //Write each page of bytes
        for(int curHitCountIdx = 0; curHitCountIdx < hitCountsVal.length; curHitCountIdx += Integer.MAX_VALUE / 4) {
            int intsToWrite = Math.min(Integer.MAX_VALUE / 4, hitCountsVal.length - curHitCountIdx);
            hitCountIntBuffer.limit(intsToWrite);
            hitCountIntBuffer.put(hitCountsVal, curHitCountIdx, intsToWrite);
            BufferUtils.writeFully(channel, hitCountByteBuffer);
            hitCountIntBuffer.clear();
        }

        final long afterCovPos = channel.position();

        final ObjectOutputStream oos = new ObjectOutputStream(Channels.newOutputStream(channel));
        try {
            oos.writeObject(perTestCoverage.get());
        } finally {
            oos.flush();
        }
        final long afterPerTestCovPos = channel.position();

        new Footer(afterCovPos - startPos, afterPerTestCovPos - afterCovPos).write(channel);
    }
}
