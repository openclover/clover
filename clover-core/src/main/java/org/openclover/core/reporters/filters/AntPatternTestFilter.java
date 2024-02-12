package org.openclover.core.reporters.filters;

import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.BaseInvertableFilter;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.util.FilterUtils;

import java.io.File;
import java.util.Arrays;

public class AntPatternTestFilter extends BaseInvertableFilter {
    private final String[] includeFilter;
    private final String[] excludeFilter;
    private final String root;

    AntPatternTestFilter(boolean inverted, String root, String[] includePattern, String[] excludePattern) {
        super(inverted);
        this.root = root == null || root.endsWith(File.separator) ? root : root + File.separator;
        this.includeFilter = includePattern;
        this.excludeFilter = excludePattern;
    }

    public AntPatternTestFilter(String root, String[] includePattern, String[] excludePattern) {
        this(false, root, includePattern, excludePattern);
    }

    public AntPatternTestFilter(String root) {
        this(root, new String[]{}, new String[]{});
    }

    @Override
    public AntPatternTestFilter invert() {
        return new AntPatternTestFilter(!isInverted(), root, includeFilter, excludeFilter);
    }

    @Override
    public boolean accept(HasMetrics hm) {
        if (hm instanceof FullClassInfo) {
            final FullClassInfo ci = (FullClassInfo) hm;
            final File file = ((FullFileInfo)ci.getContainingFile()).getPhysicalFile();
            final String path = file.getPath();
            final String relative = root != null && path.startsWith(root) ? path.substring(root.length()) : path;
            return isInverted() ^ FilterUtils.isIncluded(relative, excludeFilter, includeFilter, false);
        }

        return true;
    }

    @Override
    public String toString() {
        return "AntPatternTestFilter:[" + root + "]" + Arrays.toString(includeFilter) + Arrays.toString(excludeFilter);
    }
}
