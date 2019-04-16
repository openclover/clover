package com.atlassian.clover.instr.tests;

import com.atlassian.clover.util.FilterUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class AntPatternTestDetectorFilter implements TestDetector {
    @Nullable final String[] includeFilter;
    @Nullable final String[] excludeFilter;
    private final String projectRoot;

    public AntPatternTestDetectorFilter(String projectRoot,
                                        @Nullable String[] includeFilter,
                                        @Nullable String[] excludeFilter) {
        this.includeFilter = includeFilter;
        this.excludeFilter = excludeFilter;
        this.projectRoot = projectRoot.endsWith(File.separator) ? projectRoot : projectRoot + File.separator;
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
        final String path = file.getPath();
        final String relative = path.startsWith(projectRoot) ? path.substring(projectRoot.length()) : path;

        return FilterUtils.isIncluded(relative, excludeFilter, includeFilter, true);
    }

    @Override
    public String toString() {
        return clover.com.google.common.base.MoreObjects.toStringHelper(this)
                .add("includeFilter", includeFilter)
                .add("excludeFilter", excludeFilter)
                .add("projectRoot", projectRoot)
                .toString();
    }
}
