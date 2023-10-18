package com.atlassian.clover.test.junit

import groovy.transform.CompileStatic

import java.util.regex.Matcher

/**
 * Mixin for iterating over Spock versions in the project
 */
@CompileStatic
trait SpockCombinatorMixin {
    void eachSpock(File spockLibDir, Closure filter = {true}, Closure c) {
        findSpockAllVersionsAndJars(spockLibDir, filter).each(c)
    }

    List findSpockAllVersionsAndJars(File spockLibDir, Closure filter = {true}) {
        def isSpockJar = /^(spock-core-.*-groovy-.*)\.jar$/
        spockLibDir.list().findAll {
            Matcher matcher = it =~ isSpockJar
            if (matcher) {
                filter.call(matcher.group(1))
            } else {
                false
            }
        }.collect {String name ->
            [(name =~ isSpockJar).group(1), new File(spockLibDir, name)]
        }
    }
}