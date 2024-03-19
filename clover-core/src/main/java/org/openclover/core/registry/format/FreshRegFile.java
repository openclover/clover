package org.openclover.core.registry.format;

import org.openclover.core.CoverageData;
import org.openclover.core.registry.RegistryUpdate;
import org.openclover.runtime.api.registry.CloverRegistryException;
import org.openclover.runtime.registry.format.RegAccessMode;
import org.openclover.runtime.registry.format.RegHeader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.List;

import static org.openclover.core.util.Lists.newLinkedList;

/**
 * Representation of a registry file that is new (without any instrumentation contents).
 * The file is uncommitted to disk until {@link #save(java.util.List)} is called.
 */
public class FreshRegFile extends RegFile<UpdatableRegFile> {
    private final RegAccessMode accessMode;
    private final String name;
    private final CoverageSegment coverageSegment;

    public FreshRegFile(File file, RegAccessMode accessMode, String name, CoverageData coverageData) {
        super(file);
        this.accessMode = accessMode;
        this.name = name;
        this.coverageSegment = coverageData == null ? null : new CoverageSegment(coverageData);
    }

    public FreshRegFile(File file, RegAccessMode accessMode, String name) {
        this(file, accessMode, name, null);
    }

    public FreshRegFile(RegFile regFile, CoverageData coverageData) {
        this(regFile.getFile(), regFile.getAccessMode(), regFile.getName(), coverageData);
    }

    private static File ensureFileAccessible(File file) throws InaccessibleRegFileException {
        final File absFile = file.getAbsoluteFile();
        if (absFile.exists() && (!absFile.canWrite() || absFile.isDirectory())) {
            throw new InaccessibleRegFileException("OpenClover database " + absFile .getAbsolutePath() + " already exists but cannot be overwritten or is a directory.");
        } else if (!absFile.exists() && absFile.getParentFile() != null && absFile.getParentFile().exists() && !absFile.getParentFile().canWrite()) {
            throw new InaccessibleRegFileException("OpenClover database " + absFile.getAbsolutePath() + " cannot be written to (parent directory doesn't exist or can't be written to).");
        }
        return file;
    }

    @Override
    protected UpdatableRegFile saveImpl(List<? extends RegistryUpdate> deltas) throws IOException, CloverRegistryException {
        if (deltas.size() == 0) {
            throw new IllegalArgumentException("At least one registry update is required for saving");
        }

        final File registryFile = ensureFileAccessible(getFile());
        // write to a tmp file first, then rename, to avoid someone trying to read when half written
        final File tmpfile = new File(registryFile.getParentFile(), registryFile.getName() + ".tmp");
        final File tmpParentFile = tmpfile.getParentFile();
        if (tmpParentFile != null) {
            tmpParentFile.mkdirs();
        }
        tmpfile.delete();

        final long finalVersion = deltas.get(deltas.size() - 1).getVersion();
        final List<InstrSessionSegment> sessions = newLinkedList();
        RegHeader header = new RegHeader(accessMode, finalVersion, 0, CoverageSegment.NONE_IDX, InstrSessionSegment.NONE_IDX, getName());

        //Must use RAF here because we rewind and update - otherwise channel.position(..)
        //followed by a write will blank out intermediate data
        try (RandomAccessFile file = new RandomAccessFile(tmpfile, "rw")) { //Will close the channel as well
            final FileChannel channel = file.getChannel();

            //Write a basic header that indicates writing is incomplete
            channel.position(0);
            header.write(channel);

            //Now append coverage if it exists
            final long covLoc;
            if (coverageSegment != null) {
                coverageSegment.write(channel);
                covLoc = channel.position() - 1L;
            } else {
                covLoc = CoverageSegment.NONE_IDX;
            }

            //Store each instrumentation session taking note of the maximum slotlength
            int maxSlotLength = 0;
            for (RegistryUpdate delta : deltas) {
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

            //Now rewrite the header with correct indexes into the coverage area and the file info area
            header = new RegHeader(accessMode, finalVersion, maxSlotLength, covLoc, channel.position() - 1L, getName());
            channel.position(0);
            header.write(channel);
        }

        if (registryFile.exists()) {
            if (!registryFile.delete()) {
                throw new IOException("Can't delete existing registry file " + registryFile);
            }
        }

        if (!tmpfile.renameTo(registryFile)) {
            throw new IOException("Failed to move tmp registry file " + tmpfile + " to final registry file");
        }
        registryFile.setLastModified(finalVersion);

        //Retrun a new representation of the registry file state that reflects that it's been committed to disk
        return new UpdatableRegFile(getFile(), header);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RegAccessMode getAccessMode() {
        return accessMode;
    }

    @Override
    public boolean isAppendable() {
        return false;
    }
}