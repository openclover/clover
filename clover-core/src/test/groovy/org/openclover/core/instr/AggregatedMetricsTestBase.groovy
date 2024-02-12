package org.openclover.core.instr

import org.junit.Before
import org.openclover.core.TestUtils
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.FileInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.StatementInfo
import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig
import org.openclover.core.cfg.instr.java.LambdaInstrumentation
import org.openclover.core.cfg.instr.java.SourceLevel
import org.openclover.core.instr.java.InstrumentationSource
import org.openclover.core.instr.java.Instrumenter
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.BaseFileInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.registry.entities.FullProjectInfo

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail
import static org.openclover.core.util.Lists.newLinkedList

/**
 * Test base for {@link org.openclover.core.instr.InstrumentationSessionImpl}
 */
abstract class AggregatedMetricsTestBase {
    protected File workingDir
    protected Clover2Registry registry
    protected PackageInfo packageInfo

    protected abstract String getTestName()

    protected abstract String getTestFileBaseName()

    /**
     * Set up temporary directory
     */
    @Before
    void setUp() throws Exception {
        workingDir = TestUtils.createEmptyDirFor(getClass(), getTestName())

        // prepare stuff
        final String testFileName = "${testFileBaseName}.txt"
        final File outputFile = new File(workingDir, "${testFileBaseName}.java".toString())
        final File registryDumpFile = new File(workingDir, "${testFileBaseName}.xml".toString())
        final JavaInstrumentationConfig config = createInstrConfig("${workingDir}${File.separator}${testFileBaseName}_clover.db".toString(), false)
        final Writer output = new OutputStreamWriter(new FileOutputStream(outputFile))

        // instrument sample file, get registry and the default package
        final InstrumentationSource input = new InstrumentationSource() {
            File getSourceFileLocation() {
                return new File("AggregatedMetrics.java")
            }

            Reader createReader() throws IOException {
                return new InputStreamReader(getClass().getResourceAsStream(testFileName))
            }
        }
        registry = performInstrumentation(config, input, output)
        packageInfo = registry.getProject().findPackage(PackageInfo.DEFAULT_PACKAGE_NAME)

        // debugging
        printProjectTree(registry.getProject(), new PrintStream(new FileOutputStream(registryDumpFile)))
    }

    protected void assertClassMetrics(String message, MetricValue expectedMetric, ClassInfo actualClass) {
        assertEquals(message + " statements",
                expectedMetric.statements, actualClass.getRawMetrics().getNumStatements())
        assertEquals(message + " aggregated statements",
                expectedMetric.aggregatedStatements, actualClass.getAggregatedStatementCount())
        assertEquals(message + " complexity",
                expectedMetric.complexity, actualClass.getRawMetrics().getComplexity())
        assertEquals(message + " aggregated complexity",
                expectedMetric.aggregatedComplexity, actualClass.getAggregatedComplexity())
    }

    protected void assertMethodMetrics(String message, MetricValue expectedMetric, FullMethodInfo actualMethod) {
        assertEquals(message + " statements",
                expectedMetric.statements, actualMethod.getRawMetrics().getNumStatements())
        assertEquals(message + " aggregated statements",
                expectedMetric.aggregatedStatements, actualMethod.getAggregatedStatementCount())
        assertEquals(message + " complexity",
                expectedMetric.complexity, actualMethod.getComplexity())
        assertEquals(message + " aggregated complexity",
                expectedMetric.aggregatedComplexity, actualMethod.getAggregatedComplexity())
    }

    /**
     * Helper class for searching in Clover registry
     */
    static class RegistryKey {
        String className
        String methodName
        RegistryKey(String className) {
            this.className = className
        }
        RegistryKey(String className, String methodName) {
            this.className = className
            this.methodName = methodName
        }
    }

    /**
     * Helper class for metrics being checked
     */
    static class MetricValue {
        int statements
        int aggregatedStatements
        int aggregatedComplexity
        int complexity

        MetricValue(int statements, int aggregatedStatements, int complexity, int aggregatedComplexity) {
            this.statements = statements
            this.aggregatedStatements = aggregatedStatements
            this.complexity = complexity
            this.aggregatedComplexity = aggregatedComplexity
        }
    }

    /**
     * Find first class in provided package (packageInfo) having name as defined in key.className.
     * @param packageInfo
     * @param key
     * @return ClassInfo or throws assertion failure
     */
    ClassInfo findClass(PackageInfo packageInfo, RegistryKey key) {
        for (ClassInfo classInfo : packageInfo.getClasses()) {
            if (classInfo.getName().equals(key.className)) {
                return classInfo
            }
        }
        fail("Class not found: "  + key.className)
        return null
    }

    /**
     * Find first method in provided package (packageInfo) having name as defined in key.className + key.methodName.
     * @param packageInfo
     * @param key
     * @return MethodInfo or throws assertion failure
     */
    FullMethodInfo findMethod(PackageInfo packageInfo, RegistryKey key) {
        for (ClassInfo classInfo : packageInfo.getClasses()) {
            if (classInfo.getName().equals(key.className)) {
                for (MethodInfo methodInfo : classInfo.getAllMethods()) {
                    if (methodInfo.getSimpleName().equals(key.methodName)) {
                        return (FullMethodInfo)methodInfo
                    }
                }
            }
        }
        fail("Method not found: "  + key.className + " " + key.methodName)
        return null
    }

    /**
     * Returns list of all methods from given package (packageInfo) having class and method name as specified
     * in key.
     * @param packageInfo
     * @param key
     * @return List&lt;MethodInfo&gt
     */
    List<FullMethodInfo> findAllMethods(PackageInfo packageInfo, RegistryKey key) {
        List<FullMethodInfo> ret = newLinkedList()
        for (ClassInfo classInfo : packageInfo.getClasses()) {
            if (classInfo.getName().equals(key.className)) {
                for (MethodInfo methodInfo : classInfo.getMethods()) {
                    if (methodInfo.getSimpleName().equals(key.methodName)) {
                        ret.add((FullMethodInfo)methodInfo)
                    }
                }
            }
        }
        return ret
    }

    /**
     * Create instrumentation configuration.
     * @param initString
     * @param relativeInitString
     * @return
     */
    protected JavaInstrumentationConfig createInstrConfig(String initString, boolean relativeInitString) {
        JavaInstrumentationConfig cfg = new JavaInstrumentationConfig()
        cfg.setDefaultBaseDir(workingDir)
        cfg.setInitstring(initString)
        cfg.setRelative(relativeInitString)
        cfg.setSourceLevel(SourceLevel.JAVA_8)
        cfg.setEncoding("ISO-88591")
        cfg.setInstrumentLambda(LambdaInstrumentation.ALL)
        return cfg
    }

    /**
     * Perform instrumentation of a source file using provided configuration.
     *
     * @param config instrumentation configuration
     * @param in   data source containing source file to be instrumented
     * @param out  output stream for instrumented version of the file
     * @return Clover2Registry
     * @throws Exception
     */
    protected Clover2Registry performInstrumentation(final JavaInstrumentationConfig config,
                                                     final InstrumentationSource input,
                                                     final Writer out) throws Exception {
        final Instrumenter instr = new Instrumenter(config)
        instr.startInstrumentation()
        instr.instrument(input, out, null)
        return instr.endInstrumentation()
    }

    /**
     * Write <pre>message</pre> to <pre>out</pre> with <pre>level</pre> tabs indentation.
     * @param level
     * @param message
     */
    protected static void printIndent(PrintStream out, int level, String message) {
        StringBuilder sb = new StringBuilder(message.length() + level)
        for (int i = 0; i < level; i++) {
            sb.append("\t")
        }
        sb.append(message)
        out.println(sb.toString())
    }

    /**
     * Print database structure in a tree layout (XML).
     * @param projectInfo
     * @param out
     */
    protected void printProjectTree(FullProjectInfo projectInfo, PrintStream out) {
        int level = 0
        printIndent(out, level, "<project name=\"" + projectInfo.getName() + "\" version=\"" + projectInfo.getVersion() + "\">")

        level++
        for (PackageInfo packageInfo : projectInfo.getAllPackages()) {
            printIndent(out, level, "<package name=\"" + packageInfo.getName() + "\">")

            level++
            for (FileInfo fileInfo : packageInfo.getFiles()) {
                BaseFileInfo baseFileInfo = (BaseFileInfo)fileInfo
                printIndent(out, level, "<file name=\"" + baseFileInfo.getName() + "\" timestamp=\"" + baseFileInfo.getTimestamp() + "\">")

                level++

                // statements
                for (StatementInfo stmt : baseFileInfo.getStatements()) {
                    printStatement(out, level, stmt)
                }

                // methods
                for (MethodInfo methodInfo : baseFileInfo.getMethods()) {
                    printMethod(out, level, methodInfo)
                }

                // classes
                for (ClassInfo classInfo : baseFileInfo.getClasses()) {
                    printClass(out, level, classInfo)
                }
                level--

                printIndent(out, level, "</file>")
            }
            level--

            printIndent(out, level, "</package>")
        }
        level--

        printIndent(out, level, "</project>")
    }

    private void printClass(PrintStream out, int level, ClassInfo classInfo) {
        printIndent(out, level, "<class name=\"" + classInfo.getName() + "\" qualifiedName=\"" + classInfo.getQualifiedName() + "\">")
        level++

        // class metrics
        printIndent(out, level, "<metrics"
                + " statements=\"" + classInfo.getMetrics().getNumStatements()
                + "\" aggregatedStatementCount=\"" + classInfo.getAggregatedStatementCount()
                + "\" aggregatedComplexity=\"" + classInfo.getAggregatedComplexity()
                + "\"/>")

        // class' methods
        for (MethodInfo methodInfo : classInfo.getMethods()) {
            printMethod(out, level, methodInfo)
        }

        // inner classes
        for (ClassInfo innerClassInfo : classInfo.getClasses()) {
            printClass(out, level, innerClassInfo)
        }

        // class body statements
        for (StatementInfo stmt : classInfo.getStatements()) {
            printStatement(out, level, stmt)
        }

        level--
        printIndent(out, level, "</class>")
    }

    private void printMethod(PrintStream out, int level, MethodInfo methodInfo) {
        printIndent(out, level, "<method name=\"" + methodInfo.getName() + "\">")
        level++

        // method metrics
        printIndent(out, level, "<metrics"
                + " statements=\"" + methodInfo.getStatements().size()
                + "\" aggregatedStatementCount=\"" + methodInfo.getAggregatedStatementCount()
                + "\" aggregatedComplexity=\"" + methodInfo.getAggregatedComplexity()
                + "\"/>")

        // inner classes
        for (ClassInfo innerClassInfo : methodInfo.getClasses()) {
            printClass(out, level, innerClassInfo)
        }

        // inner methods
        for (MethodInfo innerMethodInfo : methodInfo.getMethods()) {
            printMethod(out, level, innerMethodInfo)
        }

        // method body statements
        for (StatementInfo stmt : methodInfo.getStatements()) {
            printStatement(out, level, stmt)
        }

        level--
        printIndent(out, level, "</method>")
    }

    private void printStatement(PrintStream out, int level, StatementInfo stmt) {
        printIndent(out, level,
                String.format(
                        "<statement region=\"%d:%d-%d:%d\" dataIndex=\"%d\" dataLength=\"%d\" construct=\"%s\"/>",
                        stmt.getStartLine(), stmt.getStartColumn(), stmt.getEndLine(), stmt.getEndColumn(),
                        stmt.getDataIndex(), stmt.getDataLength(), stmt.getConstruct().getId()))
    }

}
