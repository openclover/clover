package com.atlassian.clover.instr.groovy

import com.atlassian.clover.cfg.instr.InstrumentationConfig
import com.atlassian.clover.test.junit.CloverDbTestMixin
import com.atlassian.clover.test.junit.DynamicallyNamedTestBase
import com.atlassian.clover.test.junit.JavaExecutorMixin
import com.atlassian.clover.test.junit.Result
import com.atlassian.clover.test.junit.TestPropertyMixin
import com.atlassian.clover.test.junit.WorkingDirMixin
import com.atlassian.clover.CloverNames
import com.atlassian.clover.context.ContextStore

@Mixin ([WorkingDirMixin, CloverDbTestMixin, TestPropertyMixin, JavaExecutorMixin])
public abstract class TestBase extends DynamicallyNamedTestBase {
    protected File cloverRepkgRuntimeJar = getFileProp("repkg.clover.jar", false)
    protected File cloverCoreClasses = new File(getFileProp("project.dir"), "clover-core/target/classes")
    protected File cloverRuntimeClasses = new File(getFileProp("project.dir"), "clover-runtime/target/classes")
    protected File groverClasses = new File(getFileProp("project.dir"), "groovy/target/classes")
    protected File[] cloverLibs = new File(getFileProp("project.dir"), "clover-core-libs/target/dependencies/").listFiles()
    protected File servicesFolder = new File(getFileProp("project.dir"), "clover-ant/etc")
    protected File loggingProperties = new File(getFileProp("project.dir"), "groovy/src/test/resources/logging.properties")
    protected File junitJar = getFileProp("junit.jar")
    /** Location of hamcrest-core required by JUnit 4.11+ */
    protected File hamcrestJar = new File(getFileProp("project.dir"), "target/dependencies/hamcrest-core-1.3.jar")
    protected File groovyAllJar = getFileProp("groovy-all.jar", false) //Can be null as it may be set by the test suite
    protected File groverConfigDir

    public TestBase(String testName) {
        super(testName)
    }

    public TestBase(String methodName, String specificName, File groovyAllJar) {
        super(methodName, specificName);
        this.groovyAllJar = groovyAllJar
    }

    public void setUp() {
        createWorkingDir()
        reserveCloverDbFile()
        groverConfigDir = (File) File.createTempFile("grover", "config", workingDir).with {File dir ->
            dir.delete()
            dir.mkdir()
            dir
        }
    }

    public void tearDown() {
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

    public String calcRepkgJarPath() {
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
}
