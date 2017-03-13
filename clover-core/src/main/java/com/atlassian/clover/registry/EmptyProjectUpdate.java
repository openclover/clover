package com.atlassian.clover.registry;

import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.registry.entities.FullFileInfo;

import java.util.List;
import java.util.Collections;

public class EmptyProjectUpdate implements RegistryUpdate {
    private final long version;
    private final long startTs;
    private final long endTs;
    private final int slotCount;

    public EmptyProjectUpdate(long version, long startTs, long endTs, int slotCount) {
        this.version = version;
        this.startTs = startTs;
        this.endTs = endTs;
        this.slotCount = slotCount;
    }

    @Override
    public long getVersion() {
        return version;
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
        return slotCount;
    }

    @Override
    public List<FullFileInfo> getFileInfos() {
        return Collections.emptyList();
    }

    @Override
    public ContextStore getContextStore() {
        return null;
    }
}
