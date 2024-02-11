package org.openclover.core.reporters.filters;

import org.openclover.core.registry.BaseInvertableFilter;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.api.registry.HasMetrics;

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
