package com.atlassian.clover.registry;

import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.registry.entities.FullFileInfo;

import java.util.List;

public interface RegistryUpdate {
    public long getVersion();
    public long getStartTs();
    public long getEndTs();
    public int getSlotCount();
    public List<FullFileInfo> getFileInfos();
    public ContextStore getContextStore();
}
