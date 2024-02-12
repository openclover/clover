package org.openclover.groovy.test.junit

import groovy.transform.CompileStatic
import org.openclover.core.util.JavaEnvUtils
import org.openclover.core.util.collections.Pair

import static org.openclover.core.util.JavaEnvUtils.JAVA_11
import static org.openclover.core.util.JavaEnvUtils.JAVA_17
import static org.openclover.core.util.JavaEnvUtils.JAVA_8

@CompileStatic
trait JavaVersionMixin {

    boolean shouldRunInCurrentJava(String groovyVersion) {
        /** groovy version prefix - java version range */
        Map<String, Pair<String, String>> GROOVY_TO_JAVA_VERSIONS = [
                "2.": Pair.of(JAVA_8, JAVA_8),
                "3.": Pair.of(JAVA_8, JAVA_11),
                "4.": Pair.of(JAVA_8, JAVA_17)
        ]

        Pair<String, String> javaRange = null
        for (Map.Entry<String, Pair<String, String>> entry : GROOVY_TO_JAVA_VERSIONS.entrySet()) {
            if (groovyVersion.startsWith(entry.key)) {
                javaRange = entry.value
                break;
            }
        }

        return javaRange != null &&
                JavaEnvUtils.isAtLeastJavaVersion(javaRange.first) &&
                JavaEnvUtils.isAtMostJavaVersion(javaRange.second)
    }
}