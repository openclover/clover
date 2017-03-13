package com.atlassian.clover.instr.groovy

import com.atlassian.clover.test.junit.DefaultTestSelector
import com.atlassian.clover.test.junit.GroovyCombinatorMixin
import com.atlassian.clover.test.junit.TestPropertyMixin
import java.lang.reflect.Method

import com.atlassian.clover.test.junit.IncludeExcludeMixin
import com.atlassian.clover.test.junit.GroovyVersions
import com.atlassian.clover.versions.LibraryVersion
import com.atlassian.clover.test.junit.GroovyVersionStart

@Mixin ([GroovyCombinatorMixin, TestPropertyMixin, IncludeExcludeMixin])
public class TestSuite extends junit.framework.TestSuite {
    static Map<Class, Closure> TEST_CLASSES_AND_SELECTORS = [
        (GroovyProfilesTest): DefaultTestSelector.instance.closure,
        (GroovyModellingTest): DefaultTestSelector.instance.closure,
        (GroovyCoverageTest): DefaultTestSelector.instance.closure,
        (GroovyReportTest): DefaultTestSelector.instance.closure
    ]

    static List GROOVY_VERSION_INCLUDES = System.getProperty("clover.test.groovyversion.includes").with(GroovyVersions.CHOOSE_DEFAULT_SUPPORTED_IF_NULL_ELSE_SPLIT)
    static List GROOVY_VERSION_EXCLUDES = System.getProperty("clover.test.groovyversion.excludes").with(GroovyVersions.SPLIT)

    private File groovyLibDir = new File(getFileProp("project.dir"), "target/dependencies")

    public static TestSuite suite() { return new TestSuite() }

    public TestSuite() {
        eachGroovy(groovyLibDir, { shouldTestWithGroovyJar(it) }) {String version, File groovyAllJar ->
            TEST_CLASSES_AND_SELECTORS.each {Class testClass, Closure selector ->
                testClass.getDeclaredMethods().findAll(selector).each {Method m ->
                    //Run the test if groovy start version is not specified or if it is and we're >= to it
                    if (!m.isAnnotationPresent(GroovyVersionStart.class)
                        || new LibraryVersion(m.getAnnotation(GroovyVersionStart.class).value()).compareTo(new LibraryVersion(version)) <= 0) {
                        testClass.declaredConstructors.find {it.parameterTypes.length == 3}.newInstance(m.name, "${m.name}_For_Groovy_${version}".toString(), groovyAllJar).with { addTest(it) }
                    }
                }
            }
        }
        if (countTestCases() == 0) {
            throw new IllegalArgumentException(
                "No tests configured to run. Included Groovy versions: ${GROOVY_VERSION_INCLUDES}. Excluded Groovy versions: ${GROOVY_VERSION_EXCLUDES}. Groovy versions seen: ${findGroovyAllVersionsAndJars(groovyLibDir)}")
        }
    }

    public boolean shouldTestWithGroovyJar(String version) {
        shouldInclude(GROOVY_VERSION_INCLUDES, GROOVY_VERSION_EXCLUDES, version)
    }
}
