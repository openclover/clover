package org.openclover.groovy.instr

import org.openclover.buildutil.test.junit.DefaultTestSelector
import org.openclover.buildutil.test.junit.GroovyVersionStart
import org.openclover.buildutil.test.junit.GroovyVersions
import org.openclover.core.versions.LibraryVersion
import org.openclover.groovy.test.junit.GroovyCombinatorMixin
import org.openclover.groovy.test.junit.IncludeExcludeMixin
import org.openclover.groovy.test.junit.JavaVersionMixin
import org.openclover.groovy.test.junit.SpockCombinatorMixin
import org.openclover.groovy.test.junit.TestPropertyMixin

import java.lang.reflect.Method

class GroovySpockTestSuite
        extends junit.framework.TestSuite
        implements GroovyCombinatorMixin, SpockCombinatorMixin, TestPropertyMixin, IncludeExcludeMixin, JavaVersionMixin {

    static Map<Class, Closure> TEST_CLASSES_AND_SELECTORS = [
        (GroovySpockTest): DefaultTestSelector.instance.closure,
    ]

    /** Spock versions to test against */
    static List SPOCK_VERSION_INCLUDES = System.getProperty("clover.test.spockversion.includes", "")
            .with(SpockVersions.CHOOSE_DEFAULT_IF_NULL_ELSE_SPLIT)

    /** Groovy versions to test against */
    static List GROOVY_VERSION_INCLUDES = System.getProperty("clover.test.groovyversion.includes", "")
            .with(GroovyVersions.CHOOSE_LATEST_MAJOR_IF_NULL_ELSE_SPLIT)

    private File testDependenciesDir = new File("target/test-dependencies")
    private File spockLibDir = testDependenciesDir
    private File groovyLibDir = testDependenciesDir

    // dependencies of groovy jar (older versions)
    private File commonsCliJar = new File(testDependenciesDir, "commons-cli-1.2.jar")
    private File asmJar = new File(testDependenciesDir, "asm-9.7.jar")
    private File antlrJar = new File(testDependenciesDir, "antlr-2.7.7.jar")

    // used by spock
    private File opentest4jJar = new File(testDependenciesDir, "opentest4j-1.2.0.jar")

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

                            // expected constructor signature: (methodName, specificName, groovyAllJar, [ spockJar ], withJUnit5)
                            boolean withJUnit5 = spockVersion.startsWith("2.")
                            testClass.declaredConstructors.find { it.parameterTypes.length == 5 }
                                    .newInstance(
                                            testMethod.name,
                                            "${testMethod.name}_For_Spock_${spockVersion}_and_Groovy_${groovyVersion}".toString(),
                                            groovyAllJar,
                                            [ spockJar, opentest4jJar, commonsCliJar, asmJar, antlrJar ],
                                            withJUnit5).with {
                                                    addTest(it)
                                            }
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
        shouldInclude(GROOVY_VERSION_INCLUDES, groovyVersion) && shouldRunInCurrentJava(groovyVersion)
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
            "2.4" : "2.4",
            "2.5" : "2.5",
            "3.0" : "3.0",
            "4.0" : "4.0"
    ]

    /**
     * Reads groovy version number from spock version (e.g. "spock-1.0-groovy-2.0" has "2.0") and matches against
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
        // find proper version in a map (or use spock-x.x-core-groovy-4.0 if not found)
        return groovyVsSpock.get(groovyMajorVersion, "4.0").equals(spockGroovyVersion)
    }

}
