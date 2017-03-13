package com.atlassian.clover.registry;

import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.registry.entities.BaseFileInfo;
import org.jetbrains.annotations.Nullable;

public class FixedFileRegion implements FileInfoRegion {
    protected BaseFileInfo containingFile;
    protected FixedSourceRegion region;

    public FixedFileRegion(int startLine, int startColumn) {
        this(null, startLine, startColumn, startLine, startColumn);
    }

    public FixedFileRegion(int startLine, int startColumn, int endLine, int endColumn) {
        this(null, startLine, startColumn, endLine, endColumn);
    }

    public FixedFileRegion(BaseFileInfo containingFile, int startLine, int startColumn, int endLine, int endColumn) {
        this(containingFile, new FixedSourceRegion(startLine, startColumn, endLine, endColumn));
    }

    public FixedFileRegion(BaseFileInfo containingFile, SourceInfo region) {
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
