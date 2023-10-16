package com.atlassian.clover.test.junit

import com.atlassian.clover.util.JavaEnvUtils
import com.atlassian.clover.util.collections.Pair
import groovy.transform.CompileStatic

import static com.atlassian.clover.util.JavaEnvUtils.JAVA_11
import static com.atlassian.clover.util.JavaEnvUtils.JAVA_17
import static com.atlassian.clover.util.JavaEnvUtils.JAVA_8

@CompileStatic
trait JavaVersionMixin {

    /** groovy version prefix - java version range */
    private final Map<String, Pair<String, String>> GROOVY_TO_JAVA_VERSIONS = [
            "2.x": Pair.of(JAVA_8, JAVA_8),
            "3.x": Pair.of(JAVA_8, JAVA_11),
            "4.x": Pair.of(JAVA_8, JAVA_17)
    ]

    boolean shouldRunInCurrentJava(String groovyVersion) {
        Pair<String, String> javaRange = GROOVY_TO_JAVA_VERSIONS.find {
            groovyVersion.startsWith(it.key)
        }.value

        return javaRange != null &&
                JavaEnvUtils.isAtLeastJavaVersion(javaRange.first) &&
                JavaEnvUtils.isAtMostJavaVersion(javaRange.second)
    }
}