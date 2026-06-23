package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.buildutil.test.junit.GroovyVersionStart
import org.openclover.core.CloverDatabase
import org.openclover.core.CodeType
import org.openclover.core.CoverageDataSpec
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.StatementInfo
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo

/**
 * Integration tests verifying Clover instrumentation of Groovy 4 language features.
 *
 * Groovy 4 key changes vs Groovy 3:
 *   - Switch expressions (arrow and yield forms) compile to a ClosureExpression wrapping a SwitchStatement.
 *   - Switch expressions without an explicit default get a Groovyc-injected EmptyStatement with
 *     [-1:-1..-1:-1] coordinates; Clover synthesizes a 1-character region at the closing '}' so that
 *     the fallthrough branch is recorded.
 *   - Records compile to regular ClassNodes with auto-generated methods (toString, equals, hashCode, etc.).
 *   - Sealed classes/interfaces use the @Sealed annotation; no new AST nodes.
 *   - Enhanced range syntax and decimal literals without leading zero use the same AST nodes as before.
 *
 * Column positions in at(line, startCol, endLine, endCol) are 1-indexed; endCol is exclusive.
 */
@CompileStatic
class Groovy4CoverageRecordingTest extends TestBase {

    Groovy4CoverageRecordingTest(String testName) {
        super(testName)
    }

    Groovy4CoverageRecordingTest(String methodName, String specificName,
                                 File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    // -----------------------------------------------------------------------
    // Switch expression — arrow form with explicit default (value-returning)
    //
    // Compiled to: return { -> __$$sev0 = n; switch(__$$sev0) { ... } }.call()
    // Closure called 3 times (for n=1, n=2, n=99).
    // -----------------------------------------------------------------------
    @GroovyVersionStart("4.0.0")
    void testSwitchArrowWithDefaultHitCounts() {
        instrumentAndCompileWithGrover(["SwitchArrow.groovy": '''class SwitchArrow {
    static String classify(int n) {
        return switch (n) {
            case 1 -> "one"
            case 2 -> "two"
            default -> "other"
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        assert classify(2) == "two"
        assert classify(99) == "other"
    }
}'''])
        runWithAsserts("SwitchArrow")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("SwitchArrow.groovy"), { FullFileInfo f ->
                        assertClass f, named("SwitchArrow"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("classify"), { MethodInfo m ->
                                m.hitCount == 3 &&
                                assertStatement(m, at(4, 23, 4, 28), hits(1)) &&
                                assertStatement(m, at(5, 23, 5, 28), hits(1)) &&
                                assertStatement(m, at(6, 24, 6, 31), hits(1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Switch expression — arrow form without default (null fallthrough)
    //
    // Groovyc injects a synthetic EmptyStatement with [-1:-1..-1:-1] for the missing default.
    // After the fix in visitSwitch(), a 1-char synthesized region is created at the closing '}'
    // of the SwitchStatement (line 6, col 9 exclusive-end 10) and registered in the model.
    //
    // classify(1)  → case-1 branch, default NOT hit
    // classify(99) → no case matches, injected null-default IS hit
    // -----------------------------------------------------------------------
    @GroovyVersionStart("4.0.0")
    void testSwitchArrowNoDefaultNullBranch() {
        instrumentAndCompileWithGrover(["SwitchArrowNoDefault.groovy": '''class SwitchArrowNoDefault {
    static String classify(int n) {
        return switch (n) {
            case 1 -> "one"
            case 2 -> "two"
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        assert classify(99) == null
    }
}'''])
        runWithAsserts("SwitchArrowNoDefault")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("SwitchArrowNoDefault.groovy"), { FullFileInfo f ->
                        assertClass f, named("SwitchArrowNoDefault"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("classify"), { MethodInfo m ->
                                // classify called twice; the synthesized default (injected null branch)
                                // appears at the closing '}' of the switch (line 6, col 9..10) with hit=1
                                m.hitCount == 2 &&
                                assertStatement(m, at(4, 23, 4, 28), hits(1)) &&
                                assertStatement(m, at(5, 23, 5, 28), hits(0)) &&
                                assertStatement(m, at(6, 9, 6, 10), hits(1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Switch expression — yield form with explicit default (value-returning)
    // -----------------------------------------------------------------------
    @GroovyVersionStart("4.0.0")
    void testSwitchYieldWithDefaultHitCounts() {
        instrumentAndCompileWithGrover(["SwitchYield.groovy": '''class SwitchYield {
    static String classify(int n) {
        return switch (n) {
            case 1: yield "one"
            case 2: yield "two"
            default: yield "other"
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        assert classify(99) == "other"
    }
}'''])
        runWithAsserts("SwitchYield")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("SwitchYield.groovy"), { FullFileInfo f ->
                        assertClass f, named("SwitchYield"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("classify"), { MethodInfo m ->
                                m.hitCount == 2 &&
                                assertStatement(m, at(4, 21, 4, 32), hits(1)) &&
                                assertStatement(m, at(5, 21, 5, 32), hits(0)) &&
                                assertStatement(m, at(6, 22, 6, 35), hits(1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Switch expression — yield form without default (null fallthrough)
    //
    // Same injected-null-default issue as the arrow-no-default case.
    // classify(1)  → case-1 hit; classify(99) → injected default hit.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("4.0.0")
    void testSwitchYieldNoDefaultNullBranch() {
        instrumentAndCompileWithGrover(["SwitchYieldNoDefault.groovy": '''class SwitchYieldNoDefault {
    static String classify(int n) {
        return switch (n) {
            case 1: yield "one"
            case 2: yield "two"
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        assert classify(99) == null
    }
}'''])
        runWithAsserts("SwitchYieldNoDefault")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("SwitchYieldNoDefault.groovy"), { FullFileInfo f ->
                        assertClass f, named("SwitchYieldNoDefault"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("classify"), { MethodInfo m ->
                                m.hitCount == 2 &&
                                assertStatement(m, at(4, 21, 4, 32), hits(1)) &&
                                assertStatement(m, at(5, 21, 5, 32), hits(0)) &&
                                assertStatement(m, at(6, 9, 6, 10), hits(1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Switch expression — void/side-effect cases (no returned value)
    //
    // Void switch still compiles to a closure call (ExpressionStatement rather than
    // ReturnStatement wrapping the call). The injected null-default is still present
    // when there is no explicit default.
    //
    // classify(1)  → case-1 side effect; classify(99) → injected default hit.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("4.0.0")
    void testSwitchVoidHitCounts() {
        instrumentAndCompileWithGrover(["SwitchVoid.groovy": '''class SwitchVoid {
    static List<String> log = []
    static void classify(int n) {
        switch (n) {
            case 1 -> log.add("one")
            case 2 -> log.add("two")
        }
    }
    static void main(String[] args) {
        classify(1)
        classify(99)
        assert log == ["one"]
    }
}'''])
        runWithAsserts("SwitchVoid")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("SwitchVoid.groovy"), { FullFileInfo f ->
                        assertClass f, named("SwitchVoid"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("classify"), { MethodInfo m ->
                                // classify called twice; injected default is at closing '}' of switch
                                // (line 7 in the source, col 9..10)
                                m.hitCount == 2 &&
                                assertStatement(m, at(5, 23, 5, 37), hits(1)) &&
                                assertStatement(m, at(6, 23, 6, 37), hits(0)) &&
                                assertStatement(m, at(7, 9, 7, 10), hits(1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Record — auto-generated and user-defined methods
    //
    // Records compile to a ClassNode with @RecordBase, @TupleConstructor etc.
    // Auto-generated methods (toString, equals, hashCode, …) are NOT flagged synthetic,
    // so Clover instruments them. User-defined methods are instrumented normally.
    // Test verifies no crash during instrumentation and that user-defined origin() is hit.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("4.0.0")
    void testRecordGeneratedAndUserMethodHitCounts() {
        instrumentAndCompileWithGrover(["Point.groovy": '''record Point(int x, int y) {
    static Point origin() { new Point(0, 0) }
    static void main(String[] args) {
        def p = new Point(3, 4)
        assert p.x() == 3
        assert p.y() == 4
        assert p.toString() != null
        def o = origin()
        assert o.x() == 0
    }
}'''])
        runWithAsserts("Point")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("Point.groovy"), { FullFileInfo f ->
                        assertClass f, named("Point"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("origin"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                assertStatement(m, at(2, 29, 2, 44), hits(1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Sealed classes — verification that instrumentation does not crash
    //
    // sealed interface Shape permits Circle, Square {} compiles to a ClassNode
    // annotated with @groovy.transform.Sealed. No new expression/statement nodes;
    // just verify that Clover processes the code without throwing an exception.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("4.0.0")
    void testSealedClassesDoNotCrash() {
        instrumentAndCompileWithGrover(["SealedShape.groovy": '''sealed interface Shape permits Circle, Square {}
final class Circle implements Shape {
    final float radius
    Circle(float r) { this.radius = r }
}
final class Square implements Shape {
    final float side
    Square(float s) { this.side = s }
}
class SealedTest {
    static String name(Shape s) {
        if (s instanceof Circle) return "circle"
        if (s instanceof Square) return "square"
        return "unknown"
    }
    static void main(String[] args) {
        assert name(new Circle(1.0f)) == "circle"
        assert name(new Square(1.0f)) == "square"
    }
}'''])
        runWithAsserts("SealedTest")
    }

    // -----------------------------------------------------------------------
    // Enhanced range syntax — left-open (<..) and both-open (<..<)
    //
    // Parses to RangeExpression nodes (same class as before) with exclusiveLeft/
    // exclusiveRight flags. No instrumentation changes needed; verify no crash and
    // correct method hit counts.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("4.0.0")
    void testEnhancedRangeHitCounts() {
        instrumentAndCompileWithGrover(["RangeTest.groovy": '''class RangeTest {
    static List<Integer> leftOpen(int from, int to) {
        return (from<..to).toList()
    }
    static List<Integer> bothOpen(int from, int to) {
        return (from<..<to).toList()
    }
    static void main(String[] args) {
        assert leftOpen(0, 3) == [1, 2, 3]
        assert bothOpen(0, 3) == [1, 2]
    }
}'''])
        runWithAsserts("RangeTest")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("RangeTest.groovy"), { FullFileInfo f ->
                        assertClass f, named("RangeTest"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("leftOpen"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                assertStatement(m, at(3, 9, 3, 36), hits(1))
                            }) &&
                            assertMethod(c, simplyNamed("bothOpen"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                assertStatement(m, at(6, 9, 6, 37), hits(1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Decimal literal without leading zero (.5 == 0.5)
    //
    // Parses to a ConstantExpression (same as before). No instrumentation changes
    // needed; verify no crash and correct method hit counts.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("4.0.0")
    void testDecimalLiteralWithoutLeadingZero() {
        instrumentAndCompileWithGrover(["DecimalLiteralTest.groovy": '''class DecimalLiteralTest {
    static double half() { .5 }
    static double quarter() { .25 }
    static void main(String[] args) {
        assert half() == 0.5
        assert quarter() == 0.25
    }
}'''])
        runWithAsserts("DecimalLiteralTest")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("DecimalLiteralTest.groovy"), { FullFileInfo f ->
                        assertClass f, named("DecimalLiteralTest"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("half"), { MethodInfo m ->
                                m.hitCount == 1
                            }) &&
                            assertMethod(c, simplyNamed("quarter"), { MethodInfo m ->
                                m.hitCount == 1
                            })
                        }
                    }
                })
    }
}
