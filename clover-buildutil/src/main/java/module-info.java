module org.openclover.buildutil {
    requires java.compiler;
    requires asm;
//    requires asm;

    exports org.openclover.buildutil.codegen;
    exports org.openclover.buildutil.instr;
}