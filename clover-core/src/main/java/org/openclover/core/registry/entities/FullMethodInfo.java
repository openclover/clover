package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.EntityContainer;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasAggregatedMetrics;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.api.registry.StatementInfo;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.core.lang.Languages;
import org.openclover.core.registry.CoverageDataProvider;
import org.openclover.core.registry.FileElementVisitor;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.metrics.HasMetricsNode;
import org.openclover.core.spi.lang.LanguageConstruct;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.openclover.core.util.Lists.newArrayList;


public class FullMethodInfo extends FullElementInfo<BasicMethodInfo> implements HasAggregatedMetrics, HasMetricsNode, TaggedPersistent, MethodInfo {
    public static final int DEFAULT_METHOD_COMPLEXITY = 1;

    private List<FullStatementInfo> statements = newArrayList();
    private List<FullBranchInfo> branches = newArrayList();
    private List<FullClassInfo> innerClasses = newArrayList();
    private List<FullMethodInfo> innerMethods = newArrayList();

    private int aggregatedStatementCount;  // calculated during instrumentation
    private int aggregatedComplexity;  // calculated during instrumentation
    private boolean voidReturnType;

    private transient BlockMetrics rawMetrics;
    private transient BlockMetrics metrics;
    private transient ContextSet contextFilter;
    private transient CoverageDataProvider data;
    private transient ParentEntity parent;

    public FullMethodInfo(
            FullClassInfo containingClass, int relativeDataIndex, ContextSet context, SourceInfo region,
            MethodSignature signature, boolean isTest, String staticTestName, boolean isLambda, int complexity) {
        this(containingClass, relativeDataIndex, context, region,
                signature, isTest, staticTestName, isLambda, complexity, LanguageConstruct.Builtin.METHOD);
    }

    public FullMethodInfo(
            FullClassInfo containingClass, int relativeDataIndex, ContextSet context, SourceInfo region,
            MethodSignature signature, boolean isTest, String staticTestName, boolean isLambda, int complexity, LanguageConstruct construct) {

        this(containingClass, context,
                new BasicMethodInfo(region, relativeDataIndex, complexity, signature, isTest, staticTestName, isLambda, construct));
    }

    /**
     * For method-in-class
     */
    public FullMethodInfo(FullClassInfo containingClass, ContextSet context, BasicMethodInfo methodInfo) {
        super(context, methodInfo);
        parent = new ParentEntity(containingClass);
    }

    /**
     * For method-in-method
     */
    public FullMethodInfo(FullMethodInfo containingMethod, ContextSet context, BasicMethodInfo methodInfo) {
        super(context, methodInfo);
        parent = new ParentEntity(containingMethod);
    }

    /**
     * For method-in-file
     */
    public FullMethodInfo(FullFileInfo containingFile, ContextSet context, BasicMethodInfo methodInfo) {
        super(context, methodInfo);
        parent = new ParentEntity(containingFile);
    }

    /**
     * Private constructor for data serialization
     */
    private FullMethodInfo(MethodSignature signature, ContextSet context,
                           int relativeDataIndex, int dataLength,
                           int complexity, LanguageConstruct construct, SourceInfo region,
                           boolean isTest, String staticTestName, boolean isLambda,
                           List<FullStatementInfo> statements, List<FullBranchInfo> branches,
                           List<FullClassInfo> classes, List<FullMethodInfo> methods) {
        super(context,
                new BasicMethodInfo(region, relativeDataIndex, dataLength, complexity, signature,
                                    isTest, staticTestName, isLambda, construct));
        this.statements = statements;
        this.branches = branches;
        this.innerClasses = classes;
        this.innerMethods = methods;
    }

    @Override
    public String getName() {
        return sharedInfo.getName();
    }

    @Override
    public String getSimpleName() {
        return sharedInfo.getSignature().getName();
    }

    @Override
    @NotNull
    public MethodSignature getSignature() {
        return sharedInfo.getSignature();
    }

    @Override
    public String getQualifiedName() {
        final String prefix =
                (parent.getContainingClass() != null ? parent.getContainingClass().getQualifiedName()
                        : (parent.getContainingMethod() != null ? parent.getContainingMethod().getQualifiedName()
                                : ""));
        return prefix + "." + sharedInfo.getSignature().getName();
    }

    @Override
    @Nullable
    public ClassInfo getContainingClass() {
       return parent.getContainingClass();
    }

    @Override
    @Nullable
    public MethodInfo getContainingMethod() {
        return parent.getContainingMethod();
    }

    @Override
    public boolean isTest() {
       return sharedInfo.isTest();
    }

    @Override
    @Nullable
    public String getStaticTestName() {
        return sharedInfo.getStaticTestName();
    }

    @Override
    public boolean isLambda() {
        return sharedInfo.isLambda();
    }

    @Override
    public boolean isEmpty() {
        return statements.size() == 0 && branches.size() == 0;
    }

    @Override
    public ContextSet getContextFilter() {
        return contextFilter != null ? contextFilter : getParentContextFilter();
    }

    // aggregated metrics

    @Override
    public int getAggregatedStatementCount() {
        return aggregatedStatementCount;
    }

    @Override
    public void setAggregatedStatementCount(int aggregatedStatementCount) {
        this.aggregatedStatementCount = aggregatedStatementCount;
    }

    public void increaseAggregatedStatementCount(int increment) {
        this.aggregatedStatementCount += increment;
    }

    @Override
    public int getAggregatedComplexity() {
        return aggregatedComplexity;
    }

    @Override
    public void setAggregatedComplexity(int aggregatedComplexity) {
        this.aggregatedComplexity = aggregatedComplexity;
    }

    public void increaseAggregatedComplexity(int increment) {
        this.aggregatedComplexity += increment;
    }

    // inner classes and functions

    public void addClass(FullClassInfo classInfo) {
        innerClasses.add(classInfo);
    }

    public void addMethod(FullMethodInfo methodInfo) {
        innerMethods.add(methodInfo);
    }

    /**
     * {@inheritDoc}
     *
     * @return List&lt;? extends ClassInfo&gt; - a cloned list
     */
    @Override
    @NotNull
    public List<? extends ClassInfo> getClasses() {
        return newArrayList(innerClasses); // copy
    }

    @Override
    @NotNull
    public List<? extends ClassInfo> getAllClasses() {
        final List<ClassInfo> allClasses = newArrayList();
        // in-order
        for (FullClassInfo classInfo : innerClasses) {
            allClasses.add(classInfo);
            allClasses.addAll(classInfo.getAllClasses());
        }
        for (FullMethodInfo methodInfo : innerMethods) {
            allClasses.addAll(methodInfo.getAllClasses());
        }
        return allClasses;
    }

    /**
     * {@inheritDoc}
     *
     * @return List&lt;? extends MethodInfo&gt; - a cloned list
     */
    @Override
    @NotNull
    public List<? extends MethodInfo> getMethods() {
        return newArrayList(innerMethods); // copy
    }

    @Override
    @NotNull
    public List<? extends MethodInfo> getAllMethods() {
        final List<MethodInfo> allMethods = newArrayList();
        // in-order
        for (FullMethodInfo methodInfo : innerMethods) {
            allMethods.add(methodInfo);
            allMethods.addAll(methodInfo.getAllMethods());
        }
        for (FullClassInfo classInfo : innerClasses) {
            allMethods.addAll(classInfo.getAllMethods());
        }
        return allMethods;
    }

    // statements and branches

    public void addStatement(FullStatementInfo stmt) {
        statements.add(stmt);
    }

    public void addBranch(FullBranchInfo branch) {
        branches.add(branch);
    }

    @Override
    @NotNull
    public List<? extends StatementInfo> getStatements() {
        return newArrayList(statements); // clone
    }
    
    @Override
    @NotNull
    public List<? extends BranchInfo> getBranches() {
        return newArrayList(branches); // copy
    }

    public int getBranchCount() {
        return branches.size();
    }

    private ContextSet getParentContextFilter() {
        final AtomicReference<ContextSet> parentContextFilter = new AtomicReference<>();

        parent.getParentEntity().visit(new EntityVisitor() {
            @Override
            public void visitFile(final FileInfo parentFile) {
                parentContextFilter.set(parentFile.getContextFilter());
            }

            @Override
            public void visitClass(ClassInfo parentClass) {
                parentContextFilter.set(parentClass.getContextFilter());
            }

            @Override
            public void visitMethod(MethodInfo parentMethod) {
                parentContextFilter.set(parentMethod.getContextFilter());
            }
        });

        return parentContextFilter.get();
    }

    // code metrics

    @Override
    public BlockMetrics getMetrics() {
        final ContextSet parentContextFilter = getParentContextFilter();
        if (metrics == null || parentContextFilter != contextFilter) {
            contextFilter = parentContextFilter;
            metrics = calcMetrics(contextFilter);
        }
        return metrics;
    }

    @Override
    public BlockMetrics getRawMetrics() {
        if (this.rawMetrics == null) {
            this.rawMetrics = calcMetrics(null);
        }
        return rawMetrics;
    }

    @Override
    public void setMetrics(BlockMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void setDataProvider(final CoverageDataProvider data) {
        this.data = data;
        for (FullMethodInfo methodInfo : innerMethods) {
            methodInfo.setDataProvider(data);
        }
        for (FullClassInfo classInfo : innerClasses) {
            classInfo.setDataProvider(data);
        }
        // note: don't call setDataProvider on 'statements' because FullStatementInfo takes provider form its parent
        rawMetrics = null;
        metrics = null;
    }

    public void gatherSourceRegions(Set<SourceInfo> regions) {
        // this method and its code
        regions.add(this);
        regions.addAll(statements);
        regions.addAll(branches);
        // inline classes
        for (FullClassInfo classInfo : innerClasses) {
            classInfo.gatherSourceRegions(regions);
        }
        // inner functions
        for (FullMethodInfo methodInfo : innerMethods) {
            methodInfo.gatherSourceRegions(regions);
        }
    }

    /**
     * Visit yourself
     *
     * @param entityVisitor callback
     */
    @Override
    public void visit(EntityVisitor entityVisitor) {
        entityVisitor.visitMethod(this);
    }

    public void visit(FileElementVisitor visitor) {
        // this method
        visitor.visitMethod(this);
        // this method's statements and branches
        for (FullStatementInfo statementInfo : statements) {
            visitor.visitStatement(statementInfo);
        }
        for (BranchInfo branchInfo : branches) {
            visitor.visitBranch(branchInfo);
        }
        // inline classes
        for (FullClassInfo classInfo : innerClasses) {
            classInfo.visitElements(visitor);
        }
        // inner functions
        for (FullMethodInfo methodInfo : innerMethods) {
            methodInfo.visit(visitor);
        }
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public int getNumChildren() {
        return 0;
    }

    @Override
    public String getChildType() {
        return null;
    }

    @Override
    public HasMetricsNode getChild(int i) {
        return null;
    }

    @Override
    public int getIndexOfChild(HasMetricsNode child) {
        return -1;
    }

    @Override
    public void setComparator(Comparator<HasMetrics> cmp) {
        //not applied at this level
    }


    private BlockMetrics calcMetrics(ContextSet contextSet) {

        org.openclover.core.registry.metrics.BlockMetrics blockMetrics = new org.openclover.core.registry.metrics.BlockMetrics(this);

        int covered = 0;
        int numStatements = 0;
        int numBranches = 0;
        int complexity = 1; // empty methods have a complexity of 1

        // sum metrics from inner classes
        for (FullClassInfo classInfo : innerClasses) {
            blockMetrics.add(classInfo.getMetrics());
        }

        // sum metrics from inner methods
        for (FullMethodInfo methodInfo : innerMethods) {
            blockMetrics.add(methodInfo.getMetrics());
        }

        // sum metrics from statements declared in a method body
        for (FullStatementInfo statementInfo : statements) {
            if (statementInfo.isFiltered(contextSet)) {
                continue;
            }
            if (statementInfo.getHitCount() > 0) {
                covered++;
            }
            complexity += statementInfo.getComplexity();
            numStatements++;
        }
        blockMetrics.addNumCoveredStatements(covered);
        blockMetrics.addNumStatements(numStatements);

        // sum metrics from branches declared in a method body
        covered = 0;
        for (BranchInfo branchInfo : branches) {
            if (branchInfo.isFiltered(contextSet)) {
                continue;
            }
            if (branchInfo.getTrueHitCount() > 0) {
                covered++;
            }
            if (branchInfo.getFalseHitCount() > 0) {
                covered++;
            }
            complexity += branchInfo.getComplexity();
            numBranches += 2;
        }

        blockMetrics.addNumCoveredBranches(covered);
        blockMetrics.addNumBranches(numBranches);

        int totalComplexity = blockMetrics.getComplexity() + complexity;
        blockMetrics.setComplexity(totalComplexity);
        setComplexity(totalComplexity); // todo - duplication here - because a method is an Element and a HasMetricsNode
        return blockMetrics;
    }

    private void copyMethodInternals(FullMethodInfo targetMethod) {
        targetMethod.setDataProvider(getDataProvider());
        for (FullStatementInfo statementInfo : statements) {
            targetMethod.addStatement(statementInfo.copy(targetMethod));
        }
        for (FullBranchInfo branchInfo : branches) {
            targetMethod.addBranch(branchInfo.copy(targetMethod));
        }
        for (FullClassInfo classInfo : innerClasses) {
            targetMethod.addClass(classInfo);
        }
        for (FullMethodInfo methodInfo : innerMethods) {
            targetMethod.addMethod(methodInfo);
        }
        targetMethod.setDataLength(getDataLength());
    }

    public FullMethodInfo copy(FullClassInfo classAsParent) {
        FullMethodInfo method = new FullMethodInfo(classAsParent, getContext(), sharedInfo);
        copyMethodInternals(method);
        return method;
    }

    public FullMethodInfo copy(FullMethodInfo methodAsParent) {
        FullMethodInfo method = new FullMethodInfo(methodAsParent, getContext(), sharedInfo);
        copyMethodInternals(method);
        return method;
    }

    public FullMethodInfo copy(FullFileInfo fileAsParent) {
        FullMethodInfo method = new FullMethodInfo(fileAsParent, getContext(), sharedInfo);
        copyMethodInternals(method);
        return method;
    }

    /**
     * convenience method
     * @return true if public, false otherwise
     */
    public boolean isPublic() {
        return Modifier.isPublic(sharedInfo.getSignature().getBaseModifiersMask());
    }

     /**
     * convenience method
     * @return return either "public", "package", "protected" or "private"
     */
    public String getVisibility() {
        return sharedInfo.getSignature().getModifiers().getVisibility();
    }

    @Override
    public CoverageDataProvider getDataProvider() {
        return data;
    }

    @Override
    public int getDataLength() {
        return sharedInfo.getDataLength();
    }

    public void setDataLength(int length) {
        sharedInfo.setDataLength(length);
    }

    protected void setContainingClass(FullClassInfo classInfo) {
        parent = new ParentEntity(classInfo);
    }

    protected void setContainingMethod(FullMethodInfo methodInfo) {
        parent = new ParentEntity(methodInfo);
    }

    protected void setContainingFile(FullFileInfo fileInfo) {
        if (parent == null) {
            parent = new ParentEntity(fileInfo);
        } else {
            parent.setContainingFile(fileInfo);
        }
    }

    @Override
    @Nullable
    public FileInfo getContainingFile() {
        return parent.getContainingFile();
    }

    @Override
    @NotNull
    public EntityContainer getParent() {
        return parent.getParentEntity();
    }

    public void setRegionEnd(int endLine, int endCol) {
        final SourceInfo region = sharedInfo.getRegion();
        sharedInfo.setRegion(new FixedSourceRegion(region.getStartLine(), region.getStartColumn(), endLine, endCol));
    }

    public void setStaticTestName(String staticTestName) {
        sharedInfo.setStaticTestName(staticTestName);
    }

    public boolean isVoidReturnType() {
        return voidReturnType;
    }

    public void setVoidReturnType(boolean voidReturnType) {
        this.voidReturnType = voidReturnType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(TaggedDataOutput out) throws IOException {
        out.write(MethodSignature.class, sharedInfo.getSignature());
        out.writeUTF(getStaticTestName());
        out.writeBoolean(isTest());
        out.writeBoolean(isLambda());
        out.write(org.openclover.core.context.ContextSet.class, (org.openclover.core.context.ContextSet) context);
        out.writeInt(sharedInfo.getRelativeDataIndex());
        out.writeInt(getDataLength());
        out.writeInt(getComplexity());
        out.writeInt(getAggregatedComplexity());
        out.writeInt(getAggregatedStatementCount());
        out.writeUTF(sharedInfo.getConstruct().getId());
        FixedSourceRegion.writeRaw(this, out);

        // write statements, branches, classes and methods
        out.writeList(FullStatementInfo.class, statements);
        out.writeList(FullBranchInfo.class, branches);
        out.writeList(FullClassInfo.class, innerClasses);
        out.writeList(FullMethodInfo.class, innerMethods);
    }

    public static FullMethodInfo read(TaggedDataInput in) throws IOException {
        // read method's metadata
        final MethodSignature signature = in.read(MethodSignature.class);
        final String staticTestName = in.readUTF();
        final boolean isTest = in.readBoolean();
        final boolean isLambda = in.readBoolean();
        final ContextSet context = in.read(org.openclover.core.context.ContextSet.class);
        final int index = in.readInt();
        final int length = in.readInt();
        final int complexity = in.readInt();
        final int aggregatedComplexity = in.readInt();
        final int aggregatedStatements = in.readInt();
        final LanguageConstruct construct = Languages.lookupConstruct(in.readUTF());
        final FixedSourceRegion region = FixedSourceRegion.read(in);

        // read statements, branches, inner classes and inner methods
        final List<FullStatementInfo> statements = in.readList(FullStatementInfo.class);
        final List<FullBranchInfo> branches = in.readList(FullBranchInfo.class);
        final List<FullClassInfo> innerClasses = in.readList(FullClassInfo.class);
        final List<FullMethodInfo> innerMethods = in.readList(FullMethodInfo.class);

        // construct method object and attach sub-elements
        final FullMethodInfo methodInfo = new FullMethodInfo(
                signature, context, index, length, complexity, construct, region,
                isTest, staticTestName, isLambda,
                statements, branches, innerClasses, innerMethods);
        methodInfo.setAggregatedComplexity(aggregatedComplexity);
        methodInfo.setAggregatedStatementCount(aggregatedStatements);
        for (FullStatementInfo statement : statements) {
            statement.setContainingMethod(methodInfo);
        }
        for (FullBranchInfo branch : branches) {
            branch.setContainingMethod(methodInfo);
        }
        for (FullClassInfo innerClass : innerClasses) {
            innerClass.setContainingMethod(methodInfo);
        }
        for (FullMethodInfo innerMethod : innerMethods) {
            innerMethod.setContainingMethod(methodInfo);
        }

        return methodInfo;
    }

    @Override
    public String toString() {
        return "FullMethodInfo{"
                + "sharedInfo='" + sharedInfo + '\''
                + " aggregatedComplexity=" + aggregatedComplexity
                + " aggregatedStatementCount=" + aggregatedStatementCount
                + "} ";
    }

}
