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
import org.openclover.core.context.ContextSetImpl;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.core.lang.Languages;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.ElementVisitor;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.api.registry.HasMetricsNode;
import org.openclover.core.spi.lang.LanguageConstruct;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.openclover.core.util.Lists.newArrayList;


public class FullMethodInfo extends FullElementInfo<BasicElementInfo>
        implements HasAggregatedMetrics, HasMetricsNode, TaggedPersistent, MethodInfo {

    public static final int DEFAULT_METHOD_COMPLEXITY = 1;

    private final List<StatementInfo> statements = newArrayList();
    private final List<BranchInfo> branches = newArrayList();
    private final List<ClassInfo> innerClasses = newArrayList();
    private final List<MethodInfo> innerMethods = newArrayList();

    private int aggregatedStatementCount;  // calculated during instrumentation
    private int aggregatedComplexity;  // calculated during instrumentation
    private boolean voidReturnType;
    private int dataLength;

    private final MethodSignature signature;
    private final boolean isTest;
    private final boolean isLambda;

    /** Name of the method */
    private final String name;

    private transient BlockMetrics rawMetrics;
    private transient BlockMetrics metrics;
    private transient ContextSet contextFilter;
    private transient CoverageDataProvider data;
    private transient ParentEntity parent;

    /**
     * Name of the test associated with a method. Some test frameworks can declare a name of the test using annotations,
     * so that JUnit or other frameworks will use the test name and not the original method name in reporting.
     */
    @Nullable
    private String staticTestName;

    /**
     * For method-in-class
     */
    public FullMethodInfo(ClassInfo containingClass, MethodSignature signature, ContextSet context,
                          BasicElementInfo methodInfo, boolean isTest, @Nullable String staticTestName,
                          boolean isLambda) {
        this(signature, context, methodInfo, isTest, staticTestName, isLambda);
        this.parent = new ParentEntity(containingClass);
    }

    /**
     * For method-in-method
     */
    public FullMethodInfo(MethodInfo containingMethod, MethodSignature signature, ContextSet context,
                          BasicElementInfo methodInfo, boolean isTest, @Nullable String staticTestName,
                          boolean isLambda) {
        this(signature, context, methodInfo, isTest, staticTestName, isLambda);
        this.parent = new ParentEntity(containingMethod);
    }

    /**
     * For method-in-file
     */
    public FullMethodInfo(FileInfo containingFile, MethodSignature signature, ContextSet context,
                          BasicElementInfo methodInfo, boolean isTest, @Nullable String staticTestName,
                          boolean isLambda) {
        this(signature, context, methodInfo, isTest, staticTestName, isLambda);
        this.parent = new ParentEntity(containingFile);
    }

    private FullMethodInfo(MethodSignature signature, ContextSet context, BasicElementInfo elementInfo,
                           boolean isTest, @Nullable String staticTestName, boolean isLambda) {
        super(context, elementInfo);
        this.dataLength = 1;        // a method entry hit counter
        this.name = getNameFor(signature);
        this.signature = signature;
        this.isTest = isTest;
        this.staticTestName = staticTestName;
        this.isLambda = isLambda;
    }

    /**
     * Private constructor for data serialization
     */
    private FullMethodInfo(MethodSignature signature, ContextSet context,
                           int relativeDataIndex, int dataLength,
                           int complexity, LanguageConstruct construct, SourceInfo region,
                           boolean isTest, @Nullable String staticTestName, boolean isLambda,
                           List<StatementInfo> statements, List<BranchInfo> branches,
                           List<ClassInfo> classes, List<MethodInfo> methods) {
        this(signature, context,
                new BasicElementInfo(region, relativeDataIndex, complexity, construct),
                isTest, staticTestName, isLambda);
        this.dataLength = dataLength;
        this.statements.addAll(statements);
        this.branches.addAll(branches);
        this.innerClasses.addAll(classes);
        this.innerMethods.addAll(methods);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSimpleName() {
        return signature.getName();
    }

    @Override
    @NotNull
    public MethodSignature getSignature() {
        return signature;
    }

    @Override
    public String getQualifiedName() {
        final String prefix =
                (parent.getContainingClass() != null ? parent.getContainingClass().getQualifiedName()
                        : (parent.getContainingMethod() != null ? parent.getContainingMethod().getQualifiedName()
                                : ""));
        return prefix + "." + signature.getName();
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
       return isTest;
    }

    @Override
    @Nullable
    public String getStaticTestName() {
        return staticTestName;
    }

    @Override
    public boolean isLambda() {
        return isLambda;
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

    @Override
    public void addClass(ClassInfo classInfo) {
        innerClasses.add(classInfo);
    }

    @Override
    public void addMethod(MethodInfo methodInfo) {
        innerMethods.add(methodInfo);
    }

    /**
     * {@inheritDoc}
     *
     * @return List&lt;ClassInfo&gt; - a cloned list
     */
    @Override
    @NotNull
    public List<ClassInfo> getClasses() {
        return newArrayList(innerClasses); // copy
    }

    @Override
    @NotNull
    public List<ClassInfo> getAllClasses() {
        final List<ClassInfo> allClasses = newArrayList();
        // in-order
        for (ClassInfo classInfo : innerClasses) {
            allClasses.add(classInfo);
            allClasses.addAll(classInfo.getAllClasses());
        }
        for (MethodInfo methodInfo : innerMethods) {
            allClasses.addAll(methodInfo.getAllClasses());
        }
        return allClasses;
    }

    /**
     * {@inheritDoc}
     *
     * @return List&lt;MethodInfo&gt; - a cloned list
     */
    @Override
    @NotNull
    public List<MethodInfo> getMethods() {
        return newArrayList(innerMethods); // copy
    }

    @Override
    @NotNull
    public List<MethodInfo> getAllMethods() {
        final List<MethodInfo> allMethods = newArrayList();
        // in-order
        for (MethodInfo methodInfo : innerMethods) {
            allMethods.add(methodInfo);
            allMethods.addAll(methodInfo.getAllMethods());
        }
        for (ClassInfo classInfo : innerClasses) {
            allMethods.addAll(classInfo.getAllMethods());
        }
        return allMethods;
    }

    // statements and branches

    @Override
    public void addStatement(StatementInfo stmt) {
        statements.add(stmt);
    }

    @Override
    public void addBranch(BranchInfo branch) {
        branches.add(branch);
    }

    @Override
    @NotNull
    public List<StatementInfo> getStatements() {
        return newArrayList(statements); // clone
    }
    
    @Override
    @NotNull
    public List<BranchInfo> getBranches() {
        return newArrayList(branches); // copy
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
        for (MethodInfo methodInfo : innerMethods) {
            methodInfo.setDataProvider(data);
        }
        for (ClassInfo classInfo : innerClasses) {
            classInfo.setDataProvider(data);
        }
        // note: don't call setDataProvider on 'statements' because FullStatementInfo takes provider form its parent
        rawMetrics = null;
        metrics = null;
    }

    @Override
    public void gatherSourceRegions(Set<SourceInfo> regions) {
        // this method and its code
        regions.add(this);
        regions.addAll(statements);
        regions.addAll(branches);
        // inline classes
        for (ClassInfo classInfo : innerClasses) {
            classInfo.gatherSourceRegions(regions);
        }
        // inner functions
        for (MethodInfo methodInfo : innerMethods) {
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

    @Override
    public void visitElements(ElementVisitor visitor) {
        // this method
        visitor.visitMethod(this);
        // this method's statements and branches
        for (StatementInfo statementInfo : statements) {
            visitor.visitStatement(statementInfo);
        }
        for (BranchInfo branchInfo : branches) {
            visitor.visitBranch(branchInfo);
        }
        // inline classes
        for (ClassInfo classInfo : innerClasses) {
            classInfo.visitElements(visitor);
        }
        // inner functions
        for (MethodInfo methodInfo : innerMethods) {
            methodInfo.visitElements(visitor);
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
        for (ClassInfo classInfo : innerClasses) {
            blockMetrics.add(classInfo.getMetrics());
        }

        // sum metrics from inner methods
        for (MethodInfo methodInfo : innerMethods) {
            blockMetrics.add(methodInfo.getMetrics());
        }

        // sum metrics from statements declared in a method body
        for (StatementInfo statementInfo : statements) {
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

    private void copyMethodInternals(MethodInfo targetMethod) {
        targetMethod.setDataProvider(getDataProvider());
        for (StatementInfo statementInfo : statements) {
            targetMethod.addStatement(statementInfo.copy(targetMethod));
        }
        for (BranchInfo branchInfo : branches) {
            targetMethod.addBranch(branchInfo.copy(targetMethod));
        }
        for (ClassInfo classInfo : innerClasses) {
            targetMethod.addClass(classInfo);
        }
        for (MethodInfo methodInfo : innerMethods) {
            targetMethod.addMethod(methodInfo);
        }
        targetMethod.setDataLength(getDataLength());
    }

    @Override
    public MethodInfo copy(ClassInfo classAsParent) {
        MethodInfo method = new FullMethodInfo(classAsParent, signature, getContext(), sharedInfo,
                isTest, staticTestName, isLambda);
        copyMethodInternals(method);
        return method;
    }

    @Override
    public MethodInfo copy(MethodInfo methodAsParent) {
        MethodInfo method = new FullMethodInfo(methodAsParent, signature, getContext(), sharedInfo,
                isTest, staticTestName, isLambda);
        copyMethodInternals(method);
        return method;
    }

    @Override
    public MethodInfo copy(FileInfo fileAsParent) {
        MethodInfo method = new FullMethodInfo(fileAsParent, signature, getContext(), sharedInfo,
                isTest, staticTestName, isLambda);
        copyMethodInternals(method);
        return method;
    }

    /**
     * convenience method
     * @return true if public, false otherwise
     */
    @Override
    public boolean isPublic() {
        return Modifier.isPublic(signature.getBaseModifiersMask());
    }

    @Override
    public String getVisibility() {
        return signature.getModifiers().getVisibility();
    }

    @Override
    public CoverageDataProvider getDataProvider() {
        return data;
    }

    @Override
    public int getDataLength() {
        return dataLength;
    }

    @Override
    public void setDataLength(int length) {
        this.dataLength = length;
    }

    @Override
    public void setContainingClass(ClassInfo classInfo) {
        parent = new ParentEntity(classInfo);
    }

    @Override
    public void setContainingMethod(MethodInfo methodInfo) {
        parent = new ParentEntity(methodInfo);
    }

    @Override
    public void setContainingFile(FileInfo fileInfo) {
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

    public void setStaticTestName(@Nullable String testName) {
        this.staticTestName = testName;
    }

    public boolean isVoidReturnType() {
        return voidReturnType;
    }

    public void setVoidReturnType(boolean voidReturnType) {
        this.voidReturnType = voidReturnType;
    }

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.write(MethodSignature.class, signature);
        out.writeUTF(getStaticTestName());
        out.writeBoolean(isTest());
        out.writeBoolean(isLambda());
        out.write(ContextSetImpl.class, (ContextSetImpl) context);
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
        final ContextSet context = in.read(ContextSetImpl.class);
        final int index = in.readInt();
        final int length = in.readInt();
        final int complexity = in.readInt();
        final int aggregatedComplexity = in.readInt();
        final int aggregatedStatements = in.readInt();
        final LanguageConstruct construct = Languages.lookupConstruct(in.readUTF());
        final FixedSourceRegion region = FixedSourceRegion.read(in);

        // read statements, branches, inner classes and inner methods
        final List<StatementInfo> statements = in.readList(FullStatementInfo.class);
        final List<BranchInfo> branches = in.readList(FullBranchInfo.class);
        final List<ClassInfo> innerClasses = in.readList(FullClassInfo.class);
        final List<MethodInfo> innerMethods = in.readList(FullMethodInfo.class);

        // construct method object and attach sub-elements
        final FullMethodInfo methodInfo = new FullMethodInfo(
                signature, context, index, length, complexity, construct, region,
                isTest, staticTestName, isLambda,
                statements, branches, innerClasses, innerMethods);
        methodInfo.setAggregatedComplexity(aggregatedComplexity);
        methodInfo.setAggregatedStatementCount(aggregatedStatements);
        for (StatementInfo statement : statements) {
            statement.setContainingMethod(methodInfo);
        }
        for (BranchInfo branch : branches) {
            branch.setContainingMethod(methodInfo);
        }
        for (ClassInfo innerClass : innerClasses) {
            innerClass.setContainingMethod(methodInfo);
        }
        for (MethodInfo innerMethod : innerMethods) {
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

    private static String getNameFor(MethodSignature signature) {
        return signature.getName() + "("+signature.listParamTypes()+")" +
                ((signature.getReturnType() != null && signature.getReturnType().length() > 0) ? " : " + signature.getReturnType() : "");
    }

}
