package com.atlassian.clover.registry.format;

import com.atlassian.clover.api.instrumentation.ConcurrentInstrumentationException;
import com.atlassian.clover.api.registry.CloverRegistryException;
import com.atlassian.clover.registry.NoSuchRegistryException;
import com.atlassian.clover.registry.RegistryUpdate;
import com.atlassian.clover.registry.ReadOnlyRegistryException;
import com.atlassian.clover.registry.RegistryFormatException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.List;

import static clover.com.google.common.collect.Lists.newLinkedList;

public class UpdatableRegFile extends RegFile<UpdatableRegFile> {
    private final RegHeader header;

    public UpdatableRegFile(File file) throws IOException, NoSuchRegistryException, InaccessibleRegFileException, RegistryFormatException {
        this(ensureFileAccessible(file), RegHeader.readFrom(file));
    }

    UpdatableRegFile(File file, RegHeader header) throws NoSuchRegistryException, InaccessibleRegFileException {
        super(ensureFileAccessible(file));
        this.header = header;
    }

    private long calcMinSessionPosition() {
        return header.getCoverageLocation() == -1L ? RegHeader.SIZE : header.getCoverageLocation();
    }

    public RegHeader getHeader() {
        return header;
    }

    private RegContents startReading() throws IOException {
        return new RegContents(
            new RandomAccessFile(getFile(), "r"),
            header.getCoverageLocation(),
            header.getLastSessionLocation(),
            calcMinSessionPosition());
    }

    public void readContents(RegContentsConsumer consumer) throws IOException, CloverRegistryException {
        final RegContents contents = startReading();
        try {
            consumer.consume(contents);
        } finally {
            contents.close();
        }
    }

    @Override
    public UpdatableRegFile saveImpl(List<? extends RegistryUpdate> deltas) throws IOException, CloverRegistryException {
        if (deltas.size() == 0) {
            throw new IllegalArgumentException("At least one registry update is required");
        }

        if (header.getAccessMode() == RegAccessMode.READONLY) {
            throw new ReadOnlyRegistryException();
        }

        //Must use RAF here because we rewind and update - otherwise channel.position(..)
        //followed by a write will blank out intermediate data
        final File registryFile = getFile();
        final RandomAccessFile randomAccessFile = new RandomAccessFile(registryFile, "rw");
        try {
            final FileChannel channel = randomAccessFile.getChannel();
            final Collection<InstrSessionSegment> sessions = newLinkedList();

            final RegHeader currentHeader = RegHeader.readFrom(getFile().getAbsolutePath(), channel);
            if (currentHeader.getVersion() != header.getVersion()) {
                throw new ConcurrentInstrumentationException(
                    "The on-disk registry version (" + currentHeader.getVersion() + ") differs from the in-memory version (" + header.getVersion() + ")");
            }

            int maxSlotLength = header.getSlotCount();
            long latestVersion = System.currentTimeMillis();

            //Append
            channel.position(header.getLastSessionLocation() + 1);
            for (RegistryUpdate delta : deltas) {
                latestVersion = delta.getVersion();
                maxSlotLength = Math.max(maxSlotLength, delta.getSlotCount());

                final InstrSessionSegment session =
                    new InstrSessionSegment(
                        delta.getVersion(),
                        delta.getStartTs(),
                        delta.getEndTs(),
                        toRecords(delta.getFileInfos()),
                        delta.getContextStore());

                session.write(channel);
                sessions.add(session);
            }

            //Update the header with new values
            final RegHeader header =
                new RegHeader(
                    this.header.getAccessMode(),
                    latestVersion,
                    maxSlotLength,
                    this.header.getCoverageLocation(),
                    channel.position() - 1L,
                    getName());

            channel.position(0);
            header.write(channel);
            
            registryFile.setLastModified(latestVersion);

            return new UpdatableRegFile(getFile(), header);
        } finally {
            randomAccessFile.close();
        }
    }

    @Override
    public String getName() {
        return header.getName();
    }

    public long getVersion() {
        return header.getVersion();
    }

    @Override
    public RegAccessMode getAccessMode() {
        return header.getAccessMode();
    }

    public int getSlotCount() {
        return header.getSlotCount();
    }

    private static File ensureFileAccessible(File file) throws NoSuchRegistryException, InaccessibleRegFileException {
        final File absFile = file.getAbsoluteFile();
        if (!absFile.exists()) {
            throw new NoSuchRegistryException("Clover registry file: ${file} does not exist. Please ensure Clover has "
                    + "instrumented your source files. You may need to remove existing .class files for this to occur.",
                    absFile);
        } else if (!absFile.isFile()) {
            throw new InaccessibleRegFileException("Clover registry file: " + absFile.getAbsolutePath() + " is not a file.");
        } else if (!absFile.canRead()) {
            throw new InaccessibleRegFileException("Clover registry file: " + absFile.getAbsolutePath() + " can not be read by Clover. Please ensure read/write access is granted to this file.");
        }
        return absFile;
    }

    @Override
    public boolean isAppendable() {
        try {
            return RegHeader.readFrom(getFile()).getVersion() == header.getVersion();
        } catch (Exception e) {
            return false;
        }
    }
}
