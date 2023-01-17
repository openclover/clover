package com.atlassian.clover.instr.groovy

import com.atlassian.clover.cfg.instr.InstrumentationConfig
import com.atlassian.clover.test.junit.CloverDbTestMixin
import com.atlassian.clover.test.junit.DynamicallyNamedTestBase
import com.atlassian.clover.test.junit.GroovyVersions
import com.atlassian.clover.test.junit.JavaExecutorMixin
import com.atlassian.clover.test.junit.Result
import com.atlassian.clover.test.junit.TestPropertyMixin
import com.atlassian.clover.test.junit.WorkingDirMixin
import com.atlassian.clover.CloverNames
import com.atlassian.clover.context.ContextStore

@Mixin ([WorkingDirMixin, CloverDbTestMixin, TestPropertyMixin, JavaExecutorMixin])
abstract class TestBase extends DynamicallyNamedTestBase {
    protected File cloverCoreClasses = new File( "../clover-core/target/classes")
    protected File cloverRuntimeClasses = new File( "../clover-runtime/target/classes")
    protected File groverClasses = new File( "target/classes")
    protected File servicesFolder = new File( "../clover-ant/src/main/resources")
    protected File loggingProperties = new File( "src/test/resources/logging.properties")
    protected File junitJar = getJUnitJarFromProperty()
    /** Location of hamcrest-core required by JUnit 4.11+ */
    protected File hamcrestJar = getHamcrestJarFromProperty()
    protected File groovyAllJar = getGroovyJarFromProperty()
    protected File groverConfigDir

    TestBase(String testName) {
        super(testName)
    }

    TestBase(String methodName, String specificName, File groovyAllJar) {
        super(methodName, specificName);
        this.groovyAllJar = groovyAllJar
    }

    void setUp() {
        createWorkingDir()
        reserveCloverDbFile()
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
    protected Result instrumentAndCompileWithGrover(Map fileAndSource, props = "", extraClasspath = [], decorateConfig = { it }) {
        def sourceFiles = fileAndSource.entrySet().collect {Map.Entry<String, String> entry ->
            new File(workingDir, entry.key).with { getParentFile().mkdirs(); createNewFile(); append(entry.value); it }
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
    protected Result instrumentAndCompileWithGrover(List sourceFiles, props = "", extraClasspath = [], decorateConfig = { it }) {
        if (groovyAllJar == null) {
            throw new IllegalArgumentException("No -Dgroovy-all.jar property specified")
        }

        File groverInstrConfigFile = new File(groverConfigDir, CloverNames.getGroverConfigFileName())
        decorateConfig(new InstrumentationConfig()).with {
            if (!it.equals(null)) { //Groovy bug where null.with { it == null } => false
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
           ${ ([groovyAllJar.getAbsolutePath(), calcRepkgJarPath()] + extraClasspath).findAll { it != null }.join(File.pathSeparator)}
           org.codehaus.groovy.tools.FileSystemCompiler
           -classpath
           ${calcCompilationClasspath([groverConfigDir.getAbsolutePath()] + extraClasspath)}
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
    Result run(String className, props = [], extraClasspath = []) {
        List mergedClasspath = [workingDir.getAbsolutePath(), calcRepkgJarPath()] + extraClasspath
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
    Result runWithAsserts(String className, props = []) {
        def result = run(className, props)
        assertEquals "exit code=${result.getExitCode()}", 0, result.getExitCode()
        assertTrue "stderr = ${result.getStdErr()}", result.getStdErr() == null || result.getStdErr().length() == 0
        return result
    }

    String calcRepkgJarPath() {
        return cloverRepkgRuntimeJar?.exists() ? cloverRepkgRuntimeJar.absolutePath : null
    }

    protected String calcCompilationClasspath(List others = []) {
        return (others +
            cloverLibs.collect { it.absolutePath } +
            [
                groovyAllJar.absolutePath,
                cloverCoreClasses.absolutePath,
                cloverRuntimeClasses.absolutePath,
                groverClasses.absolutePath,
                servicesFolder.absolutePath,
                junitJar.absolutePath,
                hamcrestJar.absolutePath
            ]).findAll { it != null }.join(File.pathSeparator)
    }

    File getGroovyJarFromProperty() {
        def groovyVer = System.getProperty("clover-groovy.test.groovy.ver") ?: GroovyVersions.DEFAULT_VERSION
        new File("target/test-dependencies/groovy-${groovyVer}.jar")
    }

    File getSpockJarFromProperty() {
        def spockVer = System.getProperty("clover-groovy.test.spock.ver") ?: "0.7-groovy-2.0"
        new File("target/test-dependencies/spock-core-${spockVer}.jar")
    }

    File getHamcrestJarFromProperty() {
        def hamcrestVer = System.getProperty("clover-groovy.test.hamcrest.ver") ?: "1.3"
        new File("target/test-dependencies/hamcrest-core-${hamcrestVer}.jar")
    }

    File getJUnitJarFromProperty() {
        def junitVer = System.getProperty("clover-groovy.test.junit.ver") ?: "1.4"
        new File("target/test-dependencies/junit-${junitVer}.jar")
    }

    File getCloverRepkgRuntimeJar() {
        getFileProp("repkg.clover.jar", false)
    }

    File[] getCloverLibs() {
        new File("target/test-dependencies").listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                return pathname.name.matches("clover-.*\\.jar")
            }
        })
    }
}