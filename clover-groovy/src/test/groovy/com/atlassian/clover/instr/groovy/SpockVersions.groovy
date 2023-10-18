package com.atlassian.clover.instr.groovy

class SpockVersions {
    static String DEFAULT_VERSION = "spock-core-1.0-groovy-2.0,spock-core-1.0-groovy-2.3," +
            "spock-core-1.3-groovy-2.4,spock-core-1.3-groovy-2.5," +
            "spock-core-2.3-groovy-2.5,spock-core-2.3-groovy-3.0,spock-core-2.3-groovy-4.0"

    static Closure CHOOSE_DEFAULT_IF_NULL_ELSE_SPLIT = { String it ->
        it == null || it.empty ? DEFAULT_VERSION.split(",") : it.split(",")
    }
}
