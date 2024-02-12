package org.openclover.core.registry.format;

import org.openclover.core.registry.RegistryUpdate;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.runtime.api.registry.CloverRegistryException;
import org.openclover.runtime.registry.format.RegAccessMode;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openclover.core.util.Lists.newLinkedList;

/**
 * The in-memory representation of a registry file's state.
 * A registry file can only be updated/saved once after which the returned
 * new instance of RegFile must be used.
 *
 * @param <CDR> the next registry file state after {@link #save(java.util.List)} } is called
 */
public abstract class RegFile<CDR extends RegFile> {
    /** The underlying registry file */
    private final File file;
    /** Whether {@link #save(java.util.List)} has been called */
    private final AtomicBoolean saved = new AtomicBoolean(false);

    public RegFile(File file) {
        this.file = file;
    }

    /**
     * Saves a number of updates to the registry file on disk.
     * @return CDR a RegFile instance that represents the updated registrty file's state
     * @throws IllegalStateException if save has already been called
     **/
    public final CDR save(List<? extends RegistryUpdate> deltas) throws IOException, CloverRegistryException {
        if (!saved.getAndSet(true)) {
            return saveImpl(deltas);
        } else {
            throw new IllegalStateException("Registry file already committed to disk");
        }
    }

    /**
     * Saves a single update to the registry file on disk.
     * @return CDR a RegFile instance that represents the updated registrty file's state
     * @throws IllegalStateException if save has already been called
     **/
    public final CDR save(RegistryUpdate delta) throws IOException, CloverRegistryException {
        return save(Collections.singletonList(delta));
    }

    /** The meat of the save work - subclass specific */
    protected abstract CDR saveImpl(List<? extends RegistryUpdate> deltas) throws IOException, CloverRegistryException;

    /** @return the name of the registry (note: not the file name) */
    public abstract String getName();

    public File getFile() {
        return file;
    }

    protected List<FileInfoRecord> toRecords(Iterable<FullFileInfo> fileInfos) {
        List<FileInfoRecord> recs = newLinkedList();
        for (FullFileInfo fileInfo : fileInfos) {
            recs.add(new FileInfoRecord(fileInfo));
        }
        return recs;
    }

    public abstract RegAccessMode getAccessMode();

    public abstract boolean isAppendable();
}
