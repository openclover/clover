package org.openclover.core.registry;

import org.openclover.core.context.ContextStore;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.metrics.HasMetricsFilter;

import java.util.List;

public class FullProjectUpdate implements RegistryUpdate {
    private final FullProjectInfo proj;
    private final ContextStore ctxStore;
    private final long startTs;
    private final long endTs;

    public FullProjectUpdate(FullProjectInfo proj, ContextStore ctxStore, long startTs, long endTs) {
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
    public List<FullFileInfo> getFileInfos() {
        return proj.getFiles(HasMetricsFilter.ACCEPT_ALL);
    }

    @Override
    public ContextStore getContextStore() {
        return ctxStore;
    }
}
