package org.openclover.core.instr.java

import com.atlassian.clover.api.registry.StatementInfo
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.FixedSourceRegion
import com.atlassian.clover.registry.entities.FullMethodInfo
import org.junit.Test

import static org.junit.Assert.assertEquals

class InstrumentationMethodMetricsTest extends InstrumentationTestBase {

    /**
     * Simple one execution path, no cycles. Complexity of 1 "coming" from the method itself.
     */
    @Test
    void testMethodMetricsForNoCycle() throws Exception {
        // an empty method, method call, variable assignment, a few statements
        checkMethodMetrics(
                """void A() {
                }""", 0, 0, 1)
        checkMethodMetrics(
                """void A() {
                    a();
                }""", 1, 0, 1)
        checkMethodMetrics(
                """void A() {
                    a = (6 < 7);
                }""", 1, 0, 1)
        checkMethodMetrics(
                """void A() {
                    a();
                    b();
                    c();
                }""", 3, 0, 1)
    }

    /**
     * The "if-else" statement might increase cyclomatic complexity and/or number of branches.
     */
    @Test
    void testMethodMetricsForIfElseStatements() throws Exception {
        // if-else with simple method call expression, two branches (true/false) in expression
        // two paths of execution (if-path, else-path), so one cycle
        checkMethodMetrics(
                """void A() {
                    if (a()) 
                        b(); 
                    else 
                        c();
                }""", 3, 2, 2)

        // if with the boolean expression in condition, two branches (true/false) in expression
        // two paths of execution inside expression - 'a()' or 'b()' - so one extra cycle
        // two paths of execution (if-path, empty-else-path) - so another one cycle
        checkMethodMetrics(
                """void A() {
                    if (a() || b()) 
                        c();
                }""", 2, 2, 3)

        // if with a constant expression, so it always evaluates to the same value, no branches then
        // of the two paths of execution (if-path, empty-else-path) only one always runs, so in fact no cycle
        checkMethodMetrics(
                """void A() {
                    if (1 + 2 == 4) 
                        c();
                }""", 2, 0, 1)
    }

    /**
     * Similarly as above, 'for' loops might increase cyclomatic complexity and/or number of branches.
     */
    @Test
    void testMethodMetricsWithForLoops() throws Exception {
        // for
        checkMethodMetrics(
                """void A() {
                    for (;a();) 
                        b(); 
                }""", 2, 2, 2)
        checkMethodMetrics(
                """void A() {
                    for (;a() || b();) 
                        c();
                }""", 2, 2, 3)
        checkMethodMetrics(
                """void A() {
                    for (;1 + 2 == 4;) 
                        c();
                }""", 2, 0, 1)
    }

    @Test
    void testMethodMetricsWithWhileLoops() throws Exception {
        // while
        checkMethodMetrics(
                """void A() {
                    while (a()) b();
                }""", 2, 2, 2)
        checkMethodMetrics(
                """void A() {
                    while (a() || b()) 
                        c();
                }""", 2, 2, 3)
        checkMethodMetrics(
                """void A() {
                    while (1 + 2 == 4) 
                        c();
                }""", 2, 0, 1)
    }

    @Test
    void testMethodMetricsForColonBasedSwitchStatements() throws Exception {
        // NOTICE: code metrics for colon-based and lambda-based switch blocks are different!

        // switch with colon cases
        // because colon cases can be grouped together, each of them represents a separate entry
        // point into the case block; therefore, every case is instrumented separately and is treated as
        // a statement; statements in a block are also instrumented, even if there is only one case and
        // one statement
        checkMethodMetrics(
                """void A() {
                    switch (a()) { 
                        case 1: 
                            b();
                    }
                }""", 3, 0, 2)

        checkMethodMetrics(
                """void A() {
                    switch (a()) { 
                        case 1:
                            b(); 
                        case 2: 
                            c();
                    }
                }""", 5, 0, 3)

        // two cases grouped together, still the same number of statements and complexity as above
        checkMethodMetrics(
                """void A() {
                    switch (a()) { 
                        case 1:
                        case 2:  
                            b();
                            c();
                    }
                }""", 5, 0, 3)
    }

    @Test
    void testMethodMetricsForLambdaBasedSwitchStatements() throws Exception {
        // switch with lambda cases
        // because lambda cases cannot be grouped, each case label is associated with a separate code block
        // and each of them is the only entry point to that block; therefore, the case label itself is
        // NOT instrumented and does not represent a statement; expressions or code blocks inside the case
        // are of course instrumented
        checkMethodMetrics(
                """void A() {
                    switch (a()) { 
                        case 1 -> b();
                    }
                }""", 2, 0, 1)

        checkMethodMetrics(
                """void A() {
                    switch (a()) { 
                        case 1 -> b(); 
                        case 2 -> c();
                    }
                }""", 3, 0, 2)
    }

    @Test
    void testSwitchStatementsInBothFormsHaveEquivalentCyclomaticComplexity() throws Exception {
        // expressions with switch expressions inside
        // notice that in samples below both forms have the same cyclomatic complexity,
        // although number of statements can differ, as explained above
        checkMethodMetrics(
                """void A() {
                    int i = switch(j) {
                        case 0 -> -1;
                        case 1 -> 1;
                        default -> throw new IllegalArgumentException();
                    };
                }""", 4, 0, 3)

        checkMethodMetrics(
                """void A() {
                    int i = switch(j) { 
                        case 0: yield -1; 
                        case 1: yield 1; 
                        default: throw new IllegalArgumentException(); 
                    };
                }""", 7, 0, 3)
    }

    @Test
    void testCyclomaticComplexityForColonBasedSwitchStatementsWithHiddenPaths() throws Exception {
        // colon-based switch statements with hidden branches
        // the colon-based switch does not require to cover all possible values,
        // the 'default' keyword is also optional, so there might be an "invisible" branch
        checkMethodMetrics(
                """void A(int j) {
                    int i;
                    switch(j) { 
                        case 0: i = -1; break; // 1st path
                        case 1: i = 1; break;  // 2nd path
                        default: /* no op */;  // 3rd path
                    };
                }""", 10, 0, 3)

        checkMethodMetrics(
                """void A(int j) {
                    int i;
                    switch(j) { 
                        case 0: i = -1; break;         // 1st path
                        case 1: i = 1; break;          // 2nd path
                        /* "invisible" default path */ // 3rd path
                    };
                }""", 8, 0, 3)

        checkMethodMetrics(
                """void A(int j) {
                    int i;
                    switch(j) { 
                        default: i = -1;               // 1st path
                        /* no "invisible" path */ 
                    };
                }""", 4, 0, 1)
    }

    @Test
    void testCyclomaticComplexityIsPropagatedForArgumentLists() throws Exception {
        // arg list in method calls
        checkMethodMetrics(
                """void A(int j) {          // 1 from method
                    foo(                    // statement
                        switch(j) {         // 1 cycle
                            case 0 -> 10;   // statement
                            default -> 99;  // statement
                        },
                        switch(j) {         // 2 cycles
                            case 0 -> 10;   // statement
                            case 1 -> 20;   // statement
                            default -> 99;  // statement
                        },
                        switch(j) {         // 0 cycles
                            default -> 99;  // statement
                        });
                }""", 7, 0, 4)

        // arg list in constructor calls
        checkMethodMetrics(
                """void A(int j) {          // 1 from method
                    super(                  // statement
                        switch(j) {         // 2 cycles
                            case 0 -> 10;   // statement
                            case 1 -> 20;   // statement
                            default -> 99;  // statement
                        });
                }""", 4, 0, 3)
    }

    @Test
    void testMethodMetricsForTernaryExpressions() throws Exception {
        // ternary
        checkMethodMetrics(
                """void A() {
                    a() ? 1 : 2;
                }""", 1, 2, 2)

        checkMethodMetrics(
                """void A() {
                    a() || b() ? 1 : 2;
                }""", 1, 2, 3)

        checkMethodMetrics(
                """void A() {
                    a() ? 
                        b() ? 
                            c() ? 1 : 2 
                            : 3 
                        : 4;
                }""", 1, 6, 4)
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
