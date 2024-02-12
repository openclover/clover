package org.openclover.core.reporters;

import org.openclover.core.reporters.filters.AntPatternTestFilter;
import org.openclover.core.util.FilterUtils;
import org.openclover.runtime.Logger;

public class TestSelectionHelper {
    public static final String TESTS_INCLUDE_PATTERN_PARAM = "--testsIncludePattern";
    public static final String TESTS_EXCLUDE_PATTERN_PARAM = "--testsExcludePattern";
    public static final String SOURCE_ROOT_PARAM = "--sourceRoot";

    private TestSelectionHelper() {
    }

    public static String getParamsUsage() {
        return  "         " + TESTS_INCLUDE_PATTERN_PARAM + " <string>\tAnt-style pattern of files containing test classes and utilities.\n"+
                "\t\t\t\tIf unspecified Clover uses default test detection logic.\n\n"+
                "         " + TESTS_EXCLUDE_PATTERN_PARAM + " <string>\tAnt-style pattern of files NOT containing test classes and utilities.\n\n"+
                "         " + SOURCE_ROOT_PARAM + " <string>\tSource root path prefix that will be ignored when evaluating the test inclusion patterns.\n\n";
    }

    /**
     * Parse program arguments to extract (optional) Ant-style test selection filter.
     * @param cfg Current to be configured
     * @param args program arguments to be parsed
     * @throws ArrayIndexOutOfBoundsException when the parameter is the last one and the value is mising
     */
    @SuppressWarnings({"AssignmentToForLoopParameter"})
    public static void configureTestSelectionFilter(Current cfg, String[] args) {
        String includeFilter = null;
        String excludeFilter = null;
        String sourceRoot = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case TESTS_INCLUDE_PATTERN_PARAM:
                    includeFilter = args[++i];
                    break;
                case TESTS_EXCLUDE_PATTERN_PARAM:
                    excludeFilter = args[++i];
                    break;
                case SOURCE_ROOT_PARAM:
                    sourceRoot = args[++i];
                    break;
            }
        }
        if (includeFilter == null && excludeFilter == null) {
            return;
        }
        final String[] includes = includeFilter == null ? new String[0] : FilterUtils.tokenizePattern(includeFilter);
        final String[] excludes = excludeFilter == null ? new String[0] : FilterUtils.tokenizePattern(excludeFilter);

        final AntPatternTestFilter filter = new AntPatternTestFilter(sourceRoot, includes, excludes);
        cfg.setTestFilter(filter);
        Logger.getInstance().verbose("Setting up Ant-pattern based test filter.");
    }
}
