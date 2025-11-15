package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.cfg.instr.InstrumentationConfig;
import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.instr.tests.naming.JUnitParameterizedTestExtractor;
import org.openclover.core.instr.tests.naming.SpockFeatureNameExtractor;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.registry.entities.FullElementInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.Modifiers;
import org.openclover.core.spi.lang.Language;
import org.openclover.core.util.ChecksummingReader;
import org.openclover.core.util.CloverUtils;
import org.openclover.groovy.instr.bytecode.RecorderGetterBytecodeInstructionGroovy2;
import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.recorder.FixedSizeCoverageRecorder;
import org.openclover.runtime.remote.DistributedConfig;
import org_openclover_runtime.CloverProfile;
import org_openclover_runtime.CoverageRecorder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static org.openclover.core.util.UnusedUtils.ignoreReturnedValue;

public abstract class CloverAstTransformerBase implements ASTTransformation {
    protected static final String recorderFieldName = CloverNames.namespace("R");
    protected static final String recorderGetterName = CloverNames.namespace("R");

    protected final InstrumentationConfig config;
    protected InstrumentationSession session;
    protected final Clover2Registry registry;

    /**
     * Helper class containing configuration data which is being written into instrumented groovy classes.
     * It contains excerpts from {@link InstrumentationConfig}, {@link InstrumentationSession} and
     * {@link Clover2Registry}
     */
    static class GroovyInstrumentationConfig {
        /**
         * Path to Clover database.
         */
        final String initString;

        /**
         * Distributed Coverage configuration encoded as string
         *
         * @see DistributedConfig#getConfigString()
         */
        final String distConfig;

        /**
         * Clover registry version
         *
         * @see InstrumentationSession#getVersion()
         */
        final long registryVersion;

        /**
         * Bit mask containing recorder settings like flush policy etc.
         *
         * @see CoverageRecorder#getConfigBits
         */
        final long recorderConfig;

        /**
         * Required capacity of the coverage recorder (for the {@link FixedSizeCoverageRecorder})
         *
         * @see FullFileInfo#getDataIndex()
         * @see FullFileInfo#getDataLength()
         */
        final int maxElements;

        /**
         * List of runtime profiles
         */
        final List<CloverProfile> profiles;

        GroovyInstrumentationConfig(String initString, String distConfig, long registryVersion, long recorderConfig, int maxElements, List<CloverProfile> profiles) {
            this.initString = initString;
            this.distConfig = distConfig;
            this.registryVersion = registryVersion;
            this.recorderConfig = recorderConfig;
            this.maxElements = maxElements;
            this.profiles = profiles;
        }
    }

    /**
     * Helper configuration object which keeps result of the groovy source instrumentation.
     * Whenever any specific statement or expression was instrumented, we need to add proper helper method
     * to the instrumented class (for instance a wrapper for elvis operator).
     */
    static class GroovyInstrumentationResult {
        final boolean elvisExprUsed;
        final boolean fieldExprUsed;
        final boolean safeExprUsed;
        final boolean testResultsRecorded;
        final boolean isTestClass;
        final boolean isSpockSpecification;
        final boolean isParameterizedJUnit;
        final Map<String, MethodNode> safeEvalMethods;

        /**
         * @param elvisExprUsed        true if evlis expression was present in code
         * @param fieldExprUsed        true if field expression was present in code
         * @param safeExprUsed         true if save evaluation expression was present in code
         * @param testResultsRecorded  true if test results are recorded by Clover (extra instrumentation code)
         * @param safeEvalMethods      list of safeEval_X() methods to be addded to the class
         * @param isTestClass          true if it's a test class according to test detector, false otherwise
         * @param isSpockSpecification true if it's a Spock framework test class (Specification)
         * @param isParameterizedJUnit true if it's a parameterized JUnit4 test class (@Parameterized annotation)
         */
        GroovyInstrumentationResult(boolean elvisExprUsed, boolean fieldExprUsed, boolean safeExprUsed,
                                    boolean testResultsRecorded,
                                    final Map<String, MethodNode> safeEvalMethods,
                                    boolean isTestClass, boolean isSpockSpecification, boolean isParameterizedJUnit) {
            this.elvisExprUsed = elvisExprUsed;
            this.fieldExprUsed = fieldExprUsed;
            this.safeExprUsed = safeExprUsed;
            this.testResultsRecorded = testResultsRecorded;
            this.safeEvalMethods = safeEvalMethods;
            this.isTestClass = isTestClass;
            this.isSpockSpecification = isSpockSpecification;
            this.isParameterizedJUnit = isParameterizedJUnit;
        }
    }

    static class GroovySourceContext implements TestDetector.SourceContext {
        private final File srcFile;

        GroovySourceContext(File srcFile) {
            this.srcFile = srcFile;
        }

        @Override
        public Language getLanguage() {
            return Language.Builtin.GROOVY;
        }

        @Override
        public boolean areAnnotationsSupported() {
            return true;
        }

        @Override
        public File getSourceFile() {
            return srcFile;
        }
    }

    static class GroovyClassTypeContext implements TestDetector.TypeContext {
        private final ClassNode clazz;

        GroovyClassTypeContext(final ClassNode clazz) {
            this.clazz = clazz;
        }

        @Override
        public String getPackageName() {
            return clazz.getPackageName();
        }

        @Override
        public String getTypeName() {
            return clazz.getNameWithoutPackage();
        }

        @Override
        public String getSuperTypeName() {
            return clazz.getSuperClass().getName();
        }

        @Override
        public Map<String, List<String>> getDocTags() {
            return Collections.emptyMap();
        }

        @Override
        public Modifiers getModifiers() {
            return GroovyModelMiner.extractModifiers(clazz);
        }
    }

    public static InstrumentationConfig newConfigFromResource() {
        URL configURL = null;
        InstrumentationConfig config = null;

        try {
            String configResourceName = "/" + CloverNames.getGroverConfigFileName();
            ClassLoader classLoader = CloverAstTransformerBase.class.getClassLoader();
            if (classLoader != null) {
                //If there are multiple ones due to sucessive <groovyc/> invocations and mutated Path elements, try the last one
                //Although whether this is relies on the ordering of the classloader (safe for URLClassloader)
                Enumeration<URL> resources = classLoader.getResources(configResourceName);
                if (resources != null && resources.hasMoreElements()) {
                    while (resources.hasMoreElements()) {
                        configURL = resources.nextElement();
                    }
                }
            }
            //Possibly no classloader (ie we were loaded by the primordial classloader) so just load the resource
            if (configURL == null) {
                configURL = CloverAstTransformerBase.class.getResource(configResourceName);
            }
            if (configURL != null) {
                Logger.getInstance().verbose("Loading config from " + configURL);
                config = InstrumentationConfig.loadFromStream(configURL.openStream());
            } else {
                Logger.getInstance().verbose("OpenClover Groovy integration was unable to locate its configuration from resource "
                        + configResourceName + ". Instrumentation and code coverage tracking will not occur for this build.");
            }
        } catch (Throwable t) {
            Logger.getInstance().error("OpenClover Groovy integration encountered an error while loading config: ", t);
        }

        return config;
    }

    public static Expression newRecorderExpression(ClassNode classRef, int row, int column) {
        return setSourcePosition(new StaticMethodCallExpression(classRef, recorderGetterName,
                setSourcePosition(new ArgumentListExpression(), row, column)
        ), row, column);
    }

    public static Statement recorderInc(final ClassNode clazz, final FullElementInfo m, final ASTNode originalNode) {
        int column = originalNode.getColumnNumber();
        int row = originalNode.getLineNumber();
        // imitate that it's a 0-length instruction inserted at the beginning of the one being instrumented
        // original (row1, col1, row2, col2) -> recInc (row1, col1, row1, col1); do it in all nodes
        final MethodCallExpression methodInc = setSourcePosition(new MethodCallExpression(
                newRecorderExpression(clazz, row, column),
                "inc",
                setSourcePosition(new ArgumentListExpression(
                        setSourcePosition(new ConstantExpression(m.getDataIndex()), row, column)
                ), row, column)
        ), row, column);
        methodInc.setImplicitThis(false); // we don't need 'this' in our method call context
        return setSourcePosition(new ExpressionStatement(methodInc), row, column);
    }

    public static <T extends ASTNode> T setSourcePosition(T node, int row, int column) {
        node.setLineNumber(row);
        node.setLastLineNumber(row);
        node.setColumnNumber(column);
        node.setLastColumnNumber(column);
        return node;
    }

    public static File getSourceUnitFile(SourceUnit sourceUnit) {
        // SourceUnit.getName() returns '/path/to/file' for groovy 1.6.x, 1.7.x
        // and 'file:/path/to/file' for groovy 1.8.x, 2.x
        final String FILE_PREFIX = "file:";
        final String sourceUnitName = sourceUnit.getName().startsWith(FILE_PREFIX) ?
                sourceUnit.getName().substring(FILE_PREFIX.length())
                : sourceUnit.getName();
        return new File(sourceUnitName);
    }

    public CloverAstTransformerBase(InstrumentationConfig cfg) throws IOException, CloverException {
        config = cfg;
        registry = config == null ? null : Clover2Registry.createOrLoad(config.getRegistryFile(), config.getProjectName());
    }

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        visit(sourceUnit);
    }

    public abstract void visit(SourceUnit sourceUnit);

    protected void addRecorderIncCalls(final SourceUnit sourceUnit, final ModuleNode module,
                                       final TestDetector.SourceContext testSourceContext,
                                       final Map<ClassNode, GroovyInstrumentationResult> flagsForInstrumentedClasses) {
        final List<ClassNode> classes = module.getClasses();
        if (classes != null) {
            for (final ClassNode clazz : classes) {
                if (!GroovyUtils.isReportable(clazz)) {
                    // warn about non-instrumented class
                    Logger.getInstance().verbose("Class " + clazz.getName() + " cannot not instrumented because "
                            + "AST contains invalid source region definition ("
                            + clazz.getLineNumber() + ":" + clazz.getColumnNumber()
                            + " - " + clazz.getLastLineNumber() + ":" + clazz.getLastColumnNumber() + ")");
                    break;
                }

                // detect if we have a test class and of which kind
                final TestDetector.TypeContext typeContext = new GroovyClassTypeContext(clazz);
                final boolean isTestClass = config.getTestDetector().isTypeMatch(testSourceContext, typeContext);
                final boolean isSpock = SpockFeatureNameExtractor.isClassWithSpecAnnotations(typeContext.getModifiers());
                final boolean isJUnit = JUnitParameterizedTestExtractor.isParameterizedClass(typeContext.getModifiers());

                // perform instrumentation
                final InstrumentingCodeVisitor instrumenter = new InstrumentingCodeVisitor(
                        config, session, registry, sourceUnit,
                        testSourceContext, isTestClass, clazz);
                if (instrumenter.instrument(clazz)) {
                    // ... and store some flags for further class enhancements
                    flagsForInstrumentedClasses.put(
                            clazz,
                            new GroovyInstrumentationResult(
                                    instrumenter.isElvisExprUsed(),
                                    instrumenter.isFieldExprUsed(),
                                    instrumenter.isSafeExprUsed(),
                                    instrumenter.isTestResultsRecorded(),
                                    instrumenter.getSafeEvalMethods(), isTestClass, isSpock, isJUnit)
                    );
                }
            }
        }
    }

    protected boolean isNotInstrumented(ModuleNode module) {
        for (ClassNode clazz : module.getClasses()) {
            if (clazz.getNameWithoutPackage().contains(CloverNames.CLOVER_RECORDER_PREFIX)) {
                return false;
            }
        }
        return true;
    }

    protected int getLastLineNumber(List<ClassNode> classes) {
        int lastLineNumber = 0;
        for (ClassNode clazz : classes) {
            int classLastLineNumber = clazz.getLastLineNumber();
            //Enums lose line / col numbers
            if (classLastLineNumber == -1) {
                for (MethodNode method : clazz.getMethods()) {
                    if (!method.isSynthetic()) {
                        classLastLineNumber = Math.max(classLastLineNumber, method.getLastLineNumber());
                    }
                }
            } else {
                lastLineNumber = Math.max(lastLineNumber, classLastLineNumber);
            }
        }
        return lastLineNumber;
    }

    ///CLOVER:OFF
    protected void maybeDumpAST(ModuleNode module, SourceUnit sourceUnit, String description, String extension) throws IOException {
        boolean doDumpAST = Boolean.getBoolean(CloverNames.GROVER_AST_DUMP);
        if (doDumpAST) {
            final File tmpDir = config.getTmpDir() == null ?
                    File.createTempFile("clover", ".dump").getParentFile()
                    : config.getTmpDir();
            File dumpDir = new File(tmpDir, "ast/" + CloverUtils.packageNameToPath(module.getPackageName(), module.getPackageName() == null));
            ignoreReturnedValue(dumpDir.mkdirs());
            dumpAST(module, sourceUnit, description, extension, dumpDir);
        }
    }

    protected void dumpAST(ModuleNode module, SourceUnit sourceUnit, String description, String extension, File dumpDir) {
        try {
            File file = new File(dumpDir, getSourceUnitFile(sourceUnit).getName() + extension);
            ignoreReturnedValue(file.createNewFile());

            Logger.getInstance().info(description + " for " + getSourceUnitFile(sourceUnit) + " written to " + file.getAbsolutePath());
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println(description);
                new NodePrinter().print(module, writer);
            }
        } catch (Throwable t) {
            Logger.getInstance().verbose("Failed to write " + description + " for " + getSourceUnitFile(sourceUnit), t);
        }

    }
    ///CLOVER:ON

    protected boolean isIncluded(SourceUnit sourceUnit) {
        return config.getIncludedFiles() != null && config.getIncludedFiles().contains(getSourceUnitFile(sourceUnit));
    }

    /**
     * @return BytecodeInstruction - an instance of RecorderGetterBytecodeInstructionGroovy2
     */
    protected BytecodeInstruction newRecorderGetterBytecodeInstruction(final ClassNode clazz, GroovyInstrumentationConfig sessionConfig) {
        // Try Groovy2 with ASM4
        return new RecorderGetterBytecodeInstructionGroovy2(
                clazz, recorderFieldName,
                sessionConfig.initString, sessionConfig.distConfig, sessionConfig.registryVersion,
                sessionConfig.recorderConfig, sessionConfig.maxElements, sessionConfig.profiles);
    }

    protected long calculateChecksum(File file) throws IOException {
        if (file.exists()) {
            Reader fileReader;
            if (config.getEncoding() != null) {
                fileReader = new InputStreamReader(Files.newInputStream(file.toPath()), config.getEncoding());
            } else {
                fileReader = new FileReader(file);
            }
            final ChecksummingReader chksumReader = new ChecksummingReader(fileReader);
            chksumReader.readAllCharacters();
            return chksumReader.getChecksum();
        } else {
            return -1L;
        }
    }

    protected String sanitisePackageName(String name) {
        if (name == null) {
            return "";
        } else {
            //package names are "com.foo.bar." - lop off the last .
            return name.substring(0, name.length() - 1);
        }
    }

}