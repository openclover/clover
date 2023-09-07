package com.atlassian.clover.test.junit

import java.util.regex.Matcher

/** Mixin for iterating over Groovy versions in the project  */
class GroovyCombinatorMixin {
    void eachGroovy(File groovyLibDir, Closure filter = {true}, Closure c) {
        findGroovyAllVersionsAndJars(groovyLibDir, filter).each(c)
    }

    List findGroovyAllVersionsAndJars(File groovyLibDir, Closure filter = {true}) {
        def isGroovyAllJar = /groovy-(.*)\.jar/
        groovyLibDir.list().findAll {
            Matcher matcher = it =~ isGroovyAllJar
            if (matcher) {
                filter.call(matcher[0][1])
            } else {
                false
            }
        }.collect {String name ->
            [(name =~ isGroovyAllJar)[0][1], new File(groovyLibDir, name)]
        }
    }
}