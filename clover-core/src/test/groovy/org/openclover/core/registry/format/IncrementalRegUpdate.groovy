package org.openclover.core.registry.format

import com.atlassian.clover.context.ContextStore
import com.atlassian.clover.registry.RegistryUpdate
import com.atlassian.clover.registry.entities.FullFileInfo

class IncrementalRegUpdate implements RegistryUpdate {
    final long version
    final long startTs
    final long endTs
    final int slotCount
    final List<FullFileInfo> fileInfos
    final ContextStore ctxStore

    IncrementalRegUpdate(long version, long startTs, long endTs, int slotCount, List<FullFileInfo> fileInfos, ContextStore ctxStore) {
        this.version = version
        this.startTs = startTs
        this.endTs = endTs
        this.slotCount = slotCount
        this.fileInfos = Collections.unmodifiableList(fileInfos)
        this.ctxStore = ctxStore
    }

    long getVersion() {
        return version
    }

    long getStartTs() {
        return startTs
    }

    long getEndTs() {
        return endTs
    }

    int getSlotCount() {
        return slotCount
    }

    List<FullFileInfo> getFileInfos() {
        return fileInfos
    }

    ContextStore getContextStore() {
        return ctxStore
    }
}
