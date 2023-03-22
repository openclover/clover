package com.atlassian.clover.instr.groovy

import com.atlassian.clover.test.junit.DefaultTestSelector
import com.atlassian.clover.test.junit.GroovyCombinatorMixin
import com.atlassian.clover.test.junit.GroovyVersionStart
import com.atlassian.clover.test.junit.GroovyVersions
import com.atlassian.clover.test.junit.IncludeExcludeMixin
import com.atlassian.clover.test.junit.SpockCombinatorMixin
import com.atlassian.clover.test.junit.TestPropertyMixin
import com.atlassian.clover.versions.LibraryVersion

import java.lang.reflect.Method

@Mixin ([GroovyCombinatorMixin, SpockCombinatorMixin, TestPropertyMixin, IncludeExcludeMixin])
class GroovySpockTestSuite extends junit.framework.TestSuite {
    static Map<Class, Closure> TEST_CLASSES_AND_SELECTORS = [
        (GroovySpockTest): DefaultTestSelector.instance.closure,
    ]

    /** Spock versions to test against */
    static List SPOCK_VERSION_INCLUDES = System.getProperty("clover.test.spockversion.includes").with(SpockVersions.CHOOSE_DEFAULT_IF_NULL_ELSE_SPLIT)

    /** Groovy versions to test against */
    static List GROOVY_VERSION_INCLUDES = System.getProperty("clover.test.groovyversion.includes").with(GroovyVersions.CHOOSE_LATEST_MAJOR_IF_NULL_ELSE_SPLIT)

    private File spockLibDir = new File("target/test-dependencies")
    private File groovyLibDir = new File("target/test-dependencies")

    static GroovySpockTestSuite suite() { return new GroovySpockTestSuite() }

    GroovySpockTestSuite() {
        // for all spock versions
        eachSpock(spockLibDir, { shouldTestWithSpockJar(it) }) {
            String spockVersion, File spockJar ->

            // for all groovy versions find one(s) matching spock-groovy
            eachGroovy(groovyLibDir, { shouldTestWithGroovyJar(it) && doesGroovyMatchSpockVersion(it, spockVersion) }) {
                String groovyVersion, File groovyAllJar ->

                // run all test clases against every spock-groovy combination
                TEST_CLASSES_AND_SELECTORS.each { Class testClass, Closure testSelector ->

                    testClass.getDeclaredMethods().findAll(testSelector).each { Method testMethod ->
                        //Run the test if groovy start version is not specified or if it is and we're >= to it
                        if (!testMethod.isAnnotationPresent(GroovyVersionStart.class)
                                || new LibraryVersion(testMethod.getAnnotation(GroovyVersionStart.class).value()).
                                        compareTo(new LibraryVersion(groovyVersion)) <= 0) {

                            // expected constructor signature: (methodName, specificName, groovyAllJar, [ spockJar ])
                            testClass.declaredConstructors.find { it.parameterTypes.length == 4 }
                                    .newInstance(
                                            testMethod.name,
                                            "${testMethod.name}_For_Spock_${spockVersion}_and_Groovy_${groovyVersion}".toString(),
                                            groovyAllJar, [ spockJar ]).with { addTest(it) }
                        }
                    }
                }
            }
        }
        if (countTestCases() == 0) {
            println("WARNING: no tests configured to run. Included Spock versions: ${SPOCK_VERSION_INCLUDES}. "
                            + "Spock versions seen: ${findSpockAllVersionsAndJars(spockLibDir)}")
        }
    }

    boolean shouldTestWithSpockJar(String spockVersion) {
        shouldInclude(SPOCK_VERSION_INCLUDES, spockVersion)
    }

    boolean shouldTestWithGroovyJar(String groovyVersion) {
        shouldInclude(GROOVY_VERSION_INCLUDES, groovyVersion)
    }

    /** "groovy major version:spock's groovy version" map */
    static def groovyVsSpock = [
            "1.6" : "1.6",
            "1.7" : "1.7",
            "1.8" : "1.8",
            "2.0" : "2.0",
            "2.1" : "2.0",
            "2.2" : "2.0",
            "2.3" : "2.3",
            "2.4" : "2.4"
    ]

    /**
     * Reads groovy version number from spock version (e.g. "spock-0.7-groovy-2.0" has "2.0") and matches against
     * actual groovy version.
     * @param actualGroovyVersion
     * @param spockVersion
     * @return
     */
    static boolean doesGroovyMatchSpockVersion(String actualGroovyVersion, String spockVersion) {
        def spockGroovyVersionMatcher = spockVersion =~ /spock-core-(.*)-groovy-(.*)/
        String spockGroovyVersion = spockGroovyVersionMatcher[0][2]
        def groovyMajorVersionMatcher = actualGroovyVersion =~ /([0-9]*\.[0-9]*)(.*)/
        String groovyMajorVersion = groovyMajorVersionMatcher[0][1]
        // find proper version in a map (or use spock-x.x-core-groovy-2.4 if not found)
        return groovyVsSpock.get(groovyMajorVersion, "2.4").equals(spockGroovyVersion)
    }

}
