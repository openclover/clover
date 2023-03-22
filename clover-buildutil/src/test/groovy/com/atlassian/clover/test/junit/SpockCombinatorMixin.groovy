package com.atlassian.clover.test.junit

import java.util.regex.Matcher

/**
 * Mixin for iterating over Spock versions in the project
 */
class SpockCombinatorMixin {
    void eachSpock(File spockLibDir, Closure filter = {true}, Closure c) {
        findSpockAllVersionsAndJars(spockLibDir, filter).each(c)
    }

    List findSpockAllVersionsAndJars(File spockLibDir, Closure filter = {true}) {
        def isSpockJar = /(spock-core-.*-groovy-.*)\.jar/
        spockLibDir.list().findAll {
            Matcher matcher = it =~ isSpockJar
            if (matcher) {
                filter.call(matcher[0][1])
            } else {
                false
            }
        }.collect {String name ->
            [(name =~ isSpockJar)[0][1], new File(spockLibDir, name)]
        }
    }
}