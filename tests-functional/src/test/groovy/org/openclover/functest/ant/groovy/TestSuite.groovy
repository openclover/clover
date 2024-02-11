package org.openclover.functest.ant.groovy

import org.openclover.groovy.test.junit.AntCombinatorMixin
import org.openclover.buildutil.test.junit.DefaultTestSelector
import org.openclover.groovy.test.junit.GroovyCombinatorMixin
import org.openclover.groovy.test.junit.JavaVersionMixin
import org.openclover.groovy.test.junit.TestPropertyMixin
import org.openclover.groovy.test.junit.WorkingDirMixin
import groovy.transform.CompileStatic
import junit.framework.TestCase

import java.lang.reflect.Constructor
import java.lang.reflect.Method

import org.openclover.groovy.test.junit.IncludeExcludeMixin
import org.openclover.buildutil.test.junit.GroovyVersions
import org.openclover.buildutil.test.junit.AntVersions
import org.openclover.buildutil.test.junit.GroovyVersionStart
import com.atlassian.clover.versions.LibraryVersion

@CompileStatic
class TestSuite extends junit.framework.TestSuite
        implements TestPropertyMixin, GroovyCombinatorMixin, AntCombinatorMixin, WorkingDirMixin, IncludeExcludeMixin,
                JavaVersionMixin {

    static Map<Class<? extends TestCase>, Closure> TEST_CLASSES_AND_SELECTORS = [
        (CompilationTest): DefaultTestSelector.instance.closure
    ] as Map<Class<? extends TestCase>, Closure>
    
    static List<String> GROOVY_VERSION_INCLUDES = System.getProperty("clover.test.groovyversion.includes", "")
            .with(GroovyVersions.CHOOSE_DEFAULT_SUPPORTED_IF_NULL_ELSE_SPLIT)

    static List<String> ANT_VERSION_INCLUDES = System.getProperty("clover.test.antversion.includes", "")
            .with(AntVersions.CHOOSE_DEFAULT_SUPPORTED_IF_NULL_ELSE_SPLIT)

    File testDependenciesDir = new File("target/test-dependencies")

    static TestSuite suite() { return new TestSuite() }

    protected TestSuite() {
        File antHomesDir = testDependenciesDir
        File groovyLibDir = testDependenciesDir

        eachAnt(antHomesDir, { String it -> shouldTestWithAnt(it) }) { String antVersion, File antJar ->
            eachGroovy(groovyLibDir, { String it -> shouldTestWithGroovy(it) }) { String groovyVersion, File groovyAllJar ->
                TEST_CLASSES_AND_SELECTORS.each { Class<? extends TestCase> c, Closure selector ->
                    c.getDeclaredMethods().findAll(selector).each { Method m ->
                        //Run the test if groovy start version is not specified or if it is and we're >= to it
                        if (!m.isAnnotationPresent(GroovyVersionStart.class)
                            || new LibraryVersion(m.getAnnotation(GroovyVersionStart.class).value()).compareTo(new LibraryVersion(groovyVersion)) <= 0) {

                            String versionedMethodName = "${m.getName()}_For_Ant${antVersion}_And_Groovy${groovyVersion}".toString()
                            def project = new AntProjectSimulacrum(
                                methodName: m.getName(),
                                testVersionedName: versionedMethodName,
                                testDependenciesDir: testDependenciesDir,
                                antVersion: antVersion,
                                groovyVersion: groovyVersion,
                                cloverRuntimeJar: cloverRuntimeJar,
                                cloverRepkgRuntimeJar: cloverRepkgRuntimeJar)

                            Constructor<? extends TestCase> tcConstr = findThreeArgConstructor(c)
                            TestCase tc = tcConstr.newInstance(m.getName(), versionedMethodName, project)
                            addTest(tc)
                            project.test = tc
                        }
                    }
                }
                return
            }
        }

        if (countTestCases() == 0) {
            throw new IllegalArgumentException(
                "No tests configured to run.\n" +
                "Included Groovy versions: ${GROOVY_VERSION_INCLUDES}. Groovy versions seen: ${findGroovyAllVersionsAndJars(groovyLibDir)}\n" +
                "Included Ant versions: ${ANT_VERSION_INCLUDES}. Ant versions seen: ${findAntVersions(antHomesDir)}")
        }
    }

    static Constructor<? extends TestCase> findThreeArgConstructor(Class<? extends TestCase> c) {
        for (Constructor<?> it : c.declaredConstructors) {
            if (it.parameterTypes.length == 3) {
                return (Constructor<? extends TestCase>) it
            }
        }
        null
    }

    boolean shouldTestWithGroovy(String version) {
        shouldInclude(GROOVY_VERSION_INCLUDES, version) && shouldRunInCurrentJava(version)
    }

    boolean shouldTestWithAnt(String version) {
        shouldInclude(ANT_VERSION_INCLUDES, version)
    }

    File getCloverRepkgRuntimeJar() {
        getFileProp("repkg.clover.jar", false)
    }

    static File getCloverRuntimeJar() {
        // find clover-X.Y.Z-suffix.jar, but not -javadoc or -sources or clover-ant-
        new File("target").listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                pathname.name.matches("clover-[0-9]+.*\\.jar") &&
                        !pathname.name.matches("-javadoc\\.jar") &&
                        !pathname.name.matches("-sources\\.jar")
            }
        })[0]
    }
}
