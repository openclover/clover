package com.atlassian.clover.test.junit

import groovy.transform.CompileStatic

import java.util.regex.Matcher

/** Mixin for iterating over Groovy versions in the project  */
@CompileStatic
trait GroovyCombinatorMixin {
    void eachGroovy(File groovyLibDir, Closure<Boolean> filter = {true}, Closure<Void> c) {
        findGroovyAllVersionsAndJars(groovyLibDir, filter).each(c)
    }

    List findGroovyAllVersionsAndJars(File groovyLibDir, Closure<Boolean> filter = {true}) {
        def isGroovyAllJar = /^groovy-(.*)\.jar$/
        groovyLibDir.list().findAll { String it ->
            Matcher matcher = it =~ isGroovyAllJar
            if (matcher) {
                filter.call(matcher.group(1))
            } else {
                false
            }
        }.collect { String name ->
            Matcher matcher = name =~ isGroovyAllJar
            matcher.find()
            [matcher.group(1), new File(groovyLibDir, name)]
        }
    }
}