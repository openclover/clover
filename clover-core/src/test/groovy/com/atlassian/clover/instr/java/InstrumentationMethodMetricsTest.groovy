package com.atlassian.clover.instr.java

import com.atlassian.clover.api.registry.StatementInfo
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.FixedSourceRegion
import com.atlassian.clover.registry.entities.FullMethodInfo
import org.junit.Test

import static org.junit.Assert.assertEquals

class InstrumentationMethodMetricsTest extends InstrumentationTestBase {

    @Test
    void testMethodMetrics() throws Exception {
        checkMethodMetrics("void A() {}",0, 0, 1)
        checkMethodMetrics("void A() {a();}",1, 0, 1)
        checkMethodMetrics("void A() {a = (6 < 7);}",1 ,0 ,1)
        checkMethodMetrics("void A() {a();b();c();}",3, 0, 1)

        // if
        checkMethodMetrics("void A() {if (a()) b(); else c();}",3, 2, 2)
        checkMethodMetrics("void A() {if (a() || b()) c();}", 2, 2, 3)
        checkMethodMetrics("void A() {if (1 + 2 == 4) c();}", 2, 0, 1)

        // for
        checkMethodMetrics("void A() {for (;a();) b(); }",2, 2, 2)
        checkMethodMetrics("void A() {for (;a() || b();) c();}", 2, 2, 3)
        checkMethodMetrics("void A() {for (;1 + 2 == 4;) c();}", 2, 0, 1)

        // while
        checkMethodMetrics("void A() {while (a()) b();}",2, 2, 2)
        checkMethodMetrics("void A() {while (a() || b()) c();}", 2, 2, 3)
        checkMethodMetrics("void A() {while (1 + 2 == 4) c();}", 2, 0, 1)

        // switch with colon cases
        checkMethodMetrics("void A() {switch (a()) { case 1: b();}}", 3, 0, 2)
        checkMethodMetrics("void A() {switch (a()) { case 1: b(); case 2: c();}}", 5, 0, 3)

        // switch with lambda cases
        checkMethodMetrics("void A() {switch (a()) { case 1 -> b();}}", 3, 0, 2)
        checkMethodMetrics("void A() {switch (a()) { case 1 -> b(); case 2 -> c();}}", 5, 0, 3)

        // ternary
        checkMethodMetrics("void A() {a() ? 1 : 2;}", 1, 2, 2)
        checkMethodMetrics("void A() {a() || b()? 1 : 2;}", 1, 2, 3)
        checkMethodMetrics("void A() {a() ? b() ? c()? 1 : 2 : 3 : 4;}", 1, 6, 4)

        // nested functions
    }

    @Test
    void testLambdaMetrics() throws Exception {
        Clover2Registry registry

        // empty lambda
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Runnable r = () -> {  }; }}", 1, 2, 1, 0, 2)
        assertEquals(0, getLambda(registry).getStatements().size())

        // block lambda with one statement
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Runnable r = () -> { return; }; }}", 1, 2, 2, 0, 2)
        assertEquals(1, getLambda(registry).getStatements().size())
        assertSourceRegion(new FixedSourceRegion(1, 41, 1, 48), getLambda(registry).getStatements().get(0))

        // expression lambda with one statement
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Produce<Integer> i = (x) -> 123; }}", 1, 2, 2, 0, 2)
        assertEquals(1, getLambda(registry).getStatements().size())
        assertSourceRegion(new FixedSourceRegion(1, 48, 1, 51), getLambda(registry).getStatements().get(0))

        // expression lambda with one statement and a branch condition
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Produce<Integer> i = (x) -> x < 0 ? x * x : -x; }}", 1, 2, 2, 2, 3)
        assertEquals(1, getLambda(registry).getStatements().size())
        assertEquals(1, getLambda(registry).getBranches().size())
        assertSourceRegion(new FixedSourceRegion(1, 48, 1, 66), getLambda(registry).getStatements().get(0))

        // lambda inside a lambda
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Callable<Runnable> call = () -> () -> { return; }; }}", 1, 3, 3, 0, 3)
        assertEquals(1, getLambda(registry).getStatements().size());                     // outer lambda
        assertEquals(1, getLambda(registry).getMethods().size());                        // outer lambda
        assertEquals(1, getLambda(registry).getMethods().get(0).getStatements().size()); // inner lambda
        assertSourceRegion(
                new FixedSourceRegion(1, 52, 1, 69),
                getLambda(registry).getStatements().get(0)); // outer is "() -> { return; }
        assertSourceRegion(
                new FixedSourceRegion(1, 60, 1, 67),
                getLambda(registry).getMethods().get(0).getStatements().get(0)); // inner is "return;"

        // method reference
        registry = checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, true),
                "class A{void A() { Integer i = Math::abs; }}", 1, 2, 2, 0, 2)
        assertEquals(1, getLambda(registry).getStatements().size())
        assertSourceRegion(
                new FixedSourceRegion(1, 32, 1, 41),
                getLambda(registry).getStatements().get(0)); // "Math::abs"
    }

    private void checkMethodMetrics(String methodSrc,int numStatements, int numBranches, int methodComplexity) throws Exception {
        checkMetrics("class A{"+methodSrc+"}", 1, 1, numStatements, numBranches, methodComplexity)
    }

    private void checkMetrics(String src, int numClasses, int numMethods, int numStatements, int numBranches, int totalComplexity) throws Exception {
        checkMetrics(getInstrConfig(newDbTempFile().getAbsolutePath(), false, true, false),
                src, numClasses, numMethods, numStatements, numBranches, totalComplexity)
    }

    private static FullMethodInfo getLambda(Clover2Registry registry) {
        return (FullMethodInfo) registry.getProject().findClass("A").getMethods().get(0).getMethods().get(0)
    }

    private static void assertSourceRegion(FixedSourceRegion fixedSourceRegion, StatementInfo statementInfo) {
        assertEquals(fixedSourceRegion.getStartLine(), statementInfo.getStartLine())
        assertEquals(fixedSourceRegion.getStartColumn(), statementInfo.getStartColumn())
        assertEquals(fixedSourceRegion.getEndLine(), statementInfo.getEndLine())
        assertEquals(fixedSourceRegion.getEndColumn(), statementInfo.getEndColumn())
    }
}
