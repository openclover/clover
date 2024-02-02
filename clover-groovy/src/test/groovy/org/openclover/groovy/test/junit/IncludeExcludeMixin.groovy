package org.openclover.groovy.test.junit

import groovy.transform.CompileStatic

@CompileStatic
trait IncludeExcludeMixin {
    boolean shouldInclude(List<String> includes, String version) {
        boolean include = includes == null || "all" in includes || version in includes
        return include
    }
}
