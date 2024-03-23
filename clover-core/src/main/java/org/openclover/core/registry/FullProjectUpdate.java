package org.openclover.core.registry;

import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.context.ContextStore;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.api.registry.HasMetricsFilter;

import java.util.List;

public class FullProjectUpdate implements RegistryUpdate {
    private final ProjectInfo proj;
    private final ContextStore ctxStore;
    private final long startTs;
    private final long endTs;

    public FullProjectUpdate(ProjectInfo proj, ContextStore ctxStore, long startTs, long endTs) {
        this.proj = proj;
        this.ctxStore = ctxStore;
        this.startTs = startTs;
        this.endTs = endTs;
    }

    @Override
    public long getVersion() {
        return proj.getVersion();
    }

    @Override
    public long getStartTs() {
        return startTs;
    }

    @Override
    public long getEndTs() {
        return endTs;
    }

    @Override
    public int getSlotCount() {
        return proj.getDataLength();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FileInfo> getFileInfos() {
        return proj.getFiles(HasMetricsFilter.ACCEPT_ALL);
    }

    @Override
    public ContextStore getContextStore() {
        return ctxStore;
    }
}
