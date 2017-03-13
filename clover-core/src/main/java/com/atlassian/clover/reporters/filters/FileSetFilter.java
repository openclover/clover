package com.atlassian.clover.reporters.filters;

import com.atlassian.clover.registry.BaseInvertableFilter;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.api.registry.HasMetrics;

import java.util.List;

public class FileSetFilter extends BaseInvertableFilter {
    private List sourceFiles;

    public FileSetFilter(List sourceFiles) {
        this(sourceFiles, false);
    }

    FileSetFilter(List sourceFiles, boolean inverted) {
        super(inverted);
        this.sourceFiles = sourceFiles;
    }

    @Override
    public FileSetFilter invert() {
        return new FileSetFilter(sourceFiles, !isInverted());
    }

    @Override
    public boolean accept(HasMetrics hm) {
        if (hm instanceof FullFileInfo) {
            FullFileInfo fileInfo = (FullFileInfo) hm;
            return isInverted() ^ sourceFiles.contains(fileInfo.getPhysicalFile());
        }

        return true;
    }
}
