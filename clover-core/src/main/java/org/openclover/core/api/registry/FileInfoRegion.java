package org.openclover.core.api.registry;

import org.jetbrains.annotations.Nullable;

/**
 * A source region associated with a file info
 */
public interface FileInfoRegion extends SourceInfo {
    @Nullable
    FileInfo getContainingFile();
}
