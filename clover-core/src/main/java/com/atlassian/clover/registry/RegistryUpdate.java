package com.atlassian.clover.registry;

import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.registry.entities.FullFileInfo;

import java.util.List;

public interface RegistryUpdate {
    long getVersion();
    long getStartTs();
    long getEndTs();
    int getSlotCount();
    List<FullFileInfo> getFileInfos();
    ContextStore getContextStore();
}
