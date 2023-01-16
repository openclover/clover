package com.atlassian.clover.test.junit

import java.util.regex.Matcher

/** Mixin for iterating over Ant versions in the project  */
class AntCombinatorMixin {
    def eachAnt(File antHomesDir, Closure filter = {true}, Closure c) {
        findAntVersions(antHomesDir, filter).each(c)
    }

    List findAntVersions(File antHomesDir, Closure filter = {true}) {
        def isAntJar = /ant-(.*)\.jar/
        antHomesDir.list().findAll {
            Matcher matcher = it =~ isAntJar
            if (matcher) {
                filter.call(matcher[0][1])
            } else {
                false
            }
        }.collect {String name ->
            [(name =~ isAntJar)[0][1], new File(antHomesDir, name)]
        }
    }
}