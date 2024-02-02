package org.openclover.groovy.instr.bytecode;

import com_atlassian_clover.CloverProfile;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.BytecodeInstruction;

import java.util.List;


/**
 * Bytecode that defines the instructions in $CLV_R$() on instrumented classes:
 *
 * <pre>
 * private static CoverageRecorder $CLV_R$() {
 *   if ($CLV_R$ == null) {
 *      $CLV_R$ = Clover.getRecorder(
 *          initstring, registryVersion, recorderConfig, maxElements,
 *          new CloverProfiles[] {
 *                  new CloverProfile("arg1", "arg2", "arg3"),
 *                  new CloverProfile("arg1", "arg2", null),
 *                  ... },
 *          new String[] { "clover.distributed.config", distConfig });
 *   }
 *   return $CLV_R$;
 * }
 * </pre>
 *
 * Concrete classes should have implementation of the visit() method:
 * <pre>
 *  - public void visit(org.objectweb.asm.MethodVisitor)
 *     => used by groovy-eclipse-batch Maven plugin
 *     => references ASM 3.x (MethodVisitor is an interface) or ASM 4.x (MethodVisitor is a class)
 *  - public void visit(groovyjjarjarasm.asm.MethodVisitor)
 *     => used by groovyc and grails
 *     => references Groovy 1.x (MethodVisitor is an interface) or Groovy 2.x (MethodVisitor is a class)
 * </pre>
 *
 * @see BytecodeInstruction#visit(groovyjarjarasm.asm.MethodVisitor)
 * @see org.objectweb.asm.MethodVisitor
 * @see groovyjarjarasm.asm.MethodVisitor
 */
public abstract class AbstractRecorderGetterBytecodeInstruction extends BytecodeInstruction {
    protected final ClassNode clazz;
    protected final String fieldName;
    protected final String initString;
    protected final String distConfig;
    protected final long registryVersion;
    protected final long recorderConfig;
    protected final int maxElements;
    protected final List<CloverProfile> profiles;

    public AbstractRecorderGetterBytecodeInstruction(ClassNode clazz, String fieldName,
            String initString, String distConfig,
            long registryVersion, long recorderConfig, int maxElements,
            List<CloverProfile> profiles) {
        this.clazz = clazz;
        this.fieldName = fieldName;
        this.initString = initString;
        this.distConfig = distConfig;
        this.registryVersion = registryVersion;
        this.recorderConfig = recorderConfig;
        this.maxElements = maxElements;
        this.profiles = profiles;
    }

}
