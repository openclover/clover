package com.atlassian.clover.test.junit


trait IncludeExcludeMixin {
    boolean shouldInclude(List includes, String version) {
        boolean include = includes == null || "all" in includes || version in includes
        return include
    }
}
