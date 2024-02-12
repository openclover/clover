package org.openclover.core.registry;

import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.SourceInfo;

/**
 * A source region associated with a file info
 */
public interface FileInfoRegion extends SourceInfo {
    @Nullable
    FileInfo getContainingFile();
}
