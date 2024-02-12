package org.openclover.core.reporters.filters;

import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.BaseInvertableFilter;
import org.openclover.core.registry.entities.FullFileInfo;

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
