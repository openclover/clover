package com.atlassian.clover.registry;

import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.SourceInfo;
import org.jetbrains.annotations.Nullable;

/**
 * A source region associated with a file info
 */
public interface FileInfoRegion extends SourceInfo {
    @Nullable
    FileInfo getContainingFile();
}
