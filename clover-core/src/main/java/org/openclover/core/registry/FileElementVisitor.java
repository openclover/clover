package org.openclover.core.registry;

import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.StatementInfo;

public interface FileElementVisitor {

    void visitClass(ClassInfo info);

    void visitMethod(MethodInfo info);

    void visitStatement(StatementInfo info);

    void visitBranch(BranchInfo info);

}
