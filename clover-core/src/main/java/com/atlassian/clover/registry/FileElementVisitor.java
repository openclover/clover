package com.atlassian.clover.registry;

import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.StatementInfo;

public interface FileElementVisitor {

    public void visitClass(ClassInfo info);

    public void visitMethod(MethodInfo info);

    public void visitStatement(StatementInfo info);

    public void visitBranch(BranchInfo info);

}
