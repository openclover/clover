package com.atlassian.clover.registry.format;

import com.atlassian.clover.registry.RegistryFormatException;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Iterator;

public class RegContents {
    private final RandomAccessFile file;
    private final LazyProxy<CoverageSegment> coverageSegment;
    private final Iterator<InstrSessionSegment> sessionSegments;

    public RegContents(RandomAccessFile file, long coverageLocation, long lastSessionLocation, long minSessionPosition) {
        this.file = file;
        final FileChannel channel = file.getChannel();
        if (coverageLocation != CoverageSegment.NONE_IDX) {
            coverageSegment = new LazyLoader<CoverageSegment>(channel, coverageLocation) {
                @Override
                public CoverageSegment getImpl(FileChannel channel) throws IOException, RegistryFormatException {
                    return new CoverageSegment(channel);
                }
            };
        } else {
            coverageSegment = new LazyProxy.Preloaded<CoverageSegment>(null);
        }

        this.sessionSegments =
            new SessionIterator(channel, lastSessionLocation, minSessionPosition);
    }

    public CoverageSegment getCoverage() {
        return coverageSegment.get();
    }

    public Iterable<InstrSessionSegment> getSessions() {
        return new Iterable<InstrSessionSegment>() {
            @Override
            public Iterator<InstrSessionSegment> iterator() {
                return sessionSegments;
            }
        };
    }

    public void close() throws IOException {
        file.close();
    }

    private static class SessionIterator implements Iterator<InstrSessionSegment> {
        private final FileChannel channel;
        private final long minSessionPosition;
        private long position;

        private SessionIterator(FileChannel channel, long position, long minSessionPosition) {
            this.channel = channel;
            this.minSessionPosition = minSessionPosition;
            this.position = position;
        }

        @Override
        public boolean hasNext() {
            return position > minSessionPosition;
        }

        @Override
        public InstrSessionSegment next() {
            final InstrSessionSegment segment;
            try {
                //Position ready for InstrSessionSegment to load
                channel.position(position);
                segment = new InstrSessionSegment(channel);
                //Capture position for next InstrSessionSegment to load
                position = channel.position();
                return segment;
            } catch (IOException e) {
                throw new RegistryLoadException("IO error while loading instrumentation session", e);
            }
        }

        @Override
        public void remove() { throw new UnsupportedOperationException(); }
    }
}
