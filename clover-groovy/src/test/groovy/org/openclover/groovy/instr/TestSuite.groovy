package org.openclover.groovy.instr

import com.atlassian.clover.test.junit.DefaultTestSelector
import org.openclover.groovy.test.junit.GroovyCombinatorMixin
import org.openclover.groovy.test.junit.JavaVersionMixin
import org.openclover.groovy.test.junit.TestPropertyMixin
import java.lang.reflect.Method

import org.openclover.groovy.test.junit.IncludeExcludeMixin
import com.atlassian.clover.test.junit.GroovyVersions
import com.atlassian.clover.versions.LibraryVersion
import com.atlassian.clover.test.junit.GroovyVersionStart

class TestSuite
        extends junit.framework.TestSuite
        implements GroovyCombinatorMixin, TestPropertyMixin, IncludeExcludeMixin, JavaVersionMixin {

    static Map<Class, Closure> TEST_CLASSES_AND_SELECTORS = [
        (GroovyProfilesTest): DefaultTestSelector.instance.closure,
        (GroovyModellingTest): DefaultTestSelector.instance.closure,
        (GroovyCoverageTest): DefaultTestSelector.instance.closure,
        (GroovyReportTest): DefaultTestSelector.instance.closure
    ]

    static List GROOVY_VERSION_INCLUDES = System.getProperty("clover.test.groovyversion.includes")
            .with(GroovyVersions.CHOOSE_LATEST_MAJOR_IF_NULL_ELSE_SPLIT)

    private File groovyLibDir = new File("target/test-dependencies")

    static File getCommonsCliJar() {
        new File("target/test-dependencies/commons-cli-1.2.jar")
    }

    static File getAsmJar() {
        new File("target/test-dependencies/asm-4.1.jar")
    }

    static File getAntlrJar() {
        new File("target/test-dependencies/antlr-2.7.7.jar")
    }

    static TestSuite suite() { return new TestSuite() }

    TestSuite() {
        eachGroovy(groovyLibDir, { shouldTestWithGroovyJar(it) }) {String version, File groovyAllJar ->
            TEST_CLASSES_AND_SELECTORS.each {Class testClass, Closure selector ->
                testClass.getDeclaredMethods().findAll(selector).each {Method m ->
                    //Run the test if groovy start version is not specified or if it is and we're >= to it
                    if (!m.isAnnotationPresent(GroovyVersionStart.class)
                        || new LibraryVersion(m.getAnnotation(GroovyVersionStart.class).value()).compareTo(new LibraryVersion(version)) <= 0) {
                        List<File> additionalLibraries = [ commonsCliJar, asmJar, antlrJar ]
                        testClass.declaredConstructors
                                .find {it.parameterTypes.length == 4}
                                .newInstance(m.name, "${m.name}_For_Groovy_${version}".toString(), groovyAllJar, additionalLibraries)
                                .with { addTest(it) }
                    }
                }
            }
        }
        if (countTestCases() == 0) {
            throw new IllegalArgumentException(
                "No tests configured to run. Included Groovy versions: ${GROOVY_VERSION_INCLUDES}. Groovy versions seen: ${findGroovyAllVersionsAndJars(groovyLibDir)}")
        }
    }

    boolean shouldTestWithGroovyJar(String version) {
        shouldInclude(GROOVY_VERSION_INCLUDES, version) && shouldRunInCurrentJava(version)
    }
}
