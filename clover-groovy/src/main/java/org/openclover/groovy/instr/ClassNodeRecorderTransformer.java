package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.jetbrains.annotations.Nullable;
import org.openclover.groovy.instr.bytecode.RecorderGetterBytecodeInstructionGroovy2;

import static groovyjarjarasm.asm.Opcodes.ACC_PRIVATE;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;
import static groovyjarjarasm.asm.Opcodes.ACC_SYNTHETIC;

class ClassNodeRecorderTransformer implements ClassNodeTransformer {

    private final GroovyInstrumentationConfig sessionConfig;
    private final String recorderFieldName;
    private final String recorderGetterName;

    ClassNodeRecorderTransformer(GroovyInstrumentationConfig sessionConfig,
                                 String recorderFieldName, String recorderGetterName) {
        this.sessionConfig = sessionConfig;
        this.recorderFieldName = recorderFieldName;
        this.recorderGetterName = recorderGetterName;
    }

    @Override
    public void transform(ClassNode classNode, @Nullable GroovyInstrumentationResult flags) {
        createRecorderField(classNode);
        createRecorderGetter(classNode, sessionConfig);
    }

    /**
     * Creates a static field for CoverageRecorder:
     * <pre>
     *     private static CoverageRecorder $CLV_R$ = null;
     * </pre>
     */
    private void createRecorderField(final ClassNode clazz) {
        // add field
        clazz.addField(recorderFieldName, ACC_STATIC | ACC_PRIVATE | ACC_SYNTHETIC,
                ClassHelper.make(org_openclover_runtime.CoverageRecorder.class),
                ConstantExpression.NULL);
    }

    /**
     * Creates a static method (lazy initialization) like:
     * <pre>
     *     private static CoverageRecorder $CLV_R$() {
     *         ...
     *     }
     * </pre>
     */
    private void createRecorderGetter(final ClassNode clazz, GroovyInstrumentationConfig sessionConfig) {
        // add method (no code yet)
        clazz.addMethod(recorderGetterName, ACC_STATIC | ACC_PRIVATE | ACC_SYNTHETIC,
                ClassHelper.make(org_openclover_runtime.CoverageRecorder.class),
                new Parameter[]{}, new ClassNode[]{},
                new BlockStatement());

        // getter method will be filled with byte code in next stage, see CloverAstTransformerInstructionSelection
        fillRecorderGetterBytecode(clazz, sessionConfig);
    }

    private void fillRecorderGetterBytecode(ClassNode clazz, GroovyInstrumentationConfig sessionConfig) {
        final MethodNode recorderGetter = clazz.getMethod(recorderGetterName, new Parameter[0]);
        final BytecodeInstruction bytecodeInstruction = newRecorderGetterBytecodeInstruction(clazz, sessionConfig);
        ((BlockStatement) recorderGetter.getCode()).addStatement(new BytecodeSequence(bytecodeInstruction));
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
}
