package org.openclover.buildutil.test.junit

class AntVersions {
    static String DEFAULT_VERSION = "1.10.13"

    static Closure<List<String>> CHOOSE_DEFAULT_SUPPORTED_IF_NULL_ELSE_SPLIT = { String it ->
        it == null || it.empty ? [DEFAULT_VERSION] : it.split(",").toList()
    }
}
