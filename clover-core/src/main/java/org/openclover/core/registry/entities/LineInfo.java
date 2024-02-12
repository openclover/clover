package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.registry.ElementInfo;
import org.openclover.core.api.registry.SourceInfo;

import java.util.Comparator;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;


/**
 *  information about a particular source line.
 *  this class grows arrays linearly because they are almost always likely to be tiny (1 or 2 entries)
 */
public class LineInfo {
    public static Comparator<ElementInfo> COLUMN_COMPARATOR = Comparator.comparingInt(SourceInfo::getStartColumn);
    
    private int line;

    private static final FullClassInfo[] EMPTY_CLASS_INFOS = new FullClassInfo[] {};
    private static final FullMethodInfo[] EMPTY_METHOD_INFOS = new FullMethodInfo[] {};
    private static final FullStatementInfo[] EMPTY_STATEMENT_INFOS = new FullStatementInfo[] {};
    private static final FullBranchInfo[] EMPTY_BRANCH_INFOS = new FullBranchInfo[] {};

    private List<FullClassInfo>classStarts;
    private List<FullMethodInfo> methodStarts;
    private List<FullStatementInfo> statements;
    private List<FullBranchInfo> branches;
    private StackTraceInfo.TraceEntry[] failStackEntries;

    public LineInfo(int line) {
        this.line = line;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    @NotNull
    public List<FullElementInfo> getColumnOrderedElementInfos() {
        List<FullElementInfo> elements = newArrayList();
        if (methodStarts != null) {
            elements.addAll(methodStarts);
        }
        if (statements != null) {
            elements.addAll(statements);
        }
        if (branches != null) {
            elements.addAll(branches);
        }
        elements.sort(COLUMN_COMPARATOR);
        return elements; 
    }

    @NotNull
    public FullClassInfo[] getClassStarts() {
        if (classStarts == null) {
            return EMPTY_CLASS_INFOS;
        } else {
            return classStarts.toArray(new FullClassInfo[0]);
        }
    }

    @NotNull
    public FullMethodInfo[] getMethodStarts() {
        if (methodStarts == null) {
            return EMPTY_METHOD_INFOS;
        } else {
            return methodStarts.toArray(new FullMethodInfo[0]);
        }
    }

    @NotNull
    public FullStatementInfo[] getStatements() {
        if (statements == null) {
            return EMPTY_STATEMENT_INFOS;
        } else {
            return statements.toArray(new FullStatementInfo[0]);
        }
    }

    @NotNull
    public FullBranchInfo[] getBranches() {
        if (branches == null) {
            return EMPTY_BRANCH_INFOS;
        } else {
            return branches.toArray(new FullBranchInfo[0]);
        }
    }

    public StackTraceInfo.TraceEntry[] getFailStackEntries() {
        return failStackEntries;
    }

    void addClassStart(FullClassInfo clazz) {
        if (classStarts == null) {
            classStarts = newArrayList();
        }
        classStarts.add(clazz);
    }

    void addMethodStart(FullMethodInfo method) {
        if (methodStarts == null) {
            methodStarts = newArrayList();
        }
        methodStarts.add(method);
    }

    void addStatement(FullStatementInfo stmt) {
        if (statements == null) {
            statements = newArrayList();
        }
        statements.add(stmt);
    }

    void addBranch(FullBranchInfo branch) {
        if (branches == null) {
            branches = newArrayList();
        }
        branches.add(branch);
    }

    public void setFailStackEntries(StackTraceInfo.TraceEntry[] entries) {
        this.failStackEntries = new StackTraceInfo.TraceEntry[entries.length];
        System.arraycopy(entries,0, failStackEntries,0,entries.length);
    }

    public boolean hasMethodStarts() {
        return methodStarts != null;
    }

    public boolean hasClassStarts() {
        return classStarts != null;
    }

    public boolean hasFailStackEntries() {
        return failStackEntries != null;
    }

    public boolean hasBranches() {
        return branches != null;
    }

    public boolean hasStatements() {
        return statements != null;
    }
}
