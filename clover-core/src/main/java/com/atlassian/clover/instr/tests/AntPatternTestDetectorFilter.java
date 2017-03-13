package com.atlassian.clover.instr.tests;

import com.atlassian.clover.util.FilterUtils;

import java.io.File;

public class AntPatternTestDetectorFilter implements TestDetector {
    final String[] includeFilter;
    final String[] excludeFilter;
    private final String projectRoot;

    public AntPatternTestDetectorFilter(String projectRoot, String[] includeFilter, String[] excludeFilter) {
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

        return FilterUtils.isIncluded(relative, excludeFilter, includeFilter, false);
    }
}
