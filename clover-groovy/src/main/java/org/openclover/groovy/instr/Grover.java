package org.openclover.groovy.instr;

import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org.openclover.core.api.instrumentation.InstrumentationSession;
import org.openclover.core.cfg.instr.InstrumentationConfig;
import org.openclover.groovy.instr.bytecode.RecorderGetterBytecodeInstructionGroovy2;
import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.instr.tests.naming.JUnitParameterizedTestExtractor;
import org.openclover.core.instr.tests.naming.SpockFeatureNameExtractor;
import org.openclover.runtime.recorder.PerTestRecorder;
import org.openclover.runtime.recorder.pertest.SnifferType;
import org.openclover.core.registry.Clover2Registry;
import org.openclover.core.registry.entities.FullElementInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.Modifiers;
import org.openclover.core.spi.lang.Language;
import org.openclover.core.util.ChecksummingReader;
import org.openclover.core.util.CloverUtils;
import org_openclover_runtime.CloverProfile;
import org_openclover_runtime.CoverageRecorder;
import org_openclover_runtime.TestNameSniffer;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

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

import static groovyjarjarasm.asm.Opcodes.ACC_FINAL;
import static groovyjarjarasm.asm.Opcodes.ACC_PRIVATE;
import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;
import static groovyjarjarasm.asm.Opcodes.ACC_SYNTHETIC;
import static org.openclover.core.util.Maps.newHashMap;

/**
 * Shizzler of the nizzle. This is where it all starts, baby.
 * We attach to the instruction selection phase because
 * most ast transformers occur in the canonicalization phase
 */
@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class Grover implements ASTTransformation {
    private static final String recorderFieldName = CloverNames.namespace("R");
    private static final String recorderGetterName = CloverNames.namespace("R");

    private InstrumentationConfig config;
    private InstrumentationSession session;
    private Clover2Registry registry;

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
         * @see com.atlassian.clover.remote.DistributedConfig#getConfigString()
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
         * Required capacity of the coverage recorder (for the {@link com.atlassian.clover.recorder.FixedSizeCoverageRecorder})
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


    public Grover() throws IOException, CloverException {
        this(newConfigFromResource());
    }

    Grover(InstrumentationConfig cfg) throws IOException, CloverException {
        config = cfg;
        registry = config == null ? null : Clover2Registry.createOrLoad(config.getRegistryFile(), config.getProjectName());
    }

    public static InstrumentationConfig newConfigFromResource() {
        URL configURL = null;
        InstrumentationConfig config = null;

        try {
            String configResourceName = "/" + CloverNames.getGroverConfigFileName();
            ClassLoader classLoader = Grover.class.getClassLoader();
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
                configURL = Grover.class.getResource(configResourceName);
            }
            if (configURL != null) {
                Logger.getInstance().verbose("Loading config from " + configURL);
                config = InstrumentationConfig.loadFromStream(configURL.openStream());
            } else {
                Logger.getInstance().verbose("Clover-for-Groovy was unable to locate its configuration from resource " + configResourceName + ". Instrumentation and code coverage tracking will not occur for this build.");
            }
        } catch (Throwable t) {
            Logger.getInstance().error("Clover-for-Groovy encountered an error while loading config: ", t);
        }

        return config;
    }

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        visit(sourceUnit);
    }

    public void visit(SourceUnit sourceUnit) {
        try {
            final ModuleNode module = sourceUnit.getAST();
            if (config != null && config.isEnabled() && !alreadyInstrumented(module)) {
                if (!isIncluded(sourceUnit)) {
                    Logger.getInstance().verbose("Skipping " + getSourceUnitFile(sourceUnit));
                } else {

                    maybeDumpAST(module, sourceUnit, "Original source", ".before.clovered");

                    final String pkg = sanitisePackageName(module.getPackageName());
                    final File srcFile = getSourceUnitFile(sourceUnit);
                    final TestDetector.SourceContext testSourceContext = new GroovySourceContext(srcFile);
                    final int lastLineNumber = module.getClasses() == null ? 0 : getLastLineNumber(module.getClasses());
                    final Map<ClassNode, GroovyInstrumentationResult> flagsForInstrumentedClasses = newHashMap();

                    // first pass - add recorder.inc() stuff
                    Logger.getInstance().verbose("Processing \"" + getSourceUnitFile(sourceUnit) + "\", package - \"" + pkg + "\"");
                    session = registry.startInstr(config.getEncoding());
                    final FullFileInfo fileInfo = (FullFileInfo) session.enterFile(
                            pkg, srcFile, lastLineNumber, 0,
                            srcFile.lastModified(), srcFile.length(), calculateChecksum(srcFile)); // HACK - nclinecount

                    addRecorderIncCalls(sourceUnit, module, testSourceContext, flagsForInstrumentedClasses);
                    session.exitFile();

                    // second pass - add helper stuff like recorderInc method, elvis wrapper etc
                    addHelperFieldsAndMethods(fileInfo, flagsForInstrumentedClasses);

                    maybeDumpAST(module, sourceUnit, "Instrumented source", ".after.clovered");

                    session.close();
                    registry.saveAndAppendToFile();
                }
            }
        } catch (Exception e) {
            final RuntimeException re = new RuntimeException("Clover-for-Groovy failed to instrument Groovy source: " + getSourceUnitFile(sourceUnit), e);
            Logger.getInstance().error(re.getMessage(), re);
            throw re;
        }
    }


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

    /**
     * Based on flags from first instrumentation pass enhance instrumented classes by adding extra fields and methods,
     * such as:
     * <ul>
     *  <li>$CLV_R$ field and $CLV_R$() getter for CoverageRecorder</li>
     *  <li>elvis operator</li>
     *  <li>boolean expression</li>
     *  <li>safe evalation</li>
     *  <li>test result recording</li>
     * </ul>
     */
    protected void addHelperFieldsAndMethods(final FullFileInfo fileInfo,
                                             final Map<ClassNode, GroovyInstrumentationResult> flagsForInstrumentedClasses) throws Exception {
        // check which classes have been instrumented and generate extra methods according to needs
        GroovyInstrumentationConfig sessionConfig = new GroovyInstrumentationConfig(
                config.getInitString(),
                config.getDistributedConfigString(),
                session.getVersion(),
                CoverageRecorder.getConfigBits(
                        config.getFlushPolicy(),
                        config.getFlushInterval(),
                        false, false, !config.isSliceRecording()),
                fileInfo.getDataIndex() + fileInfo.getDataLength(),
                config.getProfiles());

        for (Map.Entry<ClassNode, GroovyInstrumentationResult> entry : flagsForInstrumentedClasses.entrySet()) {
            ClassNode clazz = entry.getKey();
            GroovyInstrumentationResult flags = entry.getValue();

            createRecorderFieldAndGetter(clazz, sessionConfig, flags);
            createEvalElvisMethod(clazz, flags);
            createExprEvalMethod(clazz, flags);
            createSafeEvalMethods(clazz, flags);
            createEvalTestExceptionMethod(clazz, flags);
            createTestNameSnifferField(clazz, flags);
        }
    }

    private boolean alreadyInstrumented(ModuleNode module) {
        for (ClassNode clazz : module.getClasses()) {
            if (clazz.getNameWithoutPackage().contains(CloverNames.CLOVER_RECORDER_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private int getLastLineNumber(List<ClassNode> classes) {
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
    private void maybeDumpAST(ModuleNode module, SourceUnit sourceUnit, String description, String extension) throws IOException {
        boolean doDumpAST = Boolean.getBoolean(CloverNames.GROVER_AST_DUMP);
        if (doDumpAST) {
            final File tmpDir = config.getTmpDir() == null ?
                    File.createTempFile("clover", ".dump").getParentFile()
                    : config.getTmpDir();
            File dumpDir = new File(tmpDir, "ast/" + CloverUtils.packageNameToPath(module.getPackageName(), module.getPackageName() == null));
            dumpDir.mkdirs();
            dumpAST(module, sourceUnit, description, extension, dumpDir);
        }
    }

    private void dumpAST(ModuleNode module, SourceUnit sourceUnit, String description, String extension, File dumpDir) {
        try {
            File file = new File(dumpDir, getSourceUnitFile(sourceUnit).getName() + extension);
            file.createNewFile();

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

    public static File getSourceUnitFile(SourceUnit sourceUnit) {
        // SourceUnit.getName() returns '/path/to/file' for groovy 1.6.x, 1.7.x
        // and 'file:/path/to/file' for groovy 1.8.x, 2.x
        final String FILE_PREFIX = "file:";
        final String sourceUnitName = sourceUnit.getName().startsWith(FILE_PREFIX) ?
                sourceUnit.getName().substring(FILE_PREFIX.length())
                : sourceUnit.getName();
        return new File(sourceUnitName);
    }

    private boolean isIncluded(SourceUnit sourceUnit) {
        return config.getIncludedFiles() != null && config.getIncludedFiles().contains(getSourceUnitFile(sourceUnit));
    }

    /**
     * @return BytecodeInstruction - an instance of RecorderGetterBytecodeInstructionGroovy2
     */
    private BytecodeInstruction newRecorderGetterBytecodeInstruction(final ClassNode clazz, GroovyInstrumentationConfig sessionConfig) {
        // Try Groovy2 with ASM4
        return new RecorderGetterBytecodeInstructionGroovy2(
                clazz, recorderFieldName,
                sessionConfig.initString, sessionConfig.distConfig, sessionConfig.registryVersion,
                sessionConfig.recorderConfig, sessionConfig.maxElements, sessionConfig.profiles);
    }

    /**
     * Creates a static field for CoverageRecorder and a static method (lazy initialization) like:
     * <pre>
     *     private static CoverageRecorder $CLV_R$ = null;
     *     private static CoverageRecorder $CLV_R$() {
     *          ...
     *     }
     * </pre>
     */
    private ClassNode createRecorderFieldAndGetter(final ClassNode clazz, GroovyInstrumentationConfig sessionConfig, GroovyInstrumentationResult flags) {
        // add field
        FieldNode recorderField =
                clazz.addField(recorderFieldName, ACC_STATIC | ACC_PRIVATE | ACC_SYNTHETIC,
                        ClassHelper.make(org_openclover_runtime.CoverageRecorder.class),
                        ConstantExpression.NULL);

        // add method (no code yet)
        MethodNode recorderGetter =
                clazz.addMethod(recorderGetterName, ACC_STATIC | ACC_PRIVATE | ACC_SYNTHETIC,
                        ClassHelper.make(org_openclover_runtime.CoverageRecorder.class),
                        new Parameter[]{}, new ClassNode[]{},
                        new BlockStatement());

        // fill the getter method with byte code
        BytecodeInstruction bytecodeInstruction = newRecorderGetterBytecodeInstruction(clazz, sessionConfig);
        ((BlockStatement) recorderGetter.getCode()).addStatement(new BytecodeSequence(bytecodeInstruction));

        return clazz;
    }

    private ClassNode createEvalElvisMethod(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.elvisExprUsed) {
            addEvalElvis(clazz);
        }
        return clazz;
    }

    private ClassNode createExprEvalMethod(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.fieldExprUsed) {
            addExprEval(clazz);
        }
        return clazz;
    }

    private ClassNode createSafeEvalMethods(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.safeExprUsed) {
            for (final MethodNode methodNode : flags.safeEvalMethods.values()) {
                clazz.addMethod(methodNode);
            }
        }
        return clazz;
    }

    private ClassNode createEvalTestExceptionMethod(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.testResultsRecorded) {
            addEvalTestException(clazz);
        }
        return clazz;
    }

    /**
     * Add the field named {@link CloverNames#CLOVER_TEST_NAME_SNIFFER} to the class.
     *
     * @param clazz class to be extended
     * @param flags flags after first instrumentation pass
     * @return ClassNode modified input class
     */
    private ClassNode createTestNameSnifferField(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.isTestClass) {
            return createTestNameSnifferField(clazz,
                    flags.isSpockSpecification ? SnifferType.SPOCK
                            : (flags.isParameterizedJUnit ? SnifferType.JUNIT : SnifferType.NULL));
        } else {
            return clazz;
        }

    }

    /**
     * Add the field named {@link CloverNames#CLOVER_TEST_NAME_SNIFFER} to the class.
     *
     * @param clazz class to be extended
     * @param snifferType type of the sniffer to be embedded
     * @return ClassNode modified input class
     */
    private ClassNode createTestNameSnifferField(final ClassNode clazz, final SnifferType snifferType) {
        final Expression fieldInitializationExpr;
        switch (snifferType) {
            case JUNIT:
            case SPOCK:
                // new TestNameSniffer.Simple()
                fieldInitializationExpr = new ConstructorCallExpression(
                        createSimpleSnifferClassNode(), ArgumentListExpression.EMPTY_ARGUMENTS);
                break;
            case NULL:
            default:
                // TestNameSniffer.NULL_INSTANCE
                fieldInitializationExpr = new PropertyExpression(
                        new ClassExpression(ClassHelper.make(TestNameSniffer.class)), "NULL_INSTANCE");
                break;
        }

        // add field
        // public static final __CLRx_y_z_TEST_NAME_SNIFFER = ...
        clazz.addField( CloverNames.CLOVER_TEST_NAME_SNIFFER,
                ACC_STATIC | ACC_PUBLIC | ACC_SYNTHETIC | ACC_FINAL,
                        ClassHelper.make(org_openclover_runtime.TestNameSniffer.class),
                        fieldInitializationExpr);
        return clazz;
    }

    /**
     * @return ClassNode representing TestNameSniffer.Simple class
     */
    private ClassNode createSimpleSnifferClassNode() {
        return ClassHelper.make(TestNameSniffer.Simple.class);
    }

    private void addExprEval(ClassNode clazz) {
        //def exprEval(def expr, Integer index) {
        //  RECORDERCLASS.R.inc(index)
        //  return expr
        //}
        final Parameter expr = new Parameter(ClassHelper.DYNAMIC_TYPE, "expr");
        final Parameter index = new Parameter(ClassHelper.Integer_TYPE, "index");
        final VariableScope methodScope = new VariableScope();
        final Statement methodCode = new BlockStatement(
                new Statement[]{
                        new ExpressionStatement(
                                new MethodCallExpression(
                                        newRecorderExpression(clazz, -1, -1),
                                        "inc",
                                        new ArgumentListExpression(new VariableExpression(index)))),
                        new ReturnStatement(new VariableExpression(expr))
                },
                methodScope);

        clazz.addMethod(
                CloverNames.namespace("exprEval"), ACC_STATIC | ACC_PUBLIC,
                ClassHelper.DYNAMIC_TYPE,
                new Parameter[]{expr, index},
                new ClassNode[]{},
                methodCode);
    }

    private void addEvalElvis(ClassNode clazz) {
        //def elvisEval(def expr, Integer index) {
        //  boolean isTrue = expr as Boolean
        //  if (isTrue) { RECORDERCLASS.R.inc(index) } else { RECORDERCLASS.R.inc(index + 1) }
        //  return expr
        //}
        final Parameter expr = new Parameter(ClassHelper.DYNAMIC_TYPE, "expr");
        final Parameter index = new Parameter(ClassHelper.Integer_TYPE, "index");
        final VariableScope methodScope = new VariableScope();
        final Statement methodCode = new BlockStatement(
                new Statement[]{
                        new ExpressionStatement(
                                new DeclarationExpression(
                                        new VariableExpression("isTrue", ClassHelper.Boolean_TYPE),
                                        Token.newSymbol(Types.EQUAL, -1, -1),
                                        CastExpression.asExpression(ClassHelper.Boolean_TYPE, new VariableExpression(expr)))),
                        new IfStatement(
                                new BooleanExpression(new VariableExpression("isTrue", ClassHelper.Boolean_TYPE)),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        newRecorderExpression(clazz, -1, -1),
                                                        "inc",
                                                        new ArgumentListExpression(new VariableExpression(index)))),
                                        new ReturnStatement(new VariableExpression(expr))
                                }, methodScope),
                                new BlockStatement(new Statement[]{
                                        new ExpressionStatement(
                                                new MethodCallExpression(
                                                        newRecorderExpression(clazz, -1, -1),
                                                        "inc",
                                                        new ArgumentListExpression(
                                                                new BinaryExpression(
                                                                        new VariableExpression(index),
                                                                        Token.newSymbol(Types.PLUS, -1, -1),
                                                                        new ConstantExpression(1))))),
                                        new ReturnStatement(new VariableExpression(expr))
                                }, methodScope))

                },
                methodScope
        );

        clazz.addMethod(
                CloverNames.namespace("elvisEval"), ACC_STATIC | ACC_PUBLIC,
                ClassHelper.DYNAMIC_TYPE,
                new Parameter[]{expr, index},
                new ClassNode[]{},
                methodCode);
    }

    private void addEvalTestException(ClassNode clazz) {
        //def evalTestException(Throwable exception, def expected) {
        //  boolean isExpected = false
        //  for (ex in expected) {
        //      isExpected = isExpected || (exception != null && ex.isAssignableFrom(exception.getClass()))
        //  }
        //  return (isExpected || (exception == null && expected.isEmpty())) ? $PerTestRecorder.NORMAL_EXIT$ : $PerTestRecorder.ABNORMAL_EXIT$
        //}
        Parameter exceptionParam = new Parameter(ClassHelper.make(Throwable.class), "exception");
        Parameter expectedParam = new Parameter(ClassHelper.DYNAMIC_TYPE, "expected");
        VariableScope methodScope = new VariableScope();

        VariableExpression isExpectedVar = new VariableExpression("isExpected", ClassHelper.Boolean_TYPE);
        Parameter exVariable = new Parameter(ClassHelper.make(Class.class), "ex");

        clazz.addMethod(
                CloverNames.namespace("evalTestException"), ACC_STATIC | ACC_PUBLIC,
                ClassHelper.int_TYPE,
                new Parameter[]{exceptionParam, expectedParam},
                new ClassNode[]{},
                new BlockStatement(
                        new Statement[]{
                                new ExpressionStatement(
                                        new DeclarationExpression(
                                                isExpectedVar,
                                                Token.newSymbol(Types.EQUAL, -1, -1),
                                                ConstantExpression.FALSE
                                        )),
                                new ForStatement(
                                        exVariable,
                                        new VariableExpression(expectedParam),
                                        new BlockStatement(
                                                new Statement[]{
                                                        new ExpressionStatement(
                                                                new BinaryExpression(
                                                                        isExpectedVar,
                                                                        Token.newSymbol(Types.EQUAL, -1, -1),
                                                                        new BinaryExpression(
                                                                                isExpectedVar,
                                                                                Token.newSymbol(Types.LOGICAL_OR, -1, -1),
                                                                                new BinaryExpression(
                                                                                        new BinaryExpression(
                                                                                                new VariableExpression(exceptionParam),
                                                                                                Token.newSymbol(Types.COMPARE_NOT_EQUAL, -1, -1),
                                                                                                ConstantExpression.NULL),
                                                                                        Token.newSymbol(Types.LOGICAL_AND, -1, -1),
                                                                                        new MethodCallExpression(
                                                                                                new VariableExpression(exVariable),
                                                                                                "isAssignableFrom",
                                                                                                new MethodCallExpression(
                                                                                                        new VariableExpression(exceptionParam),
                                                                                                        "getClass",
                                                                                                        ArgumentListExpression.EMPTY_ARGUMENTS))))))
                                                },
                                                methodScope
                                        )),
                                new ReturnStatement(
                                        new TernaryExpression(
                                                new BooleanExpression(
                                                        new BinaryExpression(
                                                                isExpectedVar,
                                                                Token.newSymbol(Types.LOGICAL_OR, -1, -1),
                                                                new BinaryExpression(
                                                                        new BinaryExpression(
                                                                                new VariableExpression(exceptionParam),
                                                                                Token.newSymbol(Types.COMPARE_EQUAL, -1, -1),
                                                                                ConstantExpression.NULL),
                                                                        Token.newSymbol(Types.LOGICAL_AND, -1, -1),
                                                                        new MethodCallExpression(
                                                                                new VariableExpression(expectedParam),
                                                                                "isEmpty",
                                                                                ArgumentListExpression.EMPTY_ARGUMENTS)))),
                                                new ConstantExpression(PerTestRecorder.NORMAL_EXIT),
                                                new ConstantExpression(PerTestRecorder.ABNORMAL_EXIT)
                                        ))
                        },
                        methodScope));
    }

    private long calculateChecksum(File file) throws IOException {
        if (file.exists()) {
            Reader fileReader;
            if (config.getEncoding() != null) {
                fileReader = new InputStreamReader(Files.newInputStream(file.toPath()), config.getEncoding());
            } else {
                fileReader = new FileReader(file);
            }
            final ChecksummingReader chksumReader = new ChecksummingReader(fileReader);
            while (chksumReader.read() != -1) { /*no-op*/ }
            return chksumReader.getChecksum();
        } else {
            return -1L;
        }
    }

    private String sanitisePackageName(String name) {
        if (name == null) {
            return "";
        } else {
            //package names are "com.foo.bar." - lop off the last .
            return name.substring(0, name.length() - 1);
        }
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

    private static <T extends ASTNode> T setSourcePosition(T node, int row, int column) {
        node.setLineNumber(row);
        node.setLastLineNumber(row);
        node.setColumnNumber(column);
        node.setLastColumnNumber(column);
        return node;
    }

}