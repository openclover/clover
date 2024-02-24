package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ElementInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.api.registry.StatementInfo;

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

    private static final ClassInfo[] EMPTY_CLASS_INFOS = new ClassInfo[] {};
    private static final MethodInfo[] EMPTY_METHOD_INFOS = new MethodInfo[] {};
    private static final StatementInfo[] EMPTY_STATEMENT_INFOS = new StatementInfo[] {};
    private static final BranchInfo[] EMPTY_BRANCH_INFOS = new BranchInfo[] {};

    private List<ClassInfo> classStarts;
    private List<MethodInfo> methodStarts;
    private List<StatementInfo> statements;
    private List<BranchInfo> branches;
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
    public List<ElementInfo> getColumnOrderedElementInfos() {
        List<ElementInfo> elements = newArrayList();
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
    public ClassInfo[] getClassStarts() {
        if (classStarts == null) {
            return EMPTY_CLASS_INFOS;
        } else {
            return classStarts.toArray(new ClassInfo[0]);
        }
    }

    @NotNull
    public MethodInfo[] getMethodStarts() {
        if (methodStarts == null) {
            return EMPTY_METHOD_INFOS;
        } else {
            return methodStarts.toArray(new MethodInfo[0]);
        }
    }

    @NotNull
    public StatementInfo[] getStatements() {
        if (statements == null) {
            return EMPTY_STATEMENT_INFOS;
        } else {
            return statements.toArray(new StatementInfo[0]);
        }
    }

    @NotNull
    public BranchInfo[] getBranches() {
        if (branches == null) {
            return EMPTY_BRANCH_INFOS;
        } else {
            return branches.toArray(new BranchInfo[0]);
        }
    }

    public StackTraceInfo.TraceEntry[] getFailStackEntries() {
        return failStackEntries;
    }

    void addClassStart(ClassInfo clazz) {
        if (classStarts == null) {
            classStarts = newArrayList();
        }
        classStarts.add(clazz);
    }

    void addMethodStart(MethodInfo method) {
        if (methodStarts == null) {
            methodStarts = newArrayList();
        }
        methodStarts.add(method);
    }

    void addStatement(StatementInfo stmt) {
        if (statements == null) {
            statements = newArrayList();
        }
        statements.add(stmt);
    }

    void addBranch(BranchInfo branch) {
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
