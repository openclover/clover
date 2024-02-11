package org.openclover.idea.content;

import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.api.registry.StatementInfo;
import org.openclover.core.registry.CoverageDataReceptor;
import org.openclover.core.registry.FileElementVisitor;
import com.intellij.openapi.editor.LogicalPosition;

import java.util.Collection;

import static org.openclover.core.util.Lists.newLinkedList;

public class StatementsAggregatingVisitor implements FileElementVisitor {

    private final Collection<ClassInfo> classInfos = newLinkedList();
    private final Collection<MethodInfo> methodInfos = newLinkedList();
    private final Collection<StatementInfo> statementInfos = newLinkedList();
    private final Collection<BranchInfo> branchInfos = newLinkedList();


    public Collection<BranchInfo> getBranchInfos() {
        return branchInfos;
    }

    public Collection<ClassInfo> getClassInfos() {
        return classInfos;
    }

    public Collection<MethodInfo> getMethodInfos() {
        return methodInfos;
    }

    public Collection<StatementInfo> getStatementInfos() {
        return statementInfos;
    }

    private final LogicalPosition position;

    public StatementsAggregatingVisitor(LogicalPosition position) {
        this.position = new LogicalPosition(position.line + 1, position.column); //Clover info is 1-based...
    }

    static class MostNarrowHelper {
        private SourceInfo mostNarrowOne;
        private LogicalPosition mostNarrowStart;
        private LogicalPosition mostNarrowEnd;

        SourceInfo getMostNarrowOne() {
            return mostNarrowOne;
        }

        private void updateMostNarrowRegion(SourceInfo candidate) {
            if (mostNarrowOne == null) {
                mostNarrowOne = candidate;
                mostNarrowStart = new LogicalPosition(candidate.getStartLine(), candidate.getStartColumn());
                mostNarrowEnd = new LogicalPosition(candidate.getEndLine(), candidate.getEndColumn());
            } else {
                final LogicalPosition start = new LogicalPosition(candidate.getStartLine(), candidate.getStartColumn());
                final LogicalPosition end = new LogicalPosition(candidate.getEndLine(), candidate.getEndColumn());
                if (start.compareTo(mostNarrowStart) >= 0 && end.compareTo(mostNarrowEnd) <= 0) {
                    mostNarrowOne = candidate;
                    mostNarrowStart = start;
                    mostNarrowEnd = end;
                }
            }
        }
    }

    private final MostNarrowHelper mostNarrowStatementHelper = new MostNarrowHelper();
    private final MostNarrowHelper mostNarrowMethodHelper = new MostNarrowHelper();
    private final MostNarrowHelper mostNarrowClassHelper = new MostNarrowHelper();

    public CoverageDataReceptor getMostNarrowClass() {
        return (CoverageDataReceptor) mostNarrowClassHelper.getMostNarrowOne();
    }

    public CoverageDataReceptor getMostNarrowMethod() {
        return (CoverageDataReceptor) mostNarrowMethodHelper.getMostNarrowOne();
    }

    public CoverageDataReceptor getMostNarrowStatement() {
        return (CoverageDataReceptor) mostNarrowStatementHelper.getMostNarrowOne();
    }

    private boolean contains(SourceInfo element) {
        final LogicalPosition start = new LogicalPosition(element.getStartLine(), element.getStartColumn() - 1);
        final LogicalPosition end = new LogicalPosition(element.getEndLine(), element.getEndColumn() - 1);

        return position.compareTo(start) >= 0 && position.compareTo(end) < 0;
    }


    @Override
    public void visitClass(ClassInfo info) {
        if (contains(info)) {
            mostNarrowClassHelper.updateMostNarrowRegion(info);
            classInfos.add(info);
        }
    }

    @Override
    public void visitMethod(MethodInfo info) {
        if (contains(info)) {
            mostNarrowMethodHelper.updateMostNarrowRegion(info);
            methodInfos.add(info);
        }
    }

    @Override
    public void visitStatement(StatementInfo info) {
        if (contains(info)) {
            mostNarrowStatementHelper.updateMostNarrowRegion(info);
            statementInfos.add(info);
        }
    }

    @Override
    public void visitBranch(BranchInfo info) {
        if (contains(info)) {
            mostNarrowStatementHelper.updateMostNarrowRegion(info);
            branchInfos.add(info);
        }
    }
}
