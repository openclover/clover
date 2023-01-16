package com.atlassian.clover.test.junit

class GroovyVersions {
    static String DEFAULT_VERSION = "2.5.9";

    static Closure CHOOSE_DEFAULT_SUPPORTED_IF_NULL_ELSE_SPLIT = { String it ->
        it == null ? [DEFAULT_VERSION] : it.split(",")
    }

    /** Return hardcoded list of latest major versions if input string is <code>null</code>, otherwise split the string */
    static Closure CHOOSE_LATEST_MAJOR_IF_NULL_ELSE_SPLIT = { String it ->
        it == null ? ["2.0.8", "2.1.9", "2.2.2", "2.3.11", "2.4.21", "2.5.9"] : it.split(",")
    }
}
