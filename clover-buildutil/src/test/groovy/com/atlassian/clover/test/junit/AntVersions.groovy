package com.atlassian.clover.test.junit

class AntVersions {
    static String DEFAULT_VERSION = "1.10.13";

    static Closure CHOOSE_DEFAULT_SUPPORTED_IF_NULL_ELSE_SPLIT = { String it ->
        it == null ? [DEFAULT_VERSION] : it.split(",")
    }
}
