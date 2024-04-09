module org.openclover.buildutil {
    requires java.compiler;
    requires org.objectweb.asm;

    exports org.openclover.buildutil.codegen;
    exports org.openclover.buildutil.instr;
}