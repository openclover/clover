package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.TCILookupStore;
import org.openclover.core.TestCaseInfoLookup;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ClassMetadata;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.EntityContainer;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.ModifiersInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.api.registry.StatementInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.CoverageDataReceptor;
import org.openclover.core.registry.FileElementVisitor;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.metrics.ClassMetrics;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.registry.metrics.HasMetricsNode;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.openclover.core.util.Lists.newArrayList;


public class FullClassInfo
        implements HasMetricsNode, CoverageDataReceptor, TaggedPersistent, ClassInfo {

    private final FullClassMetadata classMetadata;
    private SourceInfo region;
    private FileInfo containingFile;

    /**
     * List of methods declared in the class
     */
    private List<FullMethodInfo> methods = newArrayList();

    /**
     * List of classes declared inside the class. Unused currently. Could be used for inner or inline classes; inline
     * classes could be either inside a method or assigned to a class' field.
     */
    private List<FullClassInfo> classes = newArrayList();

    /**
     * List of statements declared in the class - could used for initializer blocks for instance (unused currently)
     */
    private List<FullStatementInfo> statements = newArrayList();

    /**
     * List of branches declared in the class - could used for initializer blocks for instance (unused currently)
     */
    // TODO handle it in: copying, metrics, read, write, add, get
    private List<FullBranchInfo> branches = newArrayList();

    private int relativeDataIndex;
    private int dataLength;
    private int aggregatedStatementCount;
    private int aggregatedComplexity;

    private transient CoverageDataProvider data;
    private transient TestCaseInfoLookup tciLookup;
    private transient ParentEntity parent;


    public FullClassInfo(final PackageInfo packageInfo, final FullClassInfo parentClass,
                         final int dataIndex, final String name, final SourceInfo region, final Modifiers modifiers,
                         final boolean typeInterface, final boolean typeEnum, final boolean typeAnnotation) {
        this.parent = new ParentEntity(parentClass);
        this.classMetadata = new FullClassMetadata(packageInfo, name, modifiers,
                typeInterface, typeEnum, typeAnnotation);
        this.region = region;
        this.containingFile = parentClass.getContainingFile();
    }

    public FullClassInfo(final PackageInfo packageInfo, final MethodInfo parentMethod,
                         final int dataIndex, final String name, final SourceInfo region, final Modifiers modifiers,
                         final boolean typeInterface, final boolean typeEnum, final boolean typeAnnotation) {
        this(new ParentEntity(parentMethod), packageInfo, dataIndex,
                name, region, modifiers,
                typeInterface, typeEnum, typeAnnotation);
    }

    public FullClassInfo(final PackageInfo packageInfo, final FileInfo parentFile,
                         final int dataIndex, final String name, final SourceInfo region, final Modifiers modifiers,
                         final boolean typeInterface, final boolean typeEnum, final boolean typeAnnotation) {
        this(new ParentEntity(parentFile), packageInfo, dataIndex,
                name, region, modifiers,
                typeInterface, typeEnum, typeAnnotation);
    }

    private FullClassInfo(final ParentEntity parent, final PackageInfo packageInfo,
                          final int dataIndex, final String name, final SourceInfo region, final Modifiers modifiers,
                          final boolean typeInterface, final boolean typeEnum, final boolean typeAnnotation) {
        this.parent = parent;
        this.classMetadata = new FullClassMetadata(packageInfo, name, modifiers,
                typeInterface, typeEnum, typeAnnotation);
        this.region = region;
        this.containingFile = parent.getContainingFile();
        this.relativeDataIndex = dataIndex;
    }

    /**
     * Constructor used for data de-serialization.
     */
    private FullClassInfo(final String name, final String qualifiedName,
                          final int dataIndex, final int dataLength,
                          final boolean typeInterface, final boolean typeEnum, final boolean typeAnnotation, boolean testClass,
                          final SourceInfo region,
                          final Modifiers modifiers,
                          final List<FullMethodInfo> methods,
                          final List<FullClassInfo> classes,
                          final List<FullStatementInfo> statements) {
        this.classMetadata = new FullClassMetadata(name, qualifiedName, modifiers, typeInterface, typeEnum, typeAnnotation, testClass);
        this.region = region;
        this.relativeDataIndex = dataIndex;
        this.dataLength = dataLength;
        this.methods = methods;
        this.classes = classes;
        this.statements = statements;
    }

    // ClassInfo

    @Override
    public String getQualifiedName() {
        return classMetadata.getQualifiedName();
    }

    @Override
    public ModifiersInfo getModifiers() {
        return classMetadata.getModifiers();
    }

    @Override
    public boolean isInterface() {
        return classMetadata.isInterface();
    }

    @Override
    public boolean isEnum() {
        return classMetadata.isEnum();
    }

    @Override
    public boolean isAnnotationType() {
        return classMetadata.isAnnotationType();
    }

    @Override
    public boolean isTestClass() {
        return classMetadata.isTestClass();
    }

    @Override
    public PackageInfo getPackage() {
        return classMetadata.getPackage();
    }

    // CoverageDataRecorder

    @Override
    public void setDataProvider(final CoverageDataProvider data) {
        this.data = data;
        this.tciLookup =
                data instanceof TCILookupStore ? ((TCILookupStore)data).namedTCILookupFor(calcTCILookupName()) : null;
        for (MethodInfo methodInfo : methods) {
            ((FullMethodInfo) methodInfo).setDataProvider(data);
        }
        for (FullClassInfo classInfo : classes) {
            classInfo.setDataProvider(data);
        }
        // note: don't call setDataProvider on 'statements' because FullStatementInfo takes provider form its parent
        classMetadata.rawMetrics = null;
        classMetadata.metrics = null;
    }

    @Override
    public CoverageDataProvider getDataProvider() {
        return data;
    }

    // HasClasses

    @Override
    @NotNull
    public List<ClassInfo> getClasses() {
        return newArrayList(classes); // copy
    }

    @Override
    @NotNull
    public List<ClassInfo> getAllClasses() {
        final List<ClassInfo> allClasses = newArrayList();
        // in-order
        for (FullClassInfo classInfo : classes) {
            allClasses.add(classInfo);
            allClasses.addAll(classInfo.getAllClasses());
        }
        for (MethodInfo methodInfo : methods) {
            allClasses.addAll(methodInfo.getAllClasses());
        }
        return allClasses;
    }

    // HasMethods

    @Override
    @NotNull
    public List<MethodInfo> getMethods() {
        return newArrayList(methods); // copy
    }

    @Override
    @NotNull
    public List<MethodInfo> getAllMethods() {
        final List<MethodInfo> allMethods = newArrayList();
        // in-order
        for (MethodInfo methodInfo : methods) {
            allMethods.add(methodInfo);
            allMethods.addAll(methodInfo.getAllMethods());
        }
        for (FullClassInfo classInfo : classes) {
            allMethods.addAll(classInfo.getAllMethods());
        }
        return allMethods;
    }

    // HasMetrics

    @Override
    public String getName() {
        return classMetadata.getName();
    }

    @Override
    public BlockMetrics getMetrics() {
        // TODO getPackage() or parent.getParentEntity() ?
        if (classMetadata.metrics == null  || getPackage().getContextFilter() != classMetadata.contextFilter) {
            classMetadata.contextFilter = getContainingFile().getContextFilter();
            classMetadata.metrics = calcMetrics(classMetadata.contextFilter, true);
        }
        return classMetadata.metrics;
    }

    @Override
    public BlockMetrics getRawMetrics() {
        if (classMetadata.rawMetrics == null) {
            classMetadata.rawMetrics = calcMetrics(null, false);
        }
        return classMetadata.rawMetrics;
    }

    @Override
    public void setMetrics(BlockMetrics metrics) {
        classMetadata.setMetrics(metrics);
    }

    // HasMetricsNode

    @Override
    public boolean isEmpty() {
        return methods.size() == 0;
    }

    @Override
    public int getNumChildren() {
        return methods.size();
    }

    @Override
    public HasMetricsNode getChild(int i) {
        return (FullMethodInfo) methods.get(i);
    }

    @Override
    public int getIndexOfChild(HasMetricsNode child) {
        return methods.indexOf(child);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }


    // HasStatements

    @Override
    @NotNull
    public List<StatementInfo> getStatements() {
        return newArrayList(statements); // copy
    }

    // TaggedPersistent

    @Override
    @SuppressWarnings("unchecked")
    public void write(TaggedDataOutput out) throws IOException {
        // write class metadata
        out.writeUTF(classMetadata.name);
        out.writeUTF(classMetadata.qualifiedName);
        out.writeInt(relativeDataIndex);
        out.writeInt(dataLength);
        out.writeInt(aggregatedComplexity);
        out.writeInt(aggregatedStatementCount);
        out.writeBoolean(classMetadata.typeAnnotation);
        out.writeBoolean(classMetadata.typeEnum);
        out.writeBoolean(classMetadata.typeInterface);
        out.writeBoolean(classMetadata.testClass);
        FixedSourceRegion.writeRaw(this, out);
        out.write(Modifiers.class, classMetadata.modifiers);

        // write methods and statements
        out.writeList(FullClassInfo.class, classes);
        out.writeList(FullMethodInfo.class, methods);
        out.writeList(FullStatementInfo.class, statements);
    }

    // OTHER

    public void addClass(FullClassInfo classInfo) {
        classes.add(classInfo);
    }

    public void addMethod(FullMethodInfo meth) {
        methods.add(meth);
        classMetadata.testClass |= meth.isTest();
    }

    public void addStatement(FullStatementInfo statement) {
        statements.add(statement);
    }

    public void addTestCase(TestCaseInfo tci) {
        final TestCaseInfoLookup lookup = this.tciLookup;
        if (lookup != null) {
            lookup.add(tci);
        }
        classMetadata.testClass = true;
    }


    public Collection<TestCaseInfo> getTestCases() {
        final TestCaseInfoLookup lookup = tciLookup;
        if (lookup != null) {
            return lookup.getTestCaseInfos();
        }
        return Collections.emptySet();
    }

    /**
     * Gets the runtime testcase for the given id of this class.
     * @param id numeric identifier of the test case
     * @return TestCaseInfo or <pre>null</pre> if not found
     */
    public TestCaseInfo getTestCase(Integer id) {
        final TestCaseInfoLookup lookup = tciLookup;
        if (lookup != null) {
            return lookup.getBy(id);
        }
        return null;
    }

    /**
     *
     * @param testname - fully qualified test name e.g. com.foo.bar.BarTest.testFoo
     * @return testCaseInfo for the given test name, or null if not found or no test case info
     */
    public TestCaseInfo getTestCase(String testname) {
        final TestCaseInfoLookup lookup = tciLookup;
        if (lookup != null) {
            return lookup.getBy(testname);
        }
        return null;
    }


    @Override
    public int getDataIndex() {
        return containingFile.getDataIndex() + relativeDataIndex;
    }

    public int getRelativeDataIndex() {
        return relativeDataIndex;
    }

    @Override
    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int length) {
        dataLength = length;
    }

    @Override
    public int getAggregatedStatementCount() {
        return aggregatedStatementCount;
    }

    @Override
    public void setAggregatedStatementCount(int aggregatedStatements) {
        this.aggregatedStatementCount = aggregatedStatements;
    }

    public void increaseAggregatedStatements(int increment) {
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

    @Override
    @Nullable
    public MethodInfo getContainingMethod() {
        return parent.getContainingMethod();
    }

    @Override
    @Nullable
    public ClassInfo getContainingClass() {
        return parent.getContainingClass();
    }

    @Override
    @Nullable
    public FileInfo getContainingFile() {
        return parent.getContainingFile();
    }

    @Override
    public EntityContainer getParent() {
        return parent.getParentEntity();
    }

    public void gatherSourceRegions(Set<SourceInfo> regions) {
        regions.add(this);
        for (FullMethodInfo methodInfo : methods) {
            methodInfo.gatherSourceRegions(regions);
        }
    }

    public void visitElements(FileElementVisitor visitor) {
        // this class
        visitor.visitClass(this);
        // inner classes
        for (FullClassInfo classInfo : classes) {
            classInfo.visitElements(visitor);
        }
        // this class methods
        for (FullMethodInfo methodInfo : methods) {
            methodInfo.visit(visitor);
        }
        // this class statements
        for (FullStatementInfo statementInfo : statements) {
            visitor.visitStatement(statementInfo);
        }
    }

    @Override
    public String getChildType() {
        return "method";
    }




    @Override
    public void setComparator(Comparator<HasMetrics> cmp) {
        if (cmp != null) {
            methods.sort(cmp);
        } else {
            methods.sort(FixedSourceRegion.SOURCE_ORDER_COMP);
        }
    }


    private String calcTCILookupName() {
        return "class@" + getDataIndex() + ":" + getQualifiedName();
    }

    private ClassMetrics calcMetrics(ContextSet filter, boolean isFiltered) {
        final ClassMetrics classMetrics = new ClassMetrics(this);

        calcAndAddMethodMetrics(classMetrics, filter, isFiltered);
        calcAndAddClassMetrics(classMetrics, isFiltered); // inner classes
        calcAndAddBranchMetrics(classMetrics, filter);
        calcAndAddStatementMetrics(classMetrics, filter);
        calcAndAddTestCaseMetrics(classMetrics);

        return classMetrics;
    }

    private void calcAndAddMethodMetrics(ClassMetrics classMetrics, ContextSet filter, boolean isFiltered) {
        int covered = 0;
        int numMethods = 0;
        int numTestMethods = 0;
        for (FullMethodInfo methodInfo : methods) {
            if (methodInfo.isFiltered(filter)) {
                continue;
            }
            if (!isFiltered) {
                classMetrics.add(methodInfo.getRawMetrics());
            } else {
                classMetrics.add(methodInfo.getMetrics());
            }

            // count covered methods - the current one and inner ones
            for (MethodInfo innerMethod : methodInfo.getAllMethods()) {
                if (innerMethod.getHitCount() > 0) {
                    covered++;
                }
            }
            if (methodInfo.getHitCount() > 0) {
                covered++;                        // TODO
            }

            // count test methods (assuming that inner methods are not test methods)
            if (methodInfo.isTest()) {
                numTestMethods++;                 // TODO
            }

            // count total number of methods
            numMethods += 1 + methodInfo.getAllMethods().size(); // current method + all inner methods
        }
        classMetrics.addNumMethods(numMethods);
        classMetrics.addNumCoveredMethods(covered);
        classMetrics.addNumTestMethods(numTestMethods);
    }

    private void calcAndAddClassMetrics(ClassMetrics classMetrics, boolean isFiltered) {
        // sum metrics from inner classes
        for (FullClassInfo classInfo : classes) {
            if (!isFiltered) {
                classMetrics.add(classInfo.getRawMetrics());
            } else {
                classMetrics.add(classInfo.getMetrics());
            }
        }
    }

    private void calcAndAddBranchMetrics(ClassMetrics classMetrics, ContextSet contextSet) {
        int covered = 0;
        int numBranches = 0;
        int complexity = 0;

        // sum metrics from branches declared in a class body
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

        classMetrics.addNumCoveredBranches(covered);
        classMetrics.addNumBranches(numBranches);
        classMetrics.addComplexity(complexity);
    }

    private void calcAndAddStatementMetrics(ClassMetrics classMetrics, ContextSet contextSet) {
        int covered = 0;
        int numStatements = 0;
        int complexity = 0;

        // sum metrics from statements declared in a class body
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
        classMetrics.addNumCoveredStatements(covered);
        classMetrics.addNumStatements(numStatements);
        classMetrics.addComplexity(complexity);
    }

    private void calcAndAddTestCaseMetrics(ClassMetrics classMetrics) {
        final TestCaseInfoLookup lookup = this.tciLookup;
        if (lookup != null) {
            int numTests = 0;
            int numPasses = 0;
            int numFailures = 0;
            int numErrors = 0;
            double executionTime = 0.0;
            for (TestCaseInfo test : lookup.getTestCaseInfos()) {
                if (test.isHasResult()) {
                    numTests++;
                    if (test.isSuccess()) {
                        numPasses++;
                    } else {
                        if (test.isError()) {
                            numErrors++;
                        } else {
                            numFailures++;
                        }
                    }
                    executionTime += test.getDuration();
                }
            }
            classMetrics.setNumTests(numTests);
            classMetrics.setNumTestPasses(numPasses);
            classMetrics.setNumTestFailures(numFailures);
            classMetrics.setNumTestErrors(numErrors);
            classMetrics.setTestExecutionTime(executionTime);
        }
    }

    public FullClassInfo copy(FullFileInfo newParent, HasMetricsFilter filter) {
        final FullClassInfo newClass = new FullClassInfo(
                newParent.getContainingPackage(), newParent,
                relativeDataIndex, classMetadata.name, this, classMetadata.modifiers,
                classMetadata.typeInterface, classMetadata.typeEnum, classMetadata.typeAnnotation);
        newClass.setDataProvider(getDataProvider());
        newClass.setDataLength(getDataLength());

        for (FullClassInfo classInfo : classes) {
            if (filter.accept(classInfo)) {
                newClass.addClass(classInfo);
            }
        }
        for (FullMethodInfo methodInfo : methods) {
            if (filter.accept(methodInfo)) {
                newClass.addMethod(methodInfo.copy(newClass));
            }
        }
        for (FullStatementInfo statementInfo : statements) {
            // note that statements are not filtered
            newClass.addStatement(statementInfo.copy(newClass));
        }

        return newClass;
    }

    /**
     * find a method decl from its name
     * at the moment this method just returns the first method that has the requested name, or null
     *
     * @return  FullMethodInfo or null if not found
     */
    public FullMethodInfo getTestMethodDeclaration(String methodname) {
        for (FullMethodInfo methodInfo : methods) {
            if (methodInfo.getSimpleName().equals(methodname) && methodInfo.isPublic()) {
                return methodInfo;
            }
        }
        //TODO - needs to also search any runtime data of inherited methods
        return null;
    }

    public int getNumMethods() {
        return methods.size();
    }

    public void setRegionEnd(int endLine, int endCol) {
        region = new FixedSourceRegion(region.getStartLine(), region.getStartColumn(), endLine, endCol);
    }

    public void setContainingMethod(FullMethodInfo containingMethod) {
        parent = new ParentEntity(containingMethod);
    }

    public void setContainingClass(FullClassInfo containingClass) {
        parent = new ParentEntity(containingClass);
    }

    public void setContainingFile(FullFileInfo fileInfo) {
        // TODO yuck; double field; one is from FixedFileRegion
        this.containingFile = fileInfo;
        if (parent == null) {
            parent = new ParentEntity(fileInfo);
        } else {
            parent.setContainingFile(fileInfo);
        }
    }


    public static FullClassInfo read(TaggedDataInput in) throws IOException {
        // read class metadata
        final String name = in.readUTF();
        final String qualifiedName = in.readUTF();
        final int index = in.readInt();
        final int length = in.readInt();
        final int aggregatedComplexity = in.readInt();
        final int aggregatedStatements = in.readInt();
        final boolean typeAnnotation = in.readBoolean();
        final boolean typeEnum = in.readBoolean();
        final boolean typeInterface = in.readBoolean();
        final boolean isTest = in.readBoolean();
        final FixedSourceRegion region = FixedSourceRegion.read(in);
        final Modifiers modifiers = in.read(Modifiers.class);

        // read methods and statements
        final List<FullClassInfo> classes = in.readList(FullClassInfo.class);
        final List<FullMethodInfo> methods = in.readList(FullMethodInfo.class);
        final List<FullStatementInfo> statements = in.readList(FullStatementInfo.class);

        // instantiate object and attach methods and statements to it
        final FullClassInfo classInfo = new FullClassInfo(name, qualifiedName,
                index, length, typeInterface, typeEnum, typeAnnotation, isTest,
                region, modifiers, methods, classes, statements);
        classInfo.setAggregatedStatementCount(aggregatedStatements);
        classInfo.setAggregatedComplexity(aggregatedComplexity);
        for (FullClassInfo cls : classes) {
            cls.setContainingClass(classInfo);
        }
        for (FullMethodInfo method : methods) {
            method.setContainingClass(classInfo);
        }
        for (FullStatementInfo statement : statements) {
            statement.setContainingClass(classInfo);
        }
        return classInfo;
    }

    @Override
    public void visit(EntityVisitor entityVisitor) {
        entityVisitor.visitClass(this);
    }

    @Override
    public ContextSet getContextFilter() {
        return containingFile.getContextFilter();
    }

    @Override
    public int getStartLine() {
        return region.getStartLine();
    }

    @Override
    public int getStartColumn() {
        return region.getStartColumn();
    }

    @Override
    public int getEndLine() {
        return region.getEndLine();
    }

    @Override
    public int getEndColumn() {
        return region.getEndColumn();
    }

    @Override
    public ClassMetadata getClassMetadata() {
        return classMetadata;
    }
}
