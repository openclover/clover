package com.atlassian.clover.registry.util;

import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.EntityVisitor;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.StatementInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Set of helper classes to extract data using the EntityVisitor
 */
public class EntityVisitorUtils {

    public static class CheckParent {
        private final AtomicBoolean isMethod = new AtomicBoolean();
        private final EntityVisitor isMethodVisitor = new EntityVisitor() {
            @Override
            public void visitMethod(@NotNull MethodInfo parentMethod) {
                isMethod.set(true);
            }
        };

        public boolean isMethod(@NotNull MethodInfo thisMethod) {
            isMethod.set(false);
            thisMethod.getParent().visit(isMethodVisitor);
            return isMethod.get();
        }

        public boolean isMethod(@NotNull StatementInfo thisStatement) {
            isMethod.set(false);
            thisStatement.getParent().visit(isMethodVisitor);
            return isMethod.get();
        }

        public boolean isMethod(@NotNull BranchInfo thisBranch) {
            isMethod.set(false);
            thisBranch.getParent().visit(isMethodVisitor);
            return isMethod.get();
        }
    }

    public static class GetParent {
        private final AtomicReference<MethodInfo> parentMethod = new AtomicReference<>();
        private final EntityVisitor parentMethodVisitor = new EntityVisitor() {
            @Override
            public void visitMethod(@NotNull MethodInfo parent) {
                parentMethod.set(parent);
            }
        };

        @Nullable
        public MethodInfo asMethod(@NotNull MethodInfo thisMethod) {
            parentMethod.set(null);
            thisMethod.getParent().visit(parentMethodVisitor);
            return parentMethod.get();
        }

        @Nullable
        public MethodInfo asMethod(@NotNull StatementInfo thisStatement) {
            parentMethod.set(null);
            thisStatement.getParent().visit(parentMethodVisitor);
            return parentMethod.get();
        }

        @Nullable
        public MethodInfo asMethod(@NotNull BranchInfo thisBranch) {
            parentMethod.set(null);
            thisBranch.getParent().visit(parentMethodVisitor);
            return parentMethod.get();
        }
    }

    public final EntityVisitorUtils.CheckParent CHECK_PARENT = new EntityVisitorUtils.CheckParent();

    public final EntityVisitorUtils.GetParent GET_PARENT = new EntityVisitorUtils.GetParent();

    /**
     * Returns true if we have such chain of parents:
     * <pre>
     *     thisMethod -> method
     * </pre>
     *
     * @param thisMethod method to be checked
     * @return <code>true</code> if method is contained inside another method, <code>false</code> otherwise
     */
    public boolean isInnerMethod(@NotNull MethodInfo thisMethod) {
        return CHECK_PARENT.isMethod(thisMethod);
    }

    /**
     * Returns true if we have such chain of parents:
     * <pre>
     *     thisStatement -> method -> method
     * </pre>
     *
     * @param thisStatement statement to be checked
     * @return <code>true</code> if statement is contained inside an inner method, <code>false</code> otherwise
     */
    public boolean isParentAnInnerMethod(@NotNull StatementInfo thisStatement) {
        MethodInfo parentMethod = GET_PARENT.asMethod(thisStatement);
        return parentMethod != null && CHECK_PARENT.isMethod(parentMethod);
    }

    /**
     * Returns true if we have such chain of parents:
     * <pre>
     *     thisBranch -> method -> method
     * </pre>
     *
     * @param thisBranch branch to be checked
     * @return <code>true</code> if branch is contained inside an inner method, <code>false</code> otherwise
     */
    public boolean isParentAnInnerMethod(@NotNull BranchInfo thisBranch) {
        MethodInfo parentMethod = GET_PARENT.asMethod(thisBranch);
        return parentMethod != null && CHECK_PARENT.isMethod(parentMethod);
    }

    /**
     * Returns true if we have such chain of parents:
     * <pre>
     *     thisStatement -> parentMethod
     * </pre>
     * and parentMethod.isLambda() returns true.
     *
     * @param thisStatement statement to be checked
     * @return <code>true</code> if statement is contained inside an lambda method, <code>false</code> otherwise
     */
    public boolean isParentALambdaMethod(@NotNull StatementInfo thisStatement) {
        MethodInfo parentMethod = GET_PARENT.asMethod(thisStatement);
        return parentMethod != null && parentMethod.isLambda();
    }

    /**
     * Returns true if we have such chain of parents:
     * <pre>
     *     thisBranch -> parentMethod
     * </pre>
     * and parentMethod.isLambda() returns true.
     *
     * @param thisBranch branch to be checked
     * @return <code>true</code> if branch is contained inside an lambda method, <code>false</code> otherwise
     */
    public boolean isParentALambdaMethod(@NotNull BranchInfo thisBranch) {
        MethodInfo parentMethod = GET_PARENT.asMethod(thisBranch);
        return parentMethod != null && parentMethod.isLambda();
    }


    /**
     * Returns true if we have such chain of parents:
     * <pre>
     *     thisStatement -> method
     * </pre>
     *
     * @param thisStatement statement to be checked
     * @return <code>true</code> if statement is contained inside a method, <code>false</code> otherwise
     */
    public boolean isParentAMethod(@NotNull StatementInfo thisStatement) {
        return CHECK_PARENT.isMethod(thisStatement);
    }

    /**
     * Returns true if we have such chain of parents:
     * <pre>
     *     thisBranch -> method
     * </pre>
     *
     * @param thisBranch branch to be checked
     * @return <code>true</code> if statement is contained inside a method, <code>false</code> otherwise
     */
    public boolean isParentAMethod(@NotNull BranchInfo thisBranch) {
        return CHECK_PARENT.isMethod(thisBranch);
    }
}