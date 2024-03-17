package org.openclover.core.api.registry;

public interface ElementVisitor {

    void visitClass(ClassInfo info);

    void visitMethod(MethodInfo info);

    void visitStatement(StatementInfo info);

    void visitBranch(BranchInfo info);

}
