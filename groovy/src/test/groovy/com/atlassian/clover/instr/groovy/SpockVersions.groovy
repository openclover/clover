package com.atlassian.clover.instr.groovy

class SpockVersions {
    static String DEFAULT_VERSION = "spock-core-0.7-groovy-2.0," +
            "spock-core-1.0-groovy-2.0,spock-core-1.0-groovy-2.3,spock-core-1.0-groovy-2.4"

    static Closure CHOOSE_DEFAULT_IF_NULL_ELSE_SPLIT = { String it ->
        it == null ? DEFAULT_VERSION.split(",") : it.split(",")
    }
}
