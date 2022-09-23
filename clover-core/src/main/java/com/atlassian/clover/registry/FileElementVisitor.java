package com.atlassian.clover.registry;

import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.StatementInfo;

public interface FileElementVisitor {

    void visitClass(ClassInfo info);

    void visitMethod(MethodInfo info);

    void visitStatement(StatementInfo info);

    void visitBranch(BranchInfo info);

}
