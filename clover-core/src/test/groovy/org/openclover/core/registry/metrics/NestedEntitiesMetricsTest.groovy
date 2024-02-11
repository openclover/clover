package org.openclover.core.registry.metrics

import org.openclover.core.api.registry.BranchInfo
import org.openclover.core.context.ContextSet
import org.openclover.core.context.ContextStore
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.registry.entities.FullStatementInfo
import org.junit.Before
import org.junit.Test

import static org.openclover.core.registry.metrics.MetricsHelper.assertMetricsEquals
import static org.junit.Assert.assertEquals

/**
 * Test of metrics for entities nested in each other
 */
public class NestedEntitiesMetricsTest {
    private HasMetricsTestFixture fixture

    private BranchInfo branchInfo
    private FullStatementInfo stmtInfo
    private FullClassInfo classInfo
    private FullMethodInfo methodInfo
    private FullMethodInfo subMethodInfo
    private FullFileInfo fileInfo

    /**
     * Sets up a FullProjectInfo model object containing the following:
     *
     * <pre>
     * + ProjectInfo
     *   + PackageInfo - "testpkg"
     *     + FileInfo - "testpkg.Test.java"
     *       + ClassInfo - "testpkg.Test"
     *         + MethodInfo - "testpkg.Test#method1"
     *           + StatementInfo - "testpkg.Test#method1 { cmp = 1, startline = 3, hitCount = 0, context = "assert"; }"
     *           + BranchInfo - "testpkg.Test#method1    { cmp = 2, startline = 4, hitCount = 1, context = "if"; }"
     * </pre>
     *
     * @throws java.io.IOException
     */
    @Before
    void setUp() throws IOException {
        fixture = new HasMetricsTestFixture(this.getClass().getName())
        fileInfo = fixture.defaultFileInfo
        //class on line1
        classInfo = fixture.newClass("Test", 1)
        // method on line2 => 1 statement, 1 double branch => complexity = 4
        methodInfo = fixture.newMethod(classInfo, "method1", 2)
        // statement on line3, cmp = 1, hitcoutnt = 0
        stmtInfo = fixture.addStatement(methodInfo, 1, 3, 0)
        stmtInfo.setContext(new ContextSet().set(ContextStore.CONTEXT_ASSERT))
        // branch on line4,
        branchInfo = fixture.addBranch(methodInfo, new ContextSet().set(ContextStore.CONTEXT_IF), 4, 1)
    }

    /**
     * Test whether method complexity takes into account a nested method too. Test enhances existing methodInfo by
     * new entities:
     * <pre>
     * method1
     *  + MethodInfo "submethod2"
     *    + StatementInfo
     *    + StatementInfo
     *    + StatementInfo
     *    + BranchInfo
     *    + BranchInfo
     * </pre>
     */
    @Test
    void testMethodMetricsWithSubMethod() {
        BlockMetrics expectedMetrics = new BlockMetrics(null)

        // check method without a sub-method
        MetricsHelper.setBlockMetrics(expectedMetrics, 1, 0, 2, 2, 4, 0, 0, 0, 0, 0.0f); // method1
        assertEquals(0, methodInfo.getMethods().size())
        assertMetricsEquals(expectedMetrics, methodInfo.getMetrics())
        methodInfo.setMetrics(null); // reset metrics

        // submethod, 3 statements, 2 branches => complexity = 7
        subMethodInfo = fixture.newMethod(methodInfo, "submethod2", FullMethodInfo.DEFAULT_METHOD_COMPLEXITY)
        fixture.addStatement(subMethodInfo, 1, 6, 0)
        fixture.addStatement(subMethodInfo, 1, 7, 0)
        fixture.addStatement(subMethodInfo, 1, 8, 0)
        fixture.addBranch(subMethodInfo, new ContextSet().set(ContextStore.CONTEXT_IF), 6, 0)
        fixture.addBranch(subMethodInfo, new ContextSet().set(ContextStore.CONTEXT_IF), 7, 0)

        MetricsHelper.setBlockMetrics(expectedMetrics, 1 + 3, 0, 2 + 4, 2, 4 + 8, 0, 0, 0, 0, 0.0f); // method1 + submethod2
        assertEquals(1, methodInfo.getMethods().size())
        assertMetricsEquals(expectedMetrics, methodInfo.getMetrics())
    }

    /**
     * Test whether method complexity takes into account a nested class too. Test enhances existing methodInfo by
     * new entities:
     * <pre>
     *  method1
     *  + ClassInfo "SubClassOne"
     *  + ClassInfo "SubClassTwo"
     * </pre>
     */
    @Test
    void testMethodMetricsWithSubClasses() {
        // method has two sub-classes
        FullClassInfo subClass1 = fixture.newClass("SubClassOne", 10)
        subClass1.setMetrics(MetricsHelper.setBlockMetrics(
                new BlockMetrics(null), 11, 1, 22, 6, 44, 5, 3, 0, 2, 0.25f))
        FullClassInfo subClass2 = fixture.newClass("SubClassTwo", 10)
        subClass2.setMetrics(MetricsHelper.setBlockMetrics(
                new BlockMetrics(null), 5, 5, 8, 4, 31, 9, 6, 2, 1, 1.5f))

        methodInfo.addClass(subClass1)
        methodInfo.addClass(subClass2)

        BlockMetrics expectedMetrics = MetricsHelper.setBlockMetrics(   // method1 + subclass1 + subclass2
                new BlockMetrics(null),
                1 + 11 + 5, 1 + 5, 2 + 22 + 8, 2 + 6 + 4, 4 + 44 + 31,
                5 + 9, 3 + 6, 2, 2 + 1, 1.75f)
        assertEquals(2, methodInfo.getClasses().size())
        assertMetricsEquals(expectedMetrics, methodInfo.getMetrics())
    }

    /**
     * Test whether class metrics take into account inner classes too. Test enhances existing classInfo by
     * new entities:
     * <pre>
     *  classInfo
     *  + ClassInfo "SubClass"
     *    + ClassInfo "SubSubClassOne"
     *    + ClassInfo "SubSubClassTwo"
     * </pre>
     */
    @Test
    void testClassMetricsWithSubClasses() {
        // class has three sub-classes on two levels of nesting
        FullClassInfo subClass = fixture.newClass(classInfo, "SubClass", 10)
        // don't set metrics for subClass, we will aggregate from two inner classes
        FullClassInfo subClass1 = fixture.newClass(subClass, "SubSubClassOne", 10)
        subClass1.setMetrics(MetricsHelper.setBlockMetrics(
                new BlockMetrics(null), 11, 1, 22, 6, 44, 5, 3, 0, 2, 0.25f))
        FullClassInfo subClass2 = fixture.newClass(subClass, "SubSubClassTwo", 10)
        subClass2.setMetrics(MetricsHelper.setBlockMetrics(
                new BlockMetrics(null), 5, 5, 8, 4, 31, 9, 6, 2, 1, 1.5f))

        ClassMetrics expectedMetrics = MetricsHelper.setClassMetrics(   // classInfo + subclass + subclass1 + subclass2
                new ClassMetrics(null),
                1 + 11 + 5, 1 + 5, 2 + 22 + 8, 2 + 6 + 4, 4 + 44 + 31,
                5 + 9, 3 + 6, 2, 2 + 1, 1.75f, 1, 0)
        assertEquals(1, classInfo.getClasses().size()); // one direct subclass
        assertEquals(3, classInfo.getAllClasses().size()); // three subclasses in total
        assertMetricsEquals(expectedMetrics, (ClassMetrics)classInfo.getMetrics())
    }

    /**
     * Test whether class metrics take into account methods and inner methods in them. Test enhances existing
     * methodInfo by new entities:
     * <pre>
     *  methodInfo
     *  + MethodInfo "myCoveredLambda"
     *  + MethodInfo "myNonCoveredLambda"
     * </pre>
     * in order to check how ClassInfo.getNumCoveredMethods() and ClassInfo.getNumMethods() are calculated.
     * <p/>
     * See CLOV-1400 bug
     */
    @Test
    void testClassMetricsWithSubMethods() {
        // add a submethod with 1 statement => complexity = 1; method is fully covered
        subMethodInfo = fixture.newMethod(methodInfo, "myCoveredLambda", FullMethodInfo.DEFAULT_METHOD_COMPLEXITY)
        fixture.newMockCoverageDataProvider(subMethodInfo.getDataIndex(), 100)
        fixture.addStatement(subMethodInfo, 1, 6, 100)

        // add a submethod with 1 statement => complexity = 1; method is not covered
        subMethodInfo = fixture.newMethod(methodInfo, "myNonCoveredLambda", FullMethodInfo.DEFAULT_METHOD_COMPLEXITY)
        fixture.newMockCoverageDataProvider(subMethodInfo.getDataIndex(), 0)
        fixture.addStatement(subMethodInfo, 1, 6, 0)

        // now check that enclosing class "sees" these two inner methods
        assertEquals(1, classInfo.getMethods().size())
        assertEquals(3, classInfo.getAllMethods().size())
        assertEquals(1, ((ClassMetrics)classInfo.getMetrics()).getNumCoveredMethods()); // note: method1 is not covered
        assertEquals(3, ((ClassMetrics)classInfo.getMetrics()).getNumMethods())
    }

    /**
     * Test whether class metrics take into account statements (i.e. statements declared directly in the class, not
     * in some methods). Test enhances existing classInfo by new entity:
     * <pre>
     *  classInfo
     *  + StatementInfo
     * </pre>
     */
    @Test
    void testClassMetricsWithStatements() {
        fixture.addStatement(classInfo, 1, 6, 0)

        ClassMetrics expectedMetrics = MetricsHelper.setClassMetrics(   // classInfo + statement
                new ClassMetrics(null),
                1 + 1, 0, 2, 2, 4 + 1,
                0, 0, 0, 0, 0.0f, 1, 0)
        assertEquals(1, classInfo.getStatements().size()); // number of statements from the class, not from its methods
        assertMetricsEquals(expectedMetrics, (ClassMetrics)classInfo.getMetrics())
    }

    /**
     * Test whether file metrics take into account top-level functions (i.e. functions declared directly in the file,
     * not inside a class). Test enhances existing fileInfo by a new entity:
     * <pre>
     *  fileInfo
     *  + MethodInfo "myMethod"
     * </pre>
     */
    @Test
    void testFileMetricsWithMethods() {
        FullMethodInfo myMethod = fixture.newMethod(fileInfo, "myMethod", 10)
        myMethod.setMetrics(MetricsHelper.setBlockMetrics(
                new BlockMetrics(null), 20, 10, 40, 30, 50, 30, 15, 10, 5, 9.99f))

        FileMetrics expectedMetrics = MetricsHelper.setFileMetrics(   // fileInfo + method
                new FileMetrics(null),
                1 + 20, 10, 2 + 40, 2 + 30, 4 + 50,
                30, 15, 10, 5, 9.99f,
                1 + 1, 0, 1, 100, 50)
        assertEquals("expected 1 top-level method", 1, fileInfo.getMethods().size())
        assertEquals("expected 1 top-level class", 1, fileInfo.getClasses().size())
        assertMetricsEquals(expectedMetrics, (FileMetrics) fileInfo.getMetrics())
    }

    /**
     * Test whether file metrics take into account top-level statements (i.e. statements declared directly in the file,
     * not inside a method or a class). Test enhances existing fileInfo by a new entity:
     * <pre>
     *  fileInfo
     *  + StatementInfo
     * </pre>
     */
    @Test
    void testFileMetricsWithStatements() {
        FullStatementInfo myStatement = fixture.addStatement(fileInfo, 2, 10, 0)

        FileMetrics expectedMetrics = MetricsHelper.setFileMetrics(   // fileInfo + myStatement
                new FileMetrics(null),
                1 + 1, 0, 2, 2, 4 + 2,
                0, 0, 0, 0, 0.0f,
                1, 0, 1, 100, 50)
        assertEquals("expected 1 top-level statement", 1, fileInfo.getStatements().size())
        assertEquals("expected 1 top-level class", 1, fileInfo.getClasses().size())
        assertMetricsEquals(expectedMetrics, (FileMetrics) fileInfo.getMetrics())
    }
}
