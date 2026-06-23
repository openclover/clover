package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.buildutil.test.junit.GroovyVersionStart
import org.openclover.core.CloverDatabase
import org.openclover.core.CodeType
import org.openclover.core.CoverageDataSpec
import org.openclover.core.api.registry.BranchInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.StatementInfo
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo

/**
 * Integration tests verifying Clover instrumentation of Groovy 5 language features.
 *
 * Groovy 5 key changes vs Groovy 4:
 *   - Logical implication operator (==>): BinaryExpression with new token; no special handling needed.
 *   - Pattern matching for instanceof (obj instanceof String s): BinaryExpression with DeclarationExpression
 *     on the RHS; branch wrapping by BranchInstrumenter preserves the binding — no code changes needed.
 *   - Index variable in for loops (for (int idx, var item in items)): ForStatement gains indexVariable field;
 *     visitForLoop uses getCollectionExpression() and getLoopBlock() — both still valid.
 *   - Multi-assignment with var (var (x, y) = [1, 2]): same TupleExpression as def (x, y) — no changes.
 *   - Underscore placeholder (_): regular VariableExpression("_") — no changes.
 *   - Multidimensional array literals: existing ArrayExpression — no changes.
 *   - Interface native default/private/static methods: native JVM interface methods in Groovy 5;
 *     isInstrumentable(MethodNode) already returns true for non-abstract, non-synthetic methods.
 *   - Instance main() method: instrumented as a regular instance method.
 *
 * No changes were made to InstrumentingCodeVisitor or StatementInstrumenter for Groovy 5.
 */
@CompileStatic
class Groovy5CoverageRecordingTest extends TestBase {

    Groovy5CoverageRecordingTest(String testName) {
        super(testName)
    }

    Groovy5CoverageRecordingTest(String methodName, String specificName,
                                 File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    // -----------------------------------------------------------------------
    // Logical implication operator (==>)
    //
    // a ==> b compiles to BinaryExpression(a, "==>", b) — same structure as any binary op.
    // No special coverage tracking needed (same policy as && and ||).
    // implies() called 4 times → hitCount == 4.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("5.0.0")
    void testLogicalImplicationHitCounts() {
        instrumentAndCompileWithGrover(["ImplicationTest.groovy": '''class ImplicationTest {
    static boolean implies(boolean a, boolean b) {
        return a ==> b
    }
    static void main(String[] args) {
        assert implies(false, true)
        assert implies(false, false)
        assert implies(true, true)
        assert !implies(true, false)
    }
}'''])
        runWithAsserts("ImplicationTest")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("ImplicationTest.groovy"), { FullFileInfo f ->
                        assertClass f, named("ImplicationTest"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("implies"), { MethodInfo m ->
                                m.hitCount == 4
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Pattern matching for instanceof — both branches
    //
    // "obj instanceof String s" compiles to BinaryExpression(obj, instanceof, DeclarationExpression(s)).
    // BranchInstrumenter wraps the entire condition; runtime test confirmed that the 's' binding is
    // accessible in the if-body even after the condition is wrapped in &&/||.
    //
    // describe("hello") → String match (true branch); describe(42) → no match (false branch).
    // Method hitCount == 2; if-branch: trueHits == 1, falseHits == 1.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("5.0.0")
    void testPatternMatchingInstanceofHitCounts() {
        instrumentAndCompileWithGrover(["PatternMatch.groovy": '''class PatternMatch {
    static String describe(Object obj) {
        if (obj instanceof String s) {
            return s.toUpperCase()
        }
        return "other"
    }
    static void main(String[] args) {
        assert describe("hello") == "HELLO"
        assert describe(42) == "other"
    }
}'''])
        runWithAsserts("PatternMatch")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("PatternMatch.groovy"), { FullFileInfo f ->
                        assertClass f, named("PatternMatch"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("describe"), { MethodInfo m ->
                                m.hitCount == 2 &&
                                m.branches.size() == 1 &&
                                assertBranch(m, { BranchInfo b -> true }, hits(1, 1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Index variable in for loops
    //
    // "for (int idx, var item in items)" uses ForStatement.indexVariable (new in Groovy 5).
    // visitForLoop in InstrumentingCodeVisitor uses getCollectionExpression() and getLoopBlock()
    // which are both still valid; getVariable() still returns the value variable for backward compat.
    //
    // 3 elements → loop body hit 3 times.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("5.0.0")
    void testIndexedForLoopHitCounts() {
        instrumentAndCompileWithGrover(["IndexedForLoop.groovy": '''class IndexedForLoop {
    static List<String> enumerate(List<String> items) {
        def result = []
        for (int idx, var item in items) {
            result.add("${idx}:${item}")
        }
        return result
    }
    static void main(String[] args) {
        assert enumerate(["a", "b", "c"]) == ["0:a", "1:b", "2:c"]
    }
}'''])
        runWithAsserts("IndexedForLoop")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("IndexedForLoop.groovy"), { FullFileInfo f ->
                        assertClass f, named("IndexedForLoop"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("enumerate"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                // loop body (result.add) hit once per element
                                assertStatement(m, { StatementInfo s -> s.hitCount == 3 }, hits(3))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Multi-assignment with var
    //
    // "var (x, y) = pair" compiles to DeclarationExpression(TupleExpression, =, ...) — same
    // AST as "def (x, y) = pair". No instrumentation changes needed.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("5.0.0")
    void testMultiAssignWithVarHitCounts() {
        instrumentAndCompileWithGrover(["MultiAssign.groovy": '''class MultiAssign {
    static def sumPair(List pair) {
        var (x, y) = pair
        return x + y
    }
    static void main(String[] args) {
        assert sumPair([3, 4]) == 7
    }
}'''])
        runWithAsserts("MultiAssign")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("MultiAssign.groovy"), { FullFileInfo f ->
                        assertClass f, named("MultiAssign"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("sumPair"), { MethodInfo m ->
                                m.hitCount == 1
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Underscore placeholder
    //
    // "var (_, second) = pair" — underscore compiles to VariableExpression("_"), not a new node type.
    // Verify the second element is accessible and the method is instrumented.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("5.0.0")
    void testUnderscorePlaceholderHitCounts() {
        instrumentAndCompileWithGrover(["UnderscorePlaceholder.groovy": '''class UnderscorePlaceholder {
    static def second(List pair) {
        var (_, second) = pair
        return second
    }
    static void main(String[] args) {
        assert second([10, 20]) == 20
    }
}'''])
        runWithAsserts("UnderscorePlaceholder")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("UnderscorePlaceholder.groovy"), { FullFileInfo f ->
                        assertClass f, named("UnderscorePlaceholder"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("second"), { MethodInfo m ->
                                m.hitCount == 1
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Interface native default method (Groovy 5: native JVM interface method)
    //
    // In Groovy 4 and earlier, default interface methods went through the $Trait$Helper mechanism.
    // In Groovy 5, they are native JVM interface default methods (JDK 8+ feature).
    // isInstrumentable(MethodNode) returns true for non-abstract, non-synthetic methods.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("5.0.0")
    void testInterfaceNativeDefaultMethodHitCounts() {
        instrumentAndCompileWithGrover(["InterfaceDefault.groovy": '''interface Greeter {
    default String greet(String name) {
        return "Hello, " + name
    }
}
class FriendlyGreeter implements Greeter {}
class InterfaceDefaultTest {
    static void main(String[] args) {
        assert new FriendlyGreeter().greet("world") == "Hello, world"
    }
}'''])
        runWithAsserts("InterfaceDefaultTest")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("InterfaceDefault.groovy"), { FullFileInfo f ->
                        assertClass f, named("Greeter"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("greet"), { MethodInfo m ->
                                m.hitCount == 1
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Interface private method (Groovy 5: native JVM private interface method)
    //
    // Private interface methods (Java 9+) can only be called from other interface methods.
    // greet() calls private prefix() — both should be instrumented and recorded.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("5.0.0")
    void testInterfacePrivateMethodHitCounts() {
        instrumentAndCompileWithGrover(["InterfacePrivate.groovy": '''interface PrivateHelper {
    default String greet(String name) {
        return prefix() + name
    }
    private String prefix() {
        return "Hi, "
    }
}
class PrivateHelperImpl implements PrivateHelper {}
class InterfacePrivateTest {
    static void main(String[] args) {
        assert new PrivateHelperImpl().greet("world") == "Hi, world"
    }
}'''])
        runWithAsserts("InterfacePrivateTest")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("InterfacePrivate.groovy"), { FullFileInfo f ->
                        assertClass f, named("PrivateHelper"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("greet"), { MethodInfo m ->
                                m.hitCount == 1
                            }) &&
                            assertMethod(c, simplyNamed("prefix"), { MethodInfo m ->
                                m.hitCount == 1
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Interface static method (Groovy 5: native JVM static interface method)
    //
    // Static interface methods were already supported earlier via $Trait$Helper.
    // In Groovy 5, they are native JVM static interface methods — callable as StaticUtil.utility().
    // -----------------------------------------------------------------------
    @GroovyVersionStart("5.0.0")
    void testInterfaceStaticMethodHitCounts() {
        instrumentAndCompileWithGrover(["InterfaceStatic.groovy": '''interface StaticUtil {
    static String utility() {
        return "I am utility"
    }
}
class InterfaceStaticTest {
    static void main(String[] args) {
        assert StaticUtil.utility() == "I am utility"
    }
}'''])
        runWithAsserts("InterfaceStaticTest")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("InterfaceStatic.groovy"), { FullFileInfo f ->
                        assertClass f, named("StaticUtil"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("utility"), { MethodInfo m ->
                                m.hitCount == 1
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Instance main() method (JEP 512 style)
    //
    // In Groovy 5, a class can have an instance "void main()" entry point.
    // Clover instruments it as a regular instance method.
    // Called twice to distinguish from the static main(String[]) (hitCount=1) in assertions.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("5.0.0")
    void testInstanceMainMethodHitCounts() {
        instrumentAndCompileWithGrover(["InstanceMain.groovy": '''class InstanceMain {
    void main() {}
    static void main(String[] args) {
        def obj = new InstanceMain()
        obj.main()
        obj.main()
    }
}'''])
        runWithAsserts("InstanceMain")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("InstanceMain.groovy"), { FullFileInfo f ->
                        assertClass f, named("InstanceMain"), { FullClassInfo c ->
                            // instance void main() called twice — distinguishes from static main (hitCount=1)
                            assertMethod(c, { MethodInfo m -> m.simpleName == "main" && m.hitCount == 2 }, { MethodInfo m ->
                                m.hitCount == 2
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Multidimensional array literals
    //
    // int[][] numsA = [[1,2,3],[4,5,6]]      → DeclarationExpression(ListExpression)
    // def numsB = new int[][] {{1,2,3},{4,5,6}}  → ConstructorCallExpression(ArrayExpression)
    // Both forms use existing AST node types — verify no crash during instrumentation.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("5.0.0")
    void testMultiDimArrayLiteralHitCounts() {
        instrumentAndCompileWithGrover(["MultiDimArray.groovy": '''class MultiDimArray {
    static void main(String[] args) {
        int[][] numsA = [[1,2,3],[4,5,6]]
        def numsB = new int[][] {{1,2,3},{4,5,6}}
        assert numsA[0][0] == 1
        assert numsB[1][2] == 6
    }
}'''])
        runWithAsserts("MultiDimArray")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("MultiDimArray.groovy"), { FullFileInfo f ->
                        assertClass f, named("MultiDimArray"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                m.hitCount == 1
                            })
                        }
                    }
                })
    }
}
