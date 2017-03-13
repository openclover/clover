package com.atlassian.clover.instr.groovy.bytecode;

import com.atlassian.clover.CloverNames;
import com_atlassian_clover.Clover;
import com_atlassian_clover.CloverProfile;
import com_atlassian_clover.CoverageRecorder;
import org.codehaus.groovy.ast.ClassNode;

import java.util.List;

/**
 * This class MUST be compiled with Groovy 2.x in classpath.
 * @see AbstractRecorderGetterBytecodeInstruction
 */
public class RecorderGetterBytecodeInstructionGroovy2 extends AbstractRecorderGetterBytecodeInstruction {

    public RecorderGetterBytecodeInstructionGroovy2(ClassNode clazz, String fieldName,
                                                    String initString, String distConfig,
                                                    long registryVersion, long recorderConfig, int maxElements,
                                                    List<CloverProfile> profiles) {
        super(clazz, fieldName, initString, distConfig, registryVersion, recorderConfig, maxElements, profiles);
    }

    /**
     * Attention: this method is identical as in RecorderGetterBytecodeInstructionGroovy1#visit().
     * The difference in on the byte code level - in Groovy 2.x groovyjarjarasm.asm.MethodVisitor is a Class.
     */
    @Override
    public void visit(groovyjarjarasm.asm.MethodVisitor methodVisitor) {
        String classInternalName = ByteCodeUtilsGroovy2.getClassInternalName(clazz);
        String recorderDesciptor = groovyjarjarasm.asm.Type.getDescriptor(CoverageRecorder.class);

        groovyjarjarasm.asm.Label returnLabel = new groovyjarjarasm.asm.Label();

        methodVisitor.visitFieldInsn(
                groovyjarjarasm.asm.Opcodes.GETSTATIC,
                classInternalName,
                fieldName,
                recorderDesciptor);

        methodVisitor.visitJumpInsn(groovyjarjarasm.asm.Opcodes.IFNONNULL, returnLabel);

        // 1st-4th arg
        methodVisitor.visitLdcInsn(initString);
        methodVisitor.visitLdcInsn(registryVersion);
        methodVisitor.visitLdcInsn(recorderConfig);
        methodVisitor.visitLdcInsn(maxElements);
        // 5th arg
        pushCloverProfilesOnStack(methodVisitor);

        // 6th arg
        //new array of type java.lang.String with size 2
        methodVisitor.visitInsn(groovyjarjarasm.asm.Opcodes.ICONST_2);
        methodVisitor.visitTypeInsn(groovyjarjarasm.asm.Opcodes.ANEWARRAY, groovyjarjarasm.asm.Type.getType(String.class).getInternalName());

        //Set the first element
        methodVisitor.visitInsn(groovyjarjarasm.asm.Opcodes.DUP);
        methodVisitor.visitInsn(groovyjarjarasm.asm.Opcodes.ICONST_0);
        methodVisitor.visitLdcInsn(CloverNames.PROP_DISTRIBUTED_CONFIG);
        methodVisitor.visitInsn(groovyjarjarasm.asm.Opcodes.AASTORE);

        //Set the second element
        methodVisitor.visitInsn(groovyjarjarasm.asm.Opcodes.DUP);
        methodVisitor.visitInsn(groovyjarjarasm.asm.Opcodes.ICONST_1);
        if (distConfig == null) {
            methodVisitor.visitInsn(groovyjarjarasm.asm.Opcodes.ACONST_NULL);
        } else {
            methodVisitor.visitLdcInsn(distConfig);
        }
        methodVisitor.visitInsn(groovyjarjarasm.asm.Opcodes.AASTORE);

        methodVisitor.visitMethodInsn(
                groovyjarjarasm.asm.Opcodes.INVOKESTATIC,
                groovyjarjarasm.asm.Type.getInternalName(Clover.class),
                "getRecorder",
                groovyjarjarasm.asm.Type.getMethodDescriptor(
                        groovyjarjarasm.asm.Type.getType(CoverageRecorder.class),
                        new groovyjarjarasm.asm.Type[]{
                                groovyjarjarasm.asm.Type.getType(String.class),
                                groovyjarjarasm.asm.Type.LONG_TYPE,
                                groovyjarjarasm.asm.Type.LONG_TYPE,
                                groovyjarjarasm.asm.Type.INT_TYPE,
                                groovyjarjarasm.asm.Type.getType(CloverProfile[].class),
                                groovyjarjarasm.asm.Type.getType(String[].class)}));

        methodVisitor.visitFieldInsn(
                groovyjarjarasm.asm.Opcodes.PUTSTATIC,
                classInternalName,
                fieldName,
                recorderDesciptor);

        methodVisitor.visitLabel(returnLabel);
        methodVisitor.visitFieldInsn(
                groovyjarjarasm.asm.Opcodes.GETSTATIC,
                classInternalName,
                fieldName,
                recorderDesciptor);
        methodVisitor.visitInsn(groovyjarjarasm.asm.Opcodes.ARETURN);
    }

    /**
     * @see #pushCloverProfilesOnStack(org.objectweb.asm.MethodVisitor)
     * @param mv visitor of the method to be modified
     */
    private void pushCloverProfilesOnStack(groovyjarjarasm.asm.MethodVisitor mv) {
        if ((profiles == null) || (profiles.isEmpty())) {
            mv.visitInsn(groovyjarjarasm.asm.Opcodes.ACONST_NULL);
        } else {
            // put array size
            ByteCodeUtilsGroovy2.pushConstant(mv, profiles.size());
            // put array type
            mv.visitTypeInsn(groovyjarjarasm.asm.Opcodes.ANEWARRAY, groovyjarjarasm.asm.Type.getInternalName(CloverProfile.class));

            // walk through all profiles and create new intances in this array
            for (int i = 0; i < profiles.size(); i++) {
                CloverProfile profile = profiles.get(i);
                // put array index and object type under it
                mv.visitInsn(groovyjarjarasm.asm.Opcodes.DUP);
                ByteCodeUtilsGroovy2.pushConstant(mv, i);
                mv.visitTypeInsn(groovyjarjarasm.asm.Opcodes.NEW, groovyjarjarasm.asm.Type.getInternalName(CloverProfile.class));
                mv.visitInsn(groovyjarjarasm.asm.Opcodes.DUP);
                // put 1st arg - profile name
                mv.visitLdcInsn(profile.getName());
                // put 2nd arg - coverage recorder type
                mv.visitLdcInsn(profile.getCoverageRecorder().toString());
                // put 3rd arg - distributed config string or null
                if (profile.getDistributedCoverage() == null) {
                    mv.visitInsn(groovyjarjarasm.asm.Opcodes.ACONST_NULL);
                } else {
                    mv.visitLdcInsn(profile.getDistributedCoverage().getConfigString());
                }
                // call new CloverProfile(String,String,String)
                mv.visitMethodInsn(groovyjarjarasm.asm.Opcodes.INVOKESPECIAL,
                        groovyjarjarasm.asm.Type.getInternalName(CloverProfile.class),
                        "<init>",
                        groovyjarjarasm.asm.Type.getMethodDescriptor(
                                groovyjarjarasm.asm.Type.VOID_TYPE,
                                new groovyjarjarasm.asm.Type[]{
                                        groovyjarjarasm.asm.Type.getType(String.class),
                                        groovyjarjarasm.asm.Type.getType(String.class),
                                        groovyjarjarasm.asm.Type.getType(String.class)}));
                // store object on stack
                mv.visitInsn(groovyjarjarasm.asm.Opcodes.AASTORE);
            }
        }
    }

    public void visit(org.objectweb.asm.MethodVisitor methodVisitor) {
        String classInternalName = ByteCodeUtilsGroovy1.getClassInternalName(clazz);
        String recorderDesciptor = org.objectweb.asm.Type.getDescriptor(CoverageRecorder.class);

        org.objectweb.asm.Label returnLabel = new org.objectweb.asm.Label();

        methodVisitor.visitFieldInsn(
                org.objectweb.asm.Opcodes.GETSTATIC,
                classInternalName,
                fieldName,
                recorderDesciptor);

        methodVisitor.visitJumpInsn(org.objectweb.asm.Opcodes.IFNONNULL, returnLabel);

        // push arguments on stack for Clover.getRecorder(String initChars, long dbVersion, long cfgbits,
        // int maxNumElements, CloverProfile[] profiles, String[] nvpProperties)
        // 1st-4th arg
        methodVisitor.visitLdcInsn(initString);
        methodVisitor.visitLdcInsn(registryVersion);
        methodVisitor.visitLdcInsn(recorderConfig);
        methodVisitor.visitLdcInsn(maxElements);
        // 5th arg
        pushCloverProfilesOnStack(methodVisitor);
        // 6th arg
        pushPropertiesArrayOnStack(methodVisitor);

        methodVisitor.visitMethodInsn(
                org.objectweb.asm.Opcodes.INVOKESTATIC,
                org.objectweb.asm.Type.getInternalName(Clover.class),
                "getRecorder",
                org.objectweb.asm.Type.getMethodDescriptor(
                        org.objectweb.asm.Type.getType(CoverageRecorder.class),
                        new org.objectweb.asm.Type[]{
                                org.objectweb.asm.Type.getType(String.class),
                                org.objectweb.asm.Type.LONG_TYPE,
                                org.objectweb.asm.Type.LONG_TYPE,
                                org.objectweb.asm.Type.INT_TYPE,
                                org.objectweb.asm.Type.getType(CloverProfile[].class),
                                org.objectweb.asm.Type.getType(String[].class)}));

        methodVisitor.visitFieldInsn(
                org.objectweb.asm.Opcodes.PUTSTATIC,
                classInternalName,
                fieldName,
                recorderDesciptor);

        methodVisitor.visitLabel(returnLabel);
        methodVisitor.visitFieldInsn(
                org.objectweb.asm.Opcodes.GETSTATIC,
                classInternalName,
                fieldName,
                recorderDesciptor);
        methodVisitor.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
    }

    /**
     * Generates code like this (if this.profiles != null):
     * <pre>
     *   new CloverProfile[] {
     *         new CloverProfile("aa", "bb", null),
     *         new CloverProfile("cc", "dd", "ee"),
     *         ...
     *   }
     * </pre>
     *
     * or (if this.profiles == null):
     *
     * <pre>
     *   null
     * </pre>
     *
     * and pushes the array/null on a stack for further method call.
     * @param mv visitor of the method to be modified
     */
    private void pushCloverProfilesOnStack(org.objectweb.asm.MethodVisitor mv) {

        if ((profiles == null) || (profiles.isEmpty())) {
            mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
        } else {
            // put array size
            ByteCodeUtilsGroovy1.pushConstant(mv, profiles.size());
            // put array type
            mv.visitTypeInsn(org.objectweb.asm.Opcodes.ANEWARRAY, org.objectweb.asm.Type.getInternalName(CloverProfile.class));

            // walk through all profiles and create new intances in this array
            for (int i = 0; i < profiles.size(); i++) {
                CloverProfile profile = profiles.get(i);
                // put array index and object type under it
                mv.visitInsn(org.objectweb.asm.Opcodes.DUP);
                ByteCodeUtilsGroovy1.pushConstant(mv, i);
                mv.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, org.objectweb.asm.Type.getInternalName(CloverProfile.class));
                mv.visitInsn(org.objectweb.asm.Opcodes.DUP);
                // put 1st arg - profile name
                mv.visitLdcInsn(profile.getName());
                // put 2nd arg - coverage recorder type
                mv.visitLdcInsn(profile.getCoverageRecorder().toString());
                // put 3rd arg - distributed config string or null
                if (profile.getDistributedCoverage() == null) {
                    mv.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
                } else {
                    mv.visitLdcInsn(profile.getDistributedCoverage().getConfigString());
                }
                // call new CloverProfile(String,String,String)
                mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,
                        org.objectweb.asm.Type.getInternalName(CloverProfile.class),
                        "<init>",
                        org.objectweb.asm.Type.getMethodDescriptor(
                                org.objectweb.asm.Type.VOID_TYPE,
                                new org.objectweb.asm.Type[]{
                                        org.objectweb.asm.Type.getType(String.class),
                                        org.objectweb.asm.Type.getType(String.class),
                                        org.objectweb.asm.Type.getType(String.class)}));
                // store object on stack
                mv.visitInsn(org.objectweb.asm.Opcodes.AASTORE);
            }
        }
    }

    /**
     * Generates set of opcodes for the following code:
     * <pre>
     *     new String[2] = { CloverNames.PROP_DISTRIBUTED_CONFIG, this.distConfig }
     * </pre>
     * which is pushed on a stack for further method call.
     */
    private void pushPropertiesArrayOnStack(org.objectweb.asm.MethodVisitor methodVisitor) {
        //new array of type java.lang.String with size 2
        methodVisitor.visitInsn(org.objectweb.asm.Opcodes.ICONST_2);
        methodVisitor.visitTypeInsn(org.objectweb.asm.Opcodes.ANEWARRAY, org.objectweb.asm.Type.getType(String.class).getInternalName());

        //Set the first element
        methodVisitor.visitInsn(org.objectweb.asm.Opcodes.DUP);
        methodVisitor.visitInsn(org.objectweb.asm.Opcodes.ICONST_0);
        methodVisitor.visitLdcInsn(CloverNames.PROP_DISTRIBUTED_CONFIG);
        methodVisitor.visitInsn(org.objectweb.asm.Opcodes.AASTORE);

        //Set the second element
        methodVisitor.visitInsn(org.objectweb.asm.Opcodes.DUP);
        methodVisitor.visitInsn(org.objectweb.asm.Opcodes.ICONST_1);
        if (distConfig == null) {
            methodVisitor.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
        } else {
            methodVisitor.visitLdcInsn(distConfig);
        }
        methodVisitor.visitInsn(org.objectweb.asm.Opcodes.AASTORE);
    }
}
