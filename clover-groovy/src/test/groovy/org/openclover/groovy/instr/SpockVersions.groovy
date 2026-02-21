package org.openclover.groovy.instr

class SpockVersions {
    static String DEFAULT_VERSION = "1.0-groovy-2.0,1.0-groovy-2.3," +
            "1.3-groovy-2.4,1.3-groovy-2.5," +
            "2.3-groovy-2.5,2.3-groovy-3.0,2.3-groovy-4.0," +
            "2.4-groovy-2.5,2.4-groovy-3.0,2.4-groovy-4.0,2.4-groovy-5.0"

    static Closure CHOOSE_DEFAULT_IF_NULL_ELSE_SPLIT = { String it ->
        it == null || it.empty ? DEFAULT_VERSION.split(",") : it.split(",")
    }
}
