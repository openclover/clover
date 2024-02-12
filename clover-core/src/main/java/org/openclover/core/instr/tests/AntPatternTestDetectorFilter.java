package org.openclover.core.instr.tests;

import org.jetbrains.annotations.Nullable;
import org.openclover.core.util.FilterUtils;
import org.openclover.core.util.Objects;

import java.io.File;

public class AntPatternTestDetectorFilter implements TestDetector {
    @Nullable final String[] includeFilter;
    @Nullable final String[] excludeFilter;
    private final File projectRoot;

    public AntPatternTestDetectorFilter(File projectRoot,
                                        @Nullable String[] includeFilter,
                                        @Nullable String[] excludeFilter) {
        this.includeFilter = includeFilter;
        this.excludeFilter = excludeFilter;
        this.projectRoot = projectRoot;
    }

    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        return matchesPattern(sourceContext.getSourceFile());
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        return matchesPattern(sourceContext.getSourceFile());
    }

    private boolean matchesPattern(File file) {
        final String path = file.getAbsolutePath();
        final String root = rootWithEndingSeparator();
        if (path.startsWith(root)) {
            // inside a folder, crop the root and check the rest against filters
            final String relative = path.substring(root.length());
            return FilterUtils.isIncluded(relative, excludeFilter, includeFilter, true);
        } else {
            return false;
        }
    }

    private String rootWithEndingSeparator() {
        final String root = projectRoot.getAbsolutePath();
        return root.endsWith(File.separator) ? root : root + File.separator;
    }

    @Override
    public String toString() {
        return Objects.toStringBuilder(this)
                .add("includeFilter", includeFilter)
                .add("excludeFilter", excludeFilter)
                .add("projectRoot", projectRoot)
                .toString();
    }
}
