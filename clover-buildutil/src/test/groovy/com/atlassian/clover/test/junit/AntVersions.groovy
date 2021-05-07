package com.atlassian.clover.test.junit

class AntVersions {
    static String DEFAULT_VERSION = "1.9.14";

    static Closure SPLIT = { String it ->
        it == null ? null : it.split(",")
    }

    static Closure CHOOSE_DEFAULT_SUPPORTED_IF_NULL_ELSE_SPLIT = { String it ->
        it == null ? [DEFAULT_VERSION] : it.split(",")
    }
}
