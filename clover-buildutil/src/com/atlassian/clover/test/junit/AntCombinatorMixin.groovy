package com.atlassian.clover.test.junit

import java.util.regex.Matcher

/** Mixin for iterating over Ant versions in the project  */
public class AntCombinatorMixin {
    public def eachAnt(File antHomesDir, Closure filter = {true}, Closure c) {
        findAntVersionsAndHomes(antHomesDir, filter).each(c)
    }

    public List findAntVersionsAndHomes(File antHomesDir, Closure filter = {true}) {
        def isAntDir = /ant-(.*)/
        antHomesDir.list().findAll {
            Matcher matcher = it =~ isAntDir
            if (matcher) {
                filter.call(matcher[0][1])
            } else {
                false
            }
        }.collect {String name ->
            [(name =~ isAntDir)[0][1], new File(antHomesDir, name)]
        }
    }
}