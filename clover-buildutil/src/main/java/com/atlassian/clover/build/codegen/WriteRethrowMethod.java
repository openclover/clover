package com.atlassian.clover.build.codegen;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Rewrites the method org_openclover_runtime.CoverageRecorder.rethrow(Throwable) to
 * rethrow the supplied Throwable instead of just returning from the method.
 * This is required as test method rewriting needs to catch Throwable for the purposes
 * of determining test failure but must be able to rethrow the Throwable without changing
 * the signature of the rewritten method (in cases where it has not declared throwing Throwable).
 */
public class WriteRethrowMethod extends ClassVisitor {
    public WriteRethrowMethod(ClassVisitor classVisitor) {
        super(Opcodes.ASM5, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if ("rethrow".equals(name)) {
            return new RethrowRewriter(super.visitMethod(access, name, desc, signature, exceptions));
        } else {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    /**
     */
    public static class RethrowRewriter extends MethodVisitor {
        public RethrowRewriter(MethodVisitor methodVisitor) {
            super(Opcodes.ASM5, methodVisitor);
        }

        /** Converts "public void rethrow(Throwable t) {return;}" to "public void rethrow(Throwable t) {throw t;}" */
        @Override
        public void visitInsn(int instruction) {
            if (instruction == Opcodes.RETURN) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitInsn(Opcodes.ATHROW);
            } else {
                super.visitInsn(instruction);
            }
        }

        @Override
        public void visitMaxs(int stackSize, int localsSize) {
            //Ensure the stack size is at least 1 since we need to push "t" before throwing
            //Ensure the locals size is at least 2 since we need "this" and "t"
            super.visitMaxs(Math.max(1, stackSize), Math.max(2, localsSize));
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            File classFile = new File(args[0]);
            if (classFile.exists()) {
                ClassWriter classWriter = new ClassWriter(0);

                FileInputStream inputStream = new FileInputStream(classFile);
                ClassReader classReader;
                try {
                    classReader = new ClassReader(inputStream);
                } finally {
                    inputStream.close();
                }

                classReader.accept(new WriteRethrowMethod(classWriter), 0);

                try (FileChannel channel = new FileOutputStream(classFile).getChannel()) {
                    channel.write(ByteBuffer.wrap(classWriter.toByteArray()));
                }
            }
        } else {
            //Throwing rather than sending to stderr as we want the build to fail if the args are bad
            throw new Exception("Usage: java " + WriteRethrowMethod.class.getName() + " <file>");
        }
    }
}
