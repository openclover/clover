package com.atlassian.clover.ant.groovy

import com.atlassian.clover.test.junit.AntCombinatorMixin
import com.atlassian.clover.test.junit.DefaultTestSelector
import com.atlassian.clover.test.junit.GroovyCombinatorMixin
import com.atlassian.clover.test.junit.TestPropertyMixin
import com.atlassian.clover.test.junit.WorkingDirMixin
import java.lang.reflect.Method

import com.atlassian.clover.test.junit.IncludeExcludeMixin
import com.atlassian.clover.test.junit.GroovyVersions
import com.atlassian.clover.test.junit.AntVersions
import com.atlassian.clover.test.junit.GroovyVersionStart
import com.atlassian.clover.versions.LibraryVersion

@Mixin ([TestPropertyMixin, GroovyCombinatorMixin, AntCombinatorMixin, WorkingDirMixin, IncludeExcludeMixin])
public class TestSuite extends junit.framework.TestSuite {
    static Map<Class, Closure> TEST_CLASSES_AND_SELECTORS = [
        (CompilationTest): DefaultTestSelector.instance.closure
    ]
    
    static List GROOVY_VERSION_INCLUDES = System.getProperty("clover.test.groovyversion.includes").with(GroovyVersions.CHOOSE_DEFAULT_SUPPORTED_IF_NULL_ELSE_SPLIT)
    static List GROOVY_VERSION_EXCLUDES = System.getProperty("clover.test.groovyversion.excludes").with(GroovyVersions.SPLIT)

    static List ANT_VERSION_INCLUDES = System.getProperty("clover.test.antversion.includes").with(AntVersions.CHOOSE_DEFAULT_SUPPORTED_IF_NULL_ELSE_SPLIT)
    static List ANT_VERSION_EXCLUDES = System.getProperty("clover.test.antversion.excludes").with(AntVersions.SPLIT)

    File projectDir = getFileProp("project.dir")
    File cloverRuntimeJar = new File(projectDir, "clover-ant/target/clover.jar")
    File antHomesDir = new File(projectDir, "target/dependencies/ant")
    File groovyLibDir = new File(projectDir, "target/dependencies")
    File cloverRepkgRuntimeJar = getFileProp("repkg.clover.jar", false)

    public static TestSuite suite() { return new TestSuite() }

    protected TestSuite() {
        eachAnt(antHomesDir, { shouldTestWithAnt(it) }) {String antVersion, File antHome ->
            eachGroovy(groovyLibDir, { shouldTestWithGroovy(it) }) {String groovyVersion, File groovyAllJar ->
                TEST_CLASSES_AND_SELECTORS.each {Class c, Closure selector ->
                    c.getDeclaredMethods().findAll(selector).each {Method m ->
                        //Run the test if groovy start version is not specified or if it is and we're >= to it
                        if (!m.isAnnotationPresent(GroovyVersionStart.class)
                            || new LibraryVersion(m.getAnnotation(GroovyVersionStart.class).value()).compareTo(new LibraryVersion(groovyVersion)) <= 0) {

                            String versionedMethodName = "${m.getName()}_For_Ant${antVersion}_And_Groovy${groovyVersion}".toString()
                            def project =
                            new AntProjectSimulacrum(
                                methodName: m.getName(),
                                testVersionedName: versionedMethodName,
                                antVersion: antVersion.replace('.', '_'),
                                antHome: antHome,
                                groovyVersion: groovyVersion.replace('.', '_'),
                                groovyAllJar: groovyAllJar,
                                cloverRuntimeJar: cloverRuntimeJar,
                                cloverRepkgRuntimeJar: cloverRepkgRuntimeJar)
                            //Let's hope the first ctor is the right one!
                            c.declaredConstructors.find {it.parameterTypes.length == 3}.newInstance(m.getName(), versionedMethodName, project).with { addTest(it); project.test = it }
                        }
                    }
                }
            }
        }

        if (countTestCases() == 0) {
            throw new IllegalArgumentException(
                "No tests configured to run.\n" +
                "Included Groovy versions: ${GROOVY_VERSION_INCLUDES}. Excluded Groovy versions: ${GROOVY_VERSION_EXCLUDES}. Groovy versions seen: ${findGroovyAllVersionsAndJars(groovyLibDir)}\n" +
                "Included Ant versions: ${ANT_VERSION_INCLUDES}. Excluded Ant versions: ${ANT_VERSION_EXCLUDES}. Ant versions seen: ${findAntVersionsAndHomes(antHomesDir)}")
        }
    }

    public boolean shouldTestWithGroovy(String version) {
        shouldInclude(GROOVY_VERSION_INCLUDES, GROOVY_VERSION_EXCLUDES, version)
    }

    public boolean shouldTestWithAnt(String version) {
        shouldInclude(ANT_VERSION_INCLUDES, ANT_VERSION_EXCLUDES, version)
    }
}
