package com.atlassian.clover.reporters.filters;

import com.atlassian.clover.registry.BaseInvertableFilter;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.api.registry.HasMetrics;

public class SourceFileFilter extends BaseInvertableFilter {
    public SourceFileFilter() {
        this(false);
    }

    SourceFileFilter(boolean inverted) {
        super(inverted);
    }

    @Override
    public SourceFileFilter invert() {
        return new SourceFileFilter(!isInverted());
    }

    @Override
    public boolean accept(HasMetrics hm) {
        if (hm instanceof FullFileInfo) {
            FullFileInfo fileInfo = (FullFileInfo) hm;
            return isInverted() ^ (!fileInfo.isTestFile()); //if not inverted returns true if source file
        }

        return true;
    }

}
