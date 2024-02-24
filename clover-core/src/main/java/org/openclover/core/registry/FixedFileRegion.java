package org.openclover.core.registry;

import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.SourceInfo;

public class FixedFileRegion implements FileInfoRegion {
    protected FileInfo containingFile;
    protected FixedSourceRegion region;

    public FixedFileRegion(int startLine, int startColumn) {
        this(null, startLine, startColumn, startLine, startColumn);
    }

    public FixedFileRegion(int startLine, int startColumn, int endLine, int endColumn) {
        this(null, startLine, startColumn, endLine, endColumn);
    }

    public FixedFileRegion(FileInfo containingFile, int startLine, int startColumn, int endLine, int endColumn) {
        this(containingFile, new FixedSourceRegion(startLine, startColumn, endLine, endColumn));
    }

    public FixedFileRegion(FileInfo containingFile, SourceInfo region) {
        this.containingFile = containingFile;
        this.region = FixedSourceRegion.of(region);
    }

    @Override
    @Nullable
    public FileInfo getContainingFile() {
        return containingFile;
    }

    @Override
    public int getStartLine() {
        return region.getStartLine();
    }

    @Override
    public int getStartColumn() {
        return region.getStartColumn();
    }

    @Override
    public int getEndLine() {
        return region.getEndLine();
    }

    @Override
    public int getEndColumn() {
        return region.getEndColumn();
    }
}
