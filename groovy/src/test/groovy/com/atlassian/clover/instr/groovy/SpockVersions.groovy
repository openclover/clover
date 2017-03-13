package com.atlassian.clover.instr.groovy

public class SpockVersions {
    static String DEFAULT_VERSION = "spock-core-0.6-groovy-1.7,spock-core-0.6-groovy-1.8," +
            "spock-core-0.7-groovy-1.8,spock-core-0.7-groovy-2.0," +
            "spock-core-1.0-groovy-2.0,spock-core-1.0-groovy-2.3,spock-core-1.0-groovy-2.4"

    static Closure CHOOSE_DEFAULT_IF_NULL_ELSE_SPLIT = {
        it.equals(null) ? SpockVersions.DEFAULT_VERSION.split(",") : it.split(",")
    }
}
