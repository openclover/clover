package org.openclover.core.registry;

import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.context.ContextStore;
import org.openclover.core.registry.entities.FullFileInfo;

import java.util.List;

public interface RegistryUpdate {
    long getVersion();
    long getStartTs();
    long getEndTs();
    int getSlotCount();
    List<FileInfo> getFileInfos();
    ContextStore getContextStore();
}
