package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.buildutil.test.junit.DynamicallyNamedTestBase
import org.openclover.buildutil.test.junit.GroovyVersions
import org.openclover.core.cfg.instr.InstrumentationConfig
import org.openclover.core.context.ContextStore
import org.openclover.groovy.test.junit.CloverDbTestMixin
import org.openclover.groovy.test.junit.JavaExecutorMixin
import org.openclover.groovy.test.junit.Result
import org.openclover.groovy.test.junit.TestPropertyMixin
import org.openclover.groovy.test.junit.WorkingDirMixin
import org.openclover.runtime.CloverNames

@CompileStatic
abstract class TestBase
        extends DynamicallyNamedTestBase
        implements WorkingDirMixin, CloverDbTestMixin, TestPropertyMixin, JavaExecutorMixin {
    protected File cloverAllResources = new File( "../clover-all/src/main/resources")
    protected File cloverCoreClasses = new File( "../clover-core/target/classes")
    protected File cloverRuntimeClasses = new File( "../clover-runtime/target/classes")
    protected File groverClasses = new File( "target/classes")
    protected File groverMetaInfServices = new File("src/main/assembly")
    protected File servicesFolder = new File( "../clover-ant/src/main/resources")
    protected File loggingProperties = new File( "src/test/resources/logging.properties")
    protected File junitJar = getJUnitJarFromProperty()
    /** Location of hamcrest-core required by JUnit 4.11+ */
    protected File hamcrestJar = getHamcrestJarFromProperty()
    protected File groovyAllJar = getGroovyJarFromProperty()
    protected List<File> additionalGroovyJars = []
    protected File groverConfigDir

    TestBase(String testName) {
        super(testName)
    }

    TestBase(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName)
        this.groovyAllJar = groovyAllJar
        this.additionalGroovyJars = additionalGroovyJars
    }

    void setUp() {
        File workingDir = createWorkingDir()
        reserveCloverDbFile(workingDir)
        groverConfigDir = (File) File.createTempFile("grover", "config", workingDir).with {File dir ->
            dir.delete()
            dir.mkdir()
            dir
        }
    }

    void tearDown() {
        //deleteWorkingDir()
    }

    /**
     * Instrument sources provided as input arguments in a map: file name -> file content
     *
     * @param fileAndSource
     * @param props
     * @param extraClasspath
     * @param decorateConfig
     * @return
     */
    protected Result instrumentAndCompileWithGrover(Map fileAndSource,
                                                    String props = "",
                                                    List<File> extraClasspath = [],
                                                    Closure<InstrumentationConfig> decorateConfig = { InstrumentationConfig cfg -> cfg }) {
        List<File> sourceFiles = fileAndSource.entrySet().collect { Map.Entry<String, String> entry ->
            new File(workingDir, entry.key.toString()).with {
                getParentFile().mkdirs();
                createNewFile();
                append(entry.value);
                it
            }
        }

        instrumentAndCompileWithGrover(sourceFiles, props, extraClasspath, decorateConfig)
    }

    /**
     * Instrument sources provided in a list of files
     *
     * @param sourceFiles
     * @param props
     * @param extraClasspath
     * @param decorateConfig
     * @return
     */
    protected Result instrumentAndCompileWithGrover(List<File> sourceFiles,
                                                    String props = "",
                                                    List<File> extraClasspath = [],
                                                    Closure<InstrumentationConfig> decorateConfig = { InstrumentationConfig cfg -> cfg }) {
        if (groovyAllJar == null) {
            throw new IllegalArgumentException("No -Dgroovy-all.jar property specified")
        }

        File groverInstrConfigFile = new File(groverConfigDir, CloverNames.getGroverConfigFileName())
        decorateConfig(new InstrumentationConfig()).with { InstrumentationConfig it ->
            if (it != null) { //Groovy bug where null.with { it == null } => false
                setInitstring(db.getAbsolutePath())
                setIncludedFiles(sourceFiles as List)
                saveToFile(groverInstrConfigFile)
                ContextStore.saveCustomContexts(it)
            }
        }

       launchJava """
           ${props}
           -Djava.io.tmpdir=${System.getProperty("java.io.tmpdir")}
           -Dclover.logging.level=verbose
           -Djava.util.logging.config.file=${loggingProperties.absolutePath}
           -Dawt.headless=true
           -classpath
           ${ ([groovyAllJar, calcRepkgJar()] + additionalGroovyJars).findAll { it != null }.join(File.pathSeparator)}
           org.codehaus.groovy.tools.FileSystemCompiler
           -classpath
           ${calcCompilationClasspath([ groverConfigDir ] + extraClasspath)}
           -d
           ${workingDir.getAbsolutePath()}
           ${sourceFiles.collect {File f -> f.absolutePath }.join(" ")}
        """
    }

    /**
     * Run java class
     * @param className name of class to run
     * @param props extra java properties
     * @return Result result of process execution (error code, stdout, strerr)
     */
    Result run(String className, List<String> props = [], List<File> extraClasspath = []) {
        List<File> mergedClasspath = [workingDir, calcRepkgJar()] + additionalGroovyJars + extraClasspath
        return launchJava("""
                ${props.join("\n")}
                -Djava.io.tmpdir=${System.getProperty("java.io.tmpdir")}
                -Djava.util.logging.config.file=${loggingProperties.absolutePath}
                -Djava.awt.headless=true
                -classpath ${calcCompilationClasspath(mergedClasspath.findAll { it != null }.toList())}
                ${className}
            """)
    }

    /**
     * Run java class and assert that return code == 0 and stderr is empty
     * @param className name of class to run
     * @param props extra java properties
     * @return Result result of process execution (error code, stdout, strerr)
     */
    Result runWithAsserts(String className, List<String> props = []) {
        def result = run(className, props)
        assertEquals "exit code=${result.getExitCode()}", 0, result.getExitCode()
        assertTrue "stderr = ${result.getStdErr()}", result.getStdErr() == null || result.getStdErr().length() == 0
        return result
    }

    File calcRepkgJar() {
        return cloverRepkgRuntimeJar?.exists() ? cloverRepkgRuntimeJar : null
    }

    protected String calcCompilationClasspath(List<File> others = []) {
        return (others + cloverLibs + [
                groovyAllJar, cloverAllResources, cloverCoreClasses, cloverRuntimeClasses,
                groverClasses, groverMetaInfServices, servicesFolder, junitJar, hamcrestJar
            ])
                .findAll { it != null }
                .collect { it.absolutePath }
                .join(File.pathSeparator)
    }

    static File getGroovyJarFromProperty() {
        def groovyVer = System.getProperty("clover-groovy.test.groovy.ver") ?: GroovyVersions.DEFAULT_VERSION
        new File("target/test-dependencies/groovy-${groovyVer}.jar")
    }

    static File getHamcrestJarFromProperty() {
        def hamcrestVer = System.getProperty("clover-groovy.test.hamcrest.ver") ?: "1.3"
        new File("target/test-dependencies/hamcrest-core-${hamcrestVer}.jar")
    }

    static File getJUnitJarFromProperty() {
        def junitVer = System.getProperty("clover-groovy.test.junit.ver") ?: "1.4"
        new File("target/test-dependencies/junit-${junitVer}.jar")
    }

    File getCloverRepkgRuntimeJar() {
        getFileProp("repkg.clover.jar", false)
    }

    static List<File> getCloverLibs() {
        new File("target/test-dependencies").listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                // skip groovy/spock/junit as we will test with specific versions
                return !pathname.name.matches("(groovy|spock|junit)-.*\\.jar")
            }
        }).toList()
    }
}
