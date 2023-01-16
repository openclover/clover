package com.atlassian.clover.test.junit


class IncludeExcludeMixin {
    boolean shouldInclude(List includes, List excludes, String version) {
        boolean include = includes == null || "all" in includes || version in includes
        boolean exclude = excludes != null && version in excludes
        return include && !exclude
    }
}
