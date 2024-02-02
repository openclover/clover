package com.atlassian.clover.ant.groovy

import com.atlassian.clover.test.junit.AntVersions
import com.atlassian.clover.test.junit.DynamicallyNamedTestBase
import com.atlassian.clover.test.junit.GroovyVersions
import org.openclover.groovy.test.junit.JavaExecutorMixin
import org.openclover.groovy.test.junit.TestPropertyMixin
import org.openclover.groovy.test.junit.WorkingDirMixin
import com.atlassian.clover.CloverNames
import groovy.transform.CompileStatic

@CompileStatic
class CompilationTest extends DynamicallyNamedTestBase
        implements WorkingDirMixin, JavaExecutorMixin, TestPropertyMixin {

    AntProjectSimulacrum project

    /**
     * Constructor called when ran as a standalone test. Using predefined values (default Ant and Groovy).
     * @param testName
     */
    CompilationTest(String testName) {
        super(testName, testName)
        this.project = createDefaultAntProjectSimulacrum(testName)
    }

    /**
     * Constructor called from a {@link com.atlassian.clover.ant.groovy.TestSuite}. Testing against various Ant and
     * Groovy versions.
     */
    CompilationTest(String baseName, String specificName, AntProjectSimulacrum project) {
        super(baseName, specificName)
        this.project = project
    }

    private AntProjectSimulacrum createDefaultAntProjectSimulacrum(String testName) {
        File cloverRuntimeJar = getCloverRuntimeJar()
        File cloverRepkgRuntimeJar = getFileProp("repkg.clover.jar", false)

        String antVersion = AntVersions.DEFAULT_VERSION
        String groovyVersion = GroovyVersions.DEFAULT_VERSION

        File testDependenciesDir = new File("target/test-dependencies")

        new AntProjectSimulacrum(
                methodName: testName,
                testVersionedName: testName,
                testDependenciesDir: testDependenciesDir,
                antVersion: antVersion,
                groovyVersion: groovyVersion,
                cloverRuntimeJar: cloverRuntimeJar,
                cloverRepkgRuntimeJar: cloverRepkgRuntimeJar,
                test: this)
    }

    static File getCloverRuntimeJar() {
        // find clover-X.Y.Z-suffix.jar, but not -javadoc or -sources or clover-ant-
        new File("target/test-dependencies").listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                pathname.name.matches("clover-[0-9]+.*\\.jar") &&
                        !pathname.name.matches("-javadoc\\.jar") &&
                        !pathname.name.matches("-sources\\.jar")
            }
        })[0]
    }


//====ARTIFACT TEMPLATES====
    static String INIT_TARGET = '''
      <target name="init">
        <mkdir dir="${workDir}/src"/>
        <mkdir dir="${workDir}/dest"/>
      </target>
  '''
    static String WITH_CLOVER_TARGET = '''
      <target name="with.clover">
        <clover-setup initstring="${workDir}/.clover/coverage.db"/>
      </target>
  '''
    static String BASIC_COMPILE_TARGET = '''
      <target name="compile" depends="init">
        <groovyc srcDir="${workDir}/src/groovy" destDir="${workDir}/dest"/>
      </target>
  '''
    static String JOINT_COMPILE_TARGET = '''
      <target name="compile" depends="init">
        <groovyc srcDir="${workDir}/src" destDir="${workDir}/dest">
          <classpath refid="groovy.path"/> <!-- Necessary because the groovy jar location is not passed to javac so stubs will not compile -->
          <javac/>
        </groovyc>
      </target>
  '''
    //====TEST DISABLED INSTRUMENTATION====
    def testDisabledInstrumentationXML = """
          <target name="with.clover.on">
            <clover-setup enabled="true" initstring="\${workDir}/.clover/coverage.db"/>
          </target>
          <target name="with.clover.off">
            <clover-setup enabled="false" initstring="\${workDir}/.clover/coverage.db"/>
          </target>
          ${INIT_TARGET}
          <target name="compile1" depends="init, with.clover.on">
            <groovyc srcDir="\${workDir}/src1/groovy" destDir="\${workDir}/dest"/>
          </target>
          <target name="compile2" depends="init, with.clover.off">
            <groovyc srcDir="\${workDir}/src2/groovy" destDir="\${workDir}/dest"/>
          </target>
      """
    def testDisabledInstrumentationSrc = [
        "src1/groovy/com/foo/Bar1.groovy": defineGroovyClass("com.foo", "Bar1"),
        "src2/groovy/com/foo/Bar2.groovy": defineGroovyClass("com.foo", "Bar2")
    ]

    void testDisabledInstrumentation() {
        executeTargets("compile1", "compile2")
        assertCloveredClassAt("dest", "com.foo.Bar1")
        assertNonCloveredClassAt("dest", "com.foo.Bar2")
    }

    def testNoTopLevelClassYieldsInstrumentedClassesXML = """
          ${WITH_CLOVER_TARGET}
          ${INIT_TARGET}
          ${BASIC_COMPILE_TARGET}
      """
    def testNoTopLevelClassYieldsInstrumentedClassesSrc = [
        "src/groovy/com/foo/Bar.groovy":
        """
        package com.foo

        println "hello"
        """,
    ]

    void testNoTopLevelClassYieldsInstrumentedClasses() {
        executeTargets("with.clover", "compile")
        assertCloveredClassAt("dest", "com.foo.Bar")
    }

    //====TEST BASIC COMPILATION====
    def testBasicCompilationXML = """
          ${WITH_CLOVER_TARGET}
          ${INIT_TARGET}
          ${BASIC_COMPILE_TARGET}
      """
    def testBasicCompilationSrc = [
        "src/groovy/com/foo/Bar.groovy": defineGroovyClass("com.foo", "Bar")
    ]

    void testBasicCompilation() {
        executeTargets("with.clover", "compile")
        assertCloveredClassAt("dest", "com.foo.Bar")
    }

    //====TEST RECORDER NAME GENERATION ====
    def testRecorderNameGenerationXML = """
          ${WITH_CLOVER_TARGET}
          ${INIT_TARGET}
          ${BASIC_COMPILE_TARGET}
      """
    def testRecorderNameGenerationSrc = [
        "src/groovy/com/foo/Bar.groovy": defineGroovyClass("com.foo", "Bar"),
        "src/groovy/com/foo/1Bar.groovy": defineGroovyClass("com.foo", "Bar1"),
        "src/groovy/com/foo/Bar-bar-black-sheep.groovy": defineGroovyClass("com.foo", "Bar2")
    ]

    //====TEST JOINT COMPILATION====
    def testJointCompilationXML = """
          ${WITH_CLOVER_TARGET}
          ${INIT_TARGET}
          ${JOINT_COMPILE_TARGET}
      """
    def testJointCompilationSrc = [
        "src/groovy/com/foo/Bar.groovy": defineGroovyClass("com.foo", "Bar"),
        "src/java/com/foo/Baz.java": defineJavaClass("com.foo", "Baz")
    ]

    void testJointCompilation() {
        executeTargets("with.clover", "compile")
        assertCloveredClassAt("dest", "com.foo.Bar")
        assertCloveredClassAt("dest", "com.foo.Baz")
    }

    //====TEST JOINT COMPILATION WITH JAVA ON GROOVY DEPENDENCIES====
    def testJointCompilationWithJavaOnGroovyDependenciesXML = """
        ${WITH_CLOVER_TARGET}
        ${INIT_TARGET}
        ${JOINT_COMPILE_TARGET}
    """
    def testJointCompilationWithJavaOnGroovyDependenciesSrc = [
      "src/groovy/com/foo/Bar.groovy" : defineGroovyClass("com.foo", "Bar"),
      "src/java/com/foo/Baz.java" : defineJavaClass("com.foo", "Baz", "public void callBar() { Bar.main(null); }")
    ]
    void testJointCompilationWithJavaOnGroovyDependencies() {
      executeTargets("with.clover", "compile")
      assertCloveredClassAt("dest", "com.foo.Bar")
      assertCloveredClassAt("dest", "com.foo.Baz")
    }

    //====TEST JOINT COMPILATION WITH JAVA ON GROOVY DEPENDENCIES AND FILESET FILTERING====
    def testJointCompilationWithJavaOnGroovyDependenciesAndFilesetFilteringXML = """
          <target name="with.clover">
            <clover-setup initstring="\${workDir}/.clover/coverage.db">
              <fileset dir="\${workDir}/src1"/>
            </clover-setup>
          </target>
        ${INIT_TARGET}
          <target name="compile" depends="init">
            <groovyc destDir="\${workDir}/dest">
              <classpath refid="groovy.path"/>
              <src path="\${workDir}/src1"/>
              <src path="\${workDir}/src2"/>
              <javac/>
            </groovyc>
          </target>
    """
    def testJointCompilationWithJavaOnGroovyDependenciesAndFilesetFilteringSrc = [
      "src1/groovy/com/foo/Foo1.groovy" : defineGroovyClass("com.foo", "Foo1"),
      "src1/java/com/foo/Bar1.java" : defineJavaClass("com.foo", "Bar1", "public void callFoo2() { Foo2.main(null); }"),
      "src2/groovy/com/foo/Foo2.groovy" : defineGroovyClass("com.foo", "Foo2", "public void callBar1() { Bar1.main(null); }"),
      "src2/java/com/foo/Bar2.java" : defineJavaClass("com.foo", "Bar2")
    ]
    void testJointCompilationWithJavaOnGroovyDependenciesAndFilesetFiltering() {
      executeTargets("with.clover", "compile")
      assertCloveredClassAt("dest", "com.foo.Foo1")
      assertCloveredClassAt("dest", "com.foo.Bar1")
      assertNonCloveredClassAt("dest", "com.foo.Foo2")
      assertNonCloveredClassAt("dest", "com.foo.Bar2")
    }

    //====TEST JOINT COMPILATION WITH CLOVER PACKAGE FILTERING ====
    def testJointCompilationWithPackageFilteringXML = """
          <target name="with.clover">
            <clover-setup initstring="\${workDir}/.clover/coverage.db">
              <files>
                <exclude name="*/com/foo2/*"/>
              </files>
            </clover-setup>
          </target>
          ${INIT_TARGET}
          ${JOINT_COMPILE_TARGET}
        """
    def testJointCompilationWithPackageFilteringSrc = [
        "src/groovy/com/foo/Bar.groovy": defineGroovyClass("com.foo", "Bar"),
        "src/java/com/foo/Baz.java": defineJavaClass("com.foo", "Baz"),
        "src/java/com/foo2/Wibble.java": defineJavaClass("com.foo2", "Wibble"),
        "src/groovy/com/foo2/Wobble.groovy": defineGroovyClass("com.foo2", "Wobble")
    ]

    void testJointCompilationWithPackageFiltering() {
        executeTargets("with.clover", "compile")
        assertCloveredClassAt("dest", "com.foo.Bar")
        assertCloveredClassAt("dest", "com.foo.Baz")
        assertNonCloveredClassAt("dest", "com.foo2.Wobble")
        assertNonCloveredClassAt("dest", "com.foo2.Wibble")
    }

    //====TEST JOINT COMPILATION WITH CLOVER FILESET FILTERING ====
    def testJointCompilationWithCloverFilesetFilteringXML = """
          <target name="with.clover">
            <clover-setup initstring="\${workDir}/.clover/coverage.db">
              <fileset dir="\${workDir}/src">
                <exclude name="*/com/foo2/*"/>
              </fileset>
            </clover-setup>
          </target>
          ${INIT_TARGET}
          ${JOINT_COMPILE_TARGET}
        """
    def testJointCompilationWithCloverFilesetFilteringSrc = [
        "src/groovy/com/foo/Bar.groovy": defineGroovyClass("com.foo", "Bar"),
        "src/java/com/foo/Baz.java": defineJavaClass("com.foo", "Baz"),
        "src/java/com/foo2/Wibble.java": defineJavaClass("com.foo2", "Wibble"),
        "src/groovy/com/foo2/Wobble.groovy": defineGroovyClass("com.foo2", "Wobble")
    ]

    void testJointCompilationWithCloverFilesetFiltering() {
        executeTargets("with.clover", "compile")
        assertCloveredClassAt("dest", "com.foo.Bar")
        assertCloveredClassAt("dest", "com.foo.Baz")
        assertNonCloveredClassAt("dest", "com.foo2.Wobble")
        assertNonCloveredClassAt("dest", "com.foo2.Wibble")
    }

    //====TEST JOINT COMPILATION WITH CLOVER FILESET FILTERING ====
    def testJointCompilationWithIncludesXML = """
          ${WITH_CLOVER_TARGET}
          ${INIT_TARGET}
          <target name="compile" depends="init">
            <groovyc srcDir="\${workDir}/src" destDir="\${workDir}/dest">
              <include name="*/com/foo/**"/>
              <javac/>
            </groovyc>
          </target>
        """
    def testJointCompilationWithIncludesSrc = [
            "src/groovy/com/foo/Bar.groovy": defineGroovyClass("com.foo", "Bar"),
            "src/java/com/foo/Baz.java": defineJavaClass("com.foo", "Baz"),
            "src/java/com/foo2/Wibble.java": defineJavaClass("com.foo2", "Wibble"),
            "src/groovy/com/foo2/Wobble.groovy": defineGroovyClass("com.foo2", "Wobble")
    ]

    void testJointCompilationWithIncludes() {
        executeTargets("with.clover", "compile")
        assertCloveredClassAt("dest", "com.foo.Bar")
        assertCloveredClassAt("dest", "com.foo.Baz")
        assertNoClassAt("dest", "com.foo2.Wobble")
        assertNoClassAt("dest", "com.foo2.Wibble")
    }

    //====TEST JOINT COMPILATION WITH CLOVER FILESET FILTERING ====
    def testJointCompilationWithIncludesAndCloverFilesetXML = """
          <target name="with.clover">
            <clover-setup initstring="\${workDir}/.clover/coverage.db">
              <fileset dir="\${workDir}/src">
                <exclude name="*/com/foo/Bing.groovy"/>
              </fileset>
            </clover-setup>
          </target>
          ${INIT_TARGET}
          <target name="compile" depends="init">
            <groovyc srcDir="\${workDir}/src" destDir="\${workDir}/dest">
              <include name="*/com/foo/**"/>
              <include name="*/com/foo2/Wibble.java"/>
              <javac/>
            </groovyc>
          </target>
        """
    def testJointCompilationWithIncludesAndCloverFilesetSrc = [
        "src/groovy/com/foo/Bar.groovy": defineGroovyClass("com.foo", "Bar"),
        "src/java/com/foo/Baz.java": defineJavaClass("com.foo", "Baz"),
        "src/java/com/foo/Bing.groovy": defineGroovyClass("com.foo", "Bing"),
        "src/java/com/foo2/Wibble.java": defineJavaClass("com.foo2", "Wibble"),
        "src/groovy/com/foo2/Wobble.groovy": defineGroovyClass("com.foo2", "Wobble")
    ]

    void testJointCompilationWithIncludesAndCloverFileset() {
        executeTargets("with.clover", "compile")
        assertCloveredClassAt("dest", "com.foo.Bar")
        assertCloveredClassAt("dest", "com.foo.Baz")
        assertNonCloveredClassAt("dest", "com.foo.Bing")
        assertCloveredClassAt("dest/com/foo2", "Wibble")
        assertNoClassAt("dest", "com.foo2.Wobble")
    }

    //====TEST JOINT COMPILATION WITH MULTIPLE SRC ELEMENTS ====
    def testJointCompilationWithIncludesAndCloverFileseAndMultipleSrcElementsXML = """
          <target name="with.clover">
            <clover-setup initstring="\${workDir}/.clover/coverage.db"/>
          </target>
          ${INIT_TARGET}
          <target name="compile" depends="init">
            <groovyc destDir="\${workDir}/dest">
              <src path="\${workDir}/src1"/>
              <src path="\${workDir}/src2"/>
              <exclude name="*/com/foo2/**"/>
              <javac/>
            </groovyc>
          </target>
        """
    def testJointCompilationWithIncludesAndCloverFileseAndMultipleSrcElementsSrc = [
        "src1/groovy/com/foo/Foo1.groovy": defineGroovyClass("com.foo", "Foo1"),
        "src2/groovy/com/foo/Bar1.groovy": defineGroovyClass("com.foo", "Bar1"),
        "src1/groovy/com/foo/Foo3.java": defineJavaClass("com.foo", "Foo3"),
        "src2/groovy/com/foo/Bar3.java": defineJavaClass("com.foo", "Bar3"),

        "src2/groovy/com/foo2/Bar2.groovy": defineGroovyClass("com.foo2", "Bar2"),
        "src1/groovy/com/foo2/Foo2.groovy": defineGroovyClass("com.foo2", "Foo2"),
        "src2/groovy/com/foo2/Bar4.java": defineJavaClass("com.foo2", "Bar4"),
        "src1/groovy/com/foo2/Foo4.java": defineJavaClass("com.foo2", "Foo4"),
    ]

    void testJointCompilationWithIncludesAndCloverFileseAndMultipleSrcElements() {
        executeTargets("with.clover", "compile")
        assertCloveredClassAt("dest", "com.foo.Foo1")
        assertCloveredClassAt("dest", "com.foo.Foo3")
        assertCloveredClassAt("dest", "com.foo.Bar1")
        assertCloveredClassAt("dest", "com.foo.Bar3")

        assertNoClassAt("dest", "com.foo2.Bar2")
        assertNoClassAt("dest", "com.foo2.Foo2")
        assertNoClassAt("dest", "com.foo2.Bar4")
        assertNoClassAt("dest", "com.foo2.Foo4")
    }

    //====PLUMBING====

    protected void setUp() {
        super.setUp()
        createWorkingDir()
    }

    protected void tearDown() {
        super.tearDown()
        deleteWorkingDir()
    }

    protected void executeTargets(String ... targets) {
        project.executeTargets(workingDir, targets)
    }

    private void assertNoClassAt(String dir, String className) {
        File classFile = getClassFile(dir, className)
        String[] classFiles = classFile.parentFile.list()
        assertFalse("Compiled class found at ${dir}/${classFile.name}.class",
                classFiles != null && classFiles.any { it == "${classFile.name}.class" })
    }

    private File getClassFile(String dir, String className) {
        // change 'com.foo.Bar' to 'com/foo/Bar' or 'com\foo\Bar' (depending on platform)
        return new File(new File(workingDir, dir), className.replace(".", File.separator))
    }

    private static Closure recorderMatcher(className) {
        return { it =~ "${className}\\\$${CloverNames.CLOVER_RECORDER_PREFIX}.*" }
    }

    private void assertCloveredClassAt(String dir, String className) {
        File classFile = getClassFile(dir, className)
        String[] classFiles = classFile.parentFile.list()
        assertTrue("Compiled class not found at ${dir}/${classFile.name}.class",
                classFiles != null && classFiles.any { it == "${classFile.name}.class" })

        if (!classFiles.any(recorderMatcher(classFile.name))) {
            def result = launchJavap "-private -classpath ${new File(workingDir, dir)} ${className}"
            assertEquals("Unexpected javap return code", 0, result.exitCode)

            assertTrue(
                    "No instrumenter classes were found nor did javap output indicate that the class was instrumented.\nJavap output:\n${result.stdOut}\n\nClass files found:\n${classFiles.findAll {it =~ /\.class/ }.join("\n")}",
                    result.stdOut.contains("private static com_atlassian_clover.CoverageRecorder \$CLV_R\$"))
        }
    }

    private void assertNonCloveredClassAt(String dir, String className) {
        File classFile = getClassFile(dir, className)
        String[] classFiles = classFile.parentFile.list()
        assertTrue("Compiled class not found at ${dir}/${classFile.name}.class",
                classFiles != null && classFiles.any { it == "${classFile.name}.class" })

        assertFalse(
                "Matching Clover recorder found for ${className} in ${dir}. " + "Class files matched:\n${classFiles.findAll(recorderMatcher(classFile.name)).join("\n")}",
                classFiles.any(recorderMatcher(classFile.name)))

        def result = launchJavap "-private -classpath ${new File(workingDir, dir)} ${className}"
        assertEquals("Unexpected javap return code", 0, result.exitCode)

        assertFalse(
                "Javap output indicates that class was instrumented:\n\n${result.stdOut}",
                result.stdOut.contains("public static com_atlassian_clover.CoverageRecorder R"))
    }

    private static String defineGroovyClass(String pkg, String className, String optionalBody = "") {
        return """
          package ${pkg}

          public class ${className} {
            public static void main(String[] args) {
              println "Hello, Groovy world! (from ${pkg.length() == 0 ? '' : (pkg + '.')}${className}"
            }
            ${optionalBody}
          }
        """
    }

    private static String defineJavaClass(String pkg, String className, String optionalBody = "") {
        return """
          package ${pkg};

          public class ${className} {
            public static void main(String[] args) {
              System.out.println("Hello, Java world! (from ${pkg.length() == 0 ? '' : (pkg + '.')}${className}");
            }
            ${optionalBody}
          }
        """
    }
}