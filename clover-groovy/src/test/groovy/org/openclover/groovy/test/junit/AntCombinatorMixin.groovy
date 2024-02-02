package org.openclover.groovy.test.junit

import groovy.transform.CompileStatic

import java.util.regex.Matcher

/** Mixin for iterating over Ant versions in the project  */
@CompileStatic
trait AntCombinatorMixin {
    void eachAnt(File antHomesDir, Closure<Boolean> filter = {true}, Closure<Void> c) {
        findAntVersions(antHomesDir, filter).each(c)
    }

    List findAntVersions(File antHomesDir, Closure<Boolean> filter = {true}) {
        def isAntJar = /^ant-(.*)\.jar$/
        antHomesDir.list().findAll( {
            Matcher matcher = it =~ isAntJar
            if (matcher) {
                filter.call(matcher.group(1))
            } else {
                false
            }
        }).collect {String name ->
            [(name =~ isAntJar).group(1), new File(antHomesDir, name)]
        }
    }
}