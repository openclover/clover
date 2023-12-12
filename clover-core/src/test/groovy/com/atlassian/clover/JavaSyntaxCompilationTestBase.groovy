package com.atlassian.clover

import clover.org.apache.commons.lang3.ArrayUtils
import com.atlassian.clover.api.registry.MethodInfo
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.FullProjectInfo
import com.atlassian.clover.registry.entities.FullStatementInfo
import com.atlassian.clover.registry.entities.LineInfo
import com.atlassian.clover.testutils.AssertionUtils
import com.atlassian.clover.testutils.IOHelper
import com.atlassian.clover.util.FileUtils
import com.atlassian.clover.util.SourceScanner
import org.apache.tools.ant.BuildEvent
import org.apache.tools.ant.BuildListener
import org.apache.tools.ant.Project
import org.apache.tools.ant.taskdefs.Execute
import org.apache.tools.ant.taskdefs.ExecuteWatchdog
import org.apache.tools.ant.taskdefs.Javac
import org.apache.tools.ant.taskdefs.PumpStreamHandler
import org.apache.tools.ant.types.Path
import org.apache.tools.ant.types.PatternSet
import org.apache.tools.ant.util.JavaEnvUtils
import org.jetbrains.annotations.Nullable
import org.junit.After
import org.junit.Rule
import org.junit.rules.TestName

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.openclover.util.Lists.newArrayList

/**
 * This is a base class for test cases testing compilation and instrumentation under different JDK versions
 */
abstract class JavaSyntaxCompilationTestBase {
    public static final String CLOVER_CORE_TESTCASES_SOURCE_DIR = "clover-core/src/test/resources"
    public static final String CLOVER_CORE_TEST_RUN_DIR = "clover-core/target/testrun"
    public static final String CLOVER_CORE_CLASSES_DIR = "clover-core/target/classes"
    public static final String CLOVER_RUNTIME_CLASSES_DIR = "clover-runtime/target/classes"

    /** Regular expression for: __CLR_hash_code.R.inc(index) */
    protected final String R_INC = "__CLR[a-zA-Z0-9_]+\\.R\\.inc\\([0-9]+\\);"

    /** Regular expression for: __CLR_hash_code.R.iget(index) */
    protected final String R_IGET = "__CLR[a-zA-Z0-9_]+\\.R\\.iget\\([0-9]+\\)"

    /** Regular expression for true part of branch coverage: (__CLR_hash_code.R.iget(index)!=0|true) */
    protected final String R_IGET_TRUE = "\\(" + R_IGET + "!=0\\|true\\)"

    /** Regular expression for false part of branch coverage: (__CLR_hash_code.R.iget(index)==0&false) */
    protected final String R_IGET_FALSE = "\\(" + R_IGET + "==0\\&false\\)"

    /** Regular expression for lambda:  */
    protected final String R_LAMBDA_INC_LEFT = "__CLR[a-zA-Z0-9_]+\\.lambdaInc\\([0-9]+,"
    protected final String R_LAMBDA_INC_RIGHT = ",[0-9]+\\)"


    protected File mTestcasesSrcDir
    private File mOutputDir
    protected File mGenSrcDir
    private File buildTmp
    private String mInitString
    private File mProjDir
    private Project mAntProj
    private FullProjectInfo mModel
    /** Keeps ant console output */
    private ByteArrayOutputStream mAntOutput
    private ByteArrayOutputStream execOutErrStream

    @Rule
    public TestName testName = new TestName()

    void setUpProject() throws Exception {
        mProjDir = IOHelper.getProjectDir()

        mTestcasesSrcDir = new File(mProjDir, CLOVER_CORE_TESTCASES_SOURCE_DIR)
        buildTmp = new File(mProjDir, CLOVER_CORE_TEST_RUN_DIR)
        buildTmp.mkdirs()
        File tmpDir = File.createTempFile("testcases", ".dir", buildTmp)
        tmpDir.delete()
        tmpDir.mkdirs()

        mOutputDir = new File(tmpDir, "classes")
        mOutputDir.mkdirs()

        mGenSrcDir = new File(tmpDir, "gensrc")
        mGenSrcDir.mkdirs()

        mInitString = tmpDir.getAbsolutePath() + "/coverage.db"

        mAntProj = new Project()
        mAntProj.init()
        mAntProj.setBaseDir(mProjDir)
        mAntOutput = new ByteArrayOutputStream(1024 * 30) // 30kB buffer
        mAntProj.addBuildListener(new PrintStreamBuildListener(new PrintStream(mAntOutput)))

        System.err.println()
        System.err.println("====")
        System.err.println("starting unit test " + testName.getMethodName())
        System.err.println("tmpDir=" + tmpDir)
        System.err.println("projDir=" + mProjDir)
    }

    @After
    void tearDown() throws Exception {
        FileUtils.deltree(new File(mInitString).getParentFile())
    }

    /**
     * Returns ant console output as string.
     */
    protected String getAntOutput() {
        return mAntOutput.toString()
    }

    /**
     * Clears ant console output.
     */
    protected void resetAntOutput() {
        mAntOutput.reset()
    }

    /**
     * Returns console output and error streams from the last {@link #executeMainClasses(String[])} call
     */
    protected String getExecOutput() {
        return execOutErrStream.toString()
    }

    /**
     * Instrument and compile file in separation, cleaning ant log. Good way to check compilation warnings etc.
     * @param srcDir
     * @param genSrcDir
     * @param fileName
     * @param srcVersion
     */
    protected void instrumentAndCompileSourceFile(File srcDir, File genSrcDir, String fileName, String srcVersion) {
        File srcFile = new File(srcDir, fileName)

        resetAntOutput()
        instrumentSourceFile(srcFile, srcVersion)
        compileSources(genSrcDir, [ fileName ] as String[], srcVersion)
    }

    /**
     * Instrument source files in given directory and compile them.
     * @param srcDir directory containing java sources
     * @param srcVersion "1.4", "1.5" .. use JavaEnvUtils.JAVA_x_y
     * @throws Exception
     */
    protected void instrumentAndCompileSources(File srcDir, String srcVersion) throws Exception {
        List<File> srcFiles = scanSrcDir(srcDir)
        for (File srcFile : srcFiles) {
            instrumentSourceFile(srcFile, srcVersion)
        }

        compileSources(mGenSrcDir, srcVersion)
    }

    protected void instrumentSourceFile(final File file, final String srcVersion) {
        instrumentSourceFile(file, srcVersion, new String[0])
    }

    /**
     * Instrument one source file only.
     * @param file
     * @param srcVersion
     */
    protected void instrumentSourceFile(final File file, final String srcVersion, final String[] extraArgs) {
        final int result = instrumentSourceFileNoAssert(file, srcVersion, extraArgs)
        assertEquals("instrumentation problem processing \"$file.absolutePath\":".toString(), 0, result)
    }

    protected int instrumentSourceFileNoAssert(final File file, final String srcVersion, final String[] extraArgs) {
        final String[] args = [
                "--source", srcVersion,
                "--verbose",
                "--encoding", "UTF-8",
                "--initstring", mInitString,
                "--destdir", mGenSrcDir.getAbsolutePath(),
                "--instrlambda", LambdaInstrumentation.ALL,
                file.getAbsolutePath()
        ]

        return CloverInstr.mainImpl((String[]) ArrayUtils.addAll(args, extraArgs))
    }

    /**
     * Build a classpath containing:
     *  - output directory with compiled testcases' classes
     *  - clover core classes
     *  - clover runtime classes
     *  - clover.jar (if specified in repkg.clover.jar property)
     * @return
     */
    protected String getClasspath() {
        String classpath = "${mOutputDir.absolutePath}${File.pathSeparator}" +
                "${mProjDir.absolutePath}${File.separator}${CLOVER_CORE_CLASSES_DIR}${File.pathSeparator}" +
                "${mProjDir.absolutePath}${File.separator}${CLOVER_RUNTIME_CLASSES_DIR}${File.pathSeparator}"


        // make sure repacked clover.jar is in there, if needed
        final String cloveringJar = System.getProperty("repkg.clover.jar")
        if (cloveringJar != null) {
            classpath += File.pathSeparator + cloveringJar
        }
        return classpath
    }

     protected List<File> scanSrcDir(final File dir) throws IOException {
        final List<File> files = newArrayList()
        SourceScanner scanner = new SourceScanner(dir, ".*\\.java")
        scanner.visit(new SourceScanner.Visitor() {

            void common(String path) throws IOException {
                files.add( new File(dir, FileUtils.getNormalizedPath(path)) )
            }

            void onlyInSrc(String path) throws IOException {
                files.add( new File(dir, FileUtils.getNormalizedPath(path)) )
            }

            void onlyInDest(String path) {
                // no-op
            }
        })

        return files
    }

    protected void executeMainClasses(String[] testCaseMainClasses) throws IOException {
        String javaExe = JavaEnvUtils.getJdkExecutable("java")

        for (String mainClass : testCaseMainClasses) {
            String[] cmdArray = [ javaExe, "-cp", getClasspath(), mainClass ]
            ExecuteWatchdog watchdog = new ExecuteWatchdog(30000L)

            execOutErrStream = new ByteArrayOutputStream(1024 * 30) // 30kB buffer
            Execute exe = new Execute(new PumpStreamHandler(execOutErrStream), watchdog)
            exe.setAntRun(mAntProj)
            exe.setWorkingDirectory(mAntProj.getBaseDir())
            exe.setCommandline(cmdArray)
            exe.execute()
            assertEquals("return value from $mainClass".toString(), 0, exe.getExitValue())
        }
    }

    protected void assertMethodCoverage(String classname, int lineno) throws Exception {
        assertMethodCoverage(classname, lineno, -1)
    }

    protected void assertMethodCoverage(String classname, int lineno, int expected) throws Exception {
        FullProjectInfo model = getModel()

        FullClassInfo c = (FullClassInfo)model.findClass(classname)
        assertNotNull("no such class $classname".toString(), classname)

        FullFileInfo fi = (FullFileInfo)c.getContainingFile()
        LineInfo li = fi.getLineInfo(false, false)[lineno]
        assertTrue("in $classname, no method at line $lineno".toString(), li.getMethodStarts().length > 0)
        final MethodInfo mi = li.getMethodStarts()[0]

        if (expected == -1) {
            assertTrue("no coverage", mi.getHitCount() > 0)
        } else {
            assertEquals("not the correct coverage", expected, mi.getHitCount())
        }
    }

    void assertNoStatement(String classname, int lineno) throws Exception {
        FullProjectInfo model = getModel()
        FullClassInfo c = (FullClassInfo)model.findClass(classname)
        assertNotNull("no such class $classname".toString(), classname)

        FullFileInfo fi = (FullFileInfo)c.getContainingFile()
        LineInfo lineInfo = fi.getLineInfo(false, false)[lineno]
        assertTrue("Expected no statement at line #$lineno but found something".toString(),
                lineInfo == null || lineInfo.getStatements().length == 0)
    }

    protected void assertStatementCoverage(String classname, int lineno, int[] expected) throws Exception {
        FullProjectInfo model = getModel()
        FullClassInfo c = (FullClassInfo)model.findClass(classname)
        assertNotNull("no such class $classname".toString(), classname)

        FullFileInfo fi = (FullFileInfo)c.getContainingFile()
        LineInfo lineInfo = fi.getLineInfo(false, false)[lineno]

        assertStatementCoverage(classname, lineno, lineInfo, expected)
    }

    protected void assertStatementCoverage(String classname, int lineno, int expected) throws Exception {
        FullProjectInfo model = getModel()
        FullClassInfo c = (FullClassInfo)model.findClass(classname)
        assertNotNull("no such class $classname".toString(), classname)

        FullFileInfo fi = (FullFileInfo)c.getContainingFile()
        LineInfo lineInfo = fi.getLineInfo(false, false)[lineno]
        assertStatementCoverage(classname, lineno, lineInfo, [ expected ] as int[])
    }

    protected void assertStatementCoverage(String classname, int lineNo, @Nullable LineInfo lineInfo, int[] expected) {
        assertNotNull(
                "in $classname , no statement at line $lineNo".toString(),
                lineInfo)
        assertTrue(
                "in $classname at line #$lineInfo.line number of statements ($lineInfo.statements.length) is not as expected ($expected.length)".toString(),
                lineInfo.getStatements().length == expected.length)

        for (int i = 0; i < expected.length; i++) {
            final FullStatementInfo si = lineInfo.getStatements()[i]
            if (expected[i] == -1) {
                assertTrue(
                        "in $classname at line #$lineInfo.line expected coverage but found no coverage".toString(),
                        si.getHitCount() > 0)
            } else {
                assertEquals(
                        "in $classname at line #$lineInfo.line expected ${expected[i]} hits but found $si.hitCount hits".toString(),
                        expected[i], si.getHitCount())
            }
        }

    }

    /**
     * Verifies whether ant console output (stored in mAntOutput) contains text
     * matching the regular expression.
     * @param regexpPattern
     * @param negate   if set to true then assert that ant output does NOT contain regexp
     */
    protected void assertAntOutputContains(String regexpPattern, boolean negate) {
        Pattern pattern = Pattern.compile(regexpPattern)
        Matcher matcher = pattern.matcher(getAntOutput())
        if (!negate) {
            assertTrue("A pattern '$regexpPattern' was not found in ant output".toString(), matcher.find())
        } else {
            assertFalse("A pattern '$regexpPattern' was found in ant output".toString(), matcher.find())
        }
    }

    /**
     * Verifies whether exec console output/error streams (stored in execOutErrStream) contains text
     * matching the regular expression.
     * @param regexpPattern
     * @param negate   if set to true then assert that exec output does NOT contain regexp
     */
    protected void assertExecOutputContains(String regexpPattern, boolean negate) {
        Pattern pattern = Pattern.compile(regexpPattern)
        Matcher matcher = pattern.matcher(getExecOutput())
        if (!negate) {
            assertTrue("A pattern '$regexpPattern' was not found in ant output".toString(), matcher.find())
        } else {
            assertFalse("A pattern '$regexpPattern' was found in ant output".toString(), matcher.find())
        }
    }

    /**
     * Verifies whether given instrumented file contains text matching the regular expression.
     * @param instrumentedFileName  relative path to instrumented file (mGenSrcDir is a root)
     * @param regExp         regular expression to be searched inside a file
     * @param negate         negate assertion - if set to true then assert that file does NOT contain regexp
     */
    protected void assertFileMatches(String instrumentedFileName, String regExp, boolean negate) {
        final File instrumentedFile = new File(mGenSrcDir, instrumentedFileName)
        AssertionUtils.assertFileMatches(regExp, instrumentedFile, negate)
    }

    /**
     * Verifies whether given instrumented file contains the substring.
     * @param instrumentedFileName  relative path to instrumented file (mGenSrcDir is a root)
     * @param subString         substring to be searched inside a file
     * @param negate         negate assertion - if set to true then assert that file does NOT contain regexp
     */
    protected void assertFileContains(String instrumentedFileName, String subString, boolean negate) {
        final File instrumentedFile = new File(mGenSrcDir, instrumentedFileName)
        AssertionUtils.assertFileContains(subString, instrumentedFile, negate)
    }

    protected FullProjectInfo getModel() throws Exception {
        if (mModel == null) {
            final CloverDatabase db = new CloverDatabase(mInitString)
            db.loadCoverageData()
            mModel = db.getModel(CodeType.APPLICATION)
        }
        return mModel

    }

    /**
     * Compile all sources from srcDir using the given java language version.
     * @param srcDir     source directory
     * @param srcVersion java version (e.g. "1.5") passed to "--source" switch
     */
    protected void compileSources(final File srcDir, final String srcVersion) {
        compileSources(srcDir, null, srcVersion)
    }

    /**
     * Compile source files from srcDir but limited to given include pattern,
     * using the given java language version.
     * @param srcDir      source directory
     * @param srcFiles    list of files to be added to "includes" or null if all files should be compiled
     * @param srcVersion  java version (e.g. "1.5") passed to "--source" switch
     */
    protected void compileSources(final File srcDir, final String[] srcFiles, final String srcVersion) {
        final Javac javac = (Javac) mAntProj.createTask("javac")
        javac.setSource(srcVersion)

        final Path srcPath = javac.createSrc()
        srcPath.setPath(srcDir.getAbsolutePath())

        // limit compilation to specific files only
        if (srcFiles != null) {
            for (String srcFile : srcFiles) {
                // add single file to include list
                PatternSet.NameEntry includeFile = javac.createInclude()
                includeFile.setName(srcFile)
            }
        }

        // add clover stuff to class path
        final Path classpath = javac.createClasspath()
        classpath.setPath(getClasspath())

        javac.setDestdir(mOutputDir)
        javac.setFork(true)
        javac.setEncoding("UTF8")
        javac.setIncludes("**/*.java")
        javac.setDebug(true)
        javac.createCompilerArg().setValue("-Xlint") // enable all extra warnings
        javac.createCompilerArg().setValue("-J-Duser.language=en") // some tests check compiler messages
        javac.createCompilerArg().setValue("-J-Duser.locale=EN")

        // compile code
        javac.perform()
    }

    /**
     * A build listener which logs ant output to System.err (if default constructor is used)
     * or to specified output print stream. By default it logs all messages with MSG_VERBOSE
     * level or higher.
     */
    static class PrintStreamBuildListener implements BuildListener {
        private final PrintStream mOut
        private int mThreshold = Project.MSG_VERBOSE

        PrintStreamBuildListener() {
            mOut = System.err
        }

        PrintStreamBuildListener(PrintStream out) {
            mOut = out
        }

        void setThreshold(int threshold) {
            mThreshold = threshold
        }

        void buildStarted(BuildEvent event) {
            log(event)
        }

        void buildFinished(BuildEvent event) {
            log(event)
        }

        void targetStarted(BuildEvent event) {
            log(event)
        }

        void targetFinished(BuildEvent event) {
            log(event)
        }

        void taskStarted(BuildEvent event) {
            log(event)
        }

        void taskFinished(BuildEvent event) {
            log(event)
        }

        void messageLogged(BuildEvent event) {
            log(event)
        }

        private void log(BuildEvent event) {
            if (event.getPriority() <= mThreshold) {
                mOut.println(event.getMessage())
                Throwable e = event.getException()
                if (e != null) {
                    e.printStackTrace(mOut)
                }
            }
        }
    }

}

