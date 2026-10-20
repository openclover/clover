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
 * Integration tests verifying Clover instrumentation of Groovy 6 language features.
 *
 * Groovy 6 key changes vs Groovy 5:
 *   - Switch expressions are a first-class SwitchExpression AST node instead of a closure wrapping a
 *     SwitchStatement. Case arms are reached through visitCaseStatement(); the default arm is instrumented
 *     by InstrumentingCodeVisitor.instrumentSwitchExpressionDefault() (resolved by reflection).
 *   - A switch expression without a default throws IllegalStateException for an unmatched selector
 *     (IncompatibleClassChangeError for a complete enum switch) rather than yielding null, so no default
 *     region is synthesized.
 *
 * Column positions in at(line, startCol, endLine, endCol) are 1-indexed; endCol is exclusive.
 */
@CompileStatic
class Groovy6CoverageRecordingTest extends TestBase {

    Groovy6CoverageRecordingTest(String testName) {
        super(testName)
    }

    Groovy6CoverageRecordingTest(String methodName, String specificName,
                                 File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    private void assertClassify(String fileName, String className, Closure<Boolean> methodAssertions) {
        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named(fileName), { FullFileInfo f ->
                        assertClass f, named(className), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("classify"), methodAssertions)
                        }
                    }
                })
    }

    private static boolean hitOnceOnLines(MethodInfo m, List<Integer> lines) {
        lines.every { int line -> m.statements.any { StatementInfo s -> s.startLine == line && s.hitCount == 1 } }
    }

    private static boolean noStatementOnLine(MethodInfo m, int line) {
        m.statements.every { StatementInfo s -> s.startLine != line }
    }

    /** Switch expression without default: unmatched selector throws, no default region */
    @GroovyVersionStart("6.0.0")
    void testSwitchArrowNoDefaultThrows() {
        instrumentAndCompileWithGrover(["SwitchNoDefault.groovy": '''class SwitchNoDefault {
    static String classify(int n) {
        return switch (n) {
            case 1 -> "one"
            case 2 -> "two"
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        try {
            classify(99)
            assert false
        } catch (IllegalStateException expected) {
        }
    }
}'''])
        runWithAsserts("SwitchNoDefault")

        assertClassify("SwitchNoDefault.groovy", "SwitchNoDefault", { MethodInfo m ->
            m.hitCount == 2 &&
            assertStatement(m, at(4, 23, 4, 28), hits(1)) &&
            assertStatement(m, at(5, 23, 5, 28), hits(0)) &&
            noStatementOnLine(m, 6)
        })
    }

    /** Default arm as a block: the arm and the statements inside it are recorded */
    @GroovyVersionStart("6.0.0")
    void testSwitchDefaultBlockArm() {
        instrumentAndCompileWithGrover(["SwitchDefaultBlock.groovy": '''class SwitchDefaultBlock {
    static String classify(int n) {
        return switch (n) {
            case 1 -> "one"
            default -> {
                def s = "n" + n
                yield s
            }
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        assert classify(7) == "n7"
        assert classify(8) == "n8"
    }
}'''])
        runWithAsserts("SwitchDefaultBlock")

        assertClassify("SwitchDefaultBlock.groovy", "SwitchDefaultBlock", { MethodInfo m ->
            m.hitCount == 3 &&
            assertStatement(m, at(4, 23, 4, 28), hits(1)) &&
            assertStatement(m, at(6, 17, 6, 32), hits(2)) &&
            assertStatement(m, at(7, 17, 7, 24), hits(2))
        })
    }

    /** Statically compiled switch expressions (int table switch and String switch) */
    @GroovyVersionStart("6.0.0")
    void testCompileStaticSwitchWithDefault() {
        instrumentAndCompileWithGrover(["SwitchStatic.groovy": '''@groovy.transform.CompileStatic
class SwitchStatic {
    static String classify(int n) {
        return switch (n) {
            case 1 -> "one"
            case 2 -> "two"
            default -> "other"
        }
    }
    static int code(String s) {
        return switch (s) {
            case "a" -> 1
            default -> 0
        }
    }
    static void main(String[] args) {
        assert classify(1) == "one"
        assert classify(99) == "other"
        assert code("a") == 1
        assert code("b") == 0
        assert code("c") == 0
    }
}'''])
        runWithAsserts("SwitchStatic")

        assertClassify("SwitchStatic.groovy", "SwitchStatic", { MethodInfo m ->
            m.hitCount == 2 &&
            assertStatement(m, at(5, 23, 5, 28), hits(1)) &&
            assertStatement(m, at(6, 23, 6, 28), hits(0)) &&
            assertStatement(m, at(7, 24, 7, 31), hits(1))
        })
        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("SwitchStatic.groovy"), { FullFileInfo f ->
                        assertClass f, named("SwitchStatic"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("code"), { MethodInfo m ->
                                m.hitCount == 3 &&
                                assertStatement(m, at(12, 25, 12, 26), hits(1)) &&
                                assertStatement(m, at(13, 24, 13, 25), hits(2))
                            })
                        }
                    }
                })
    }

    /** Complete enum switch without default */
    @GroovyVersionStart("6.0.0")
    void testEnumSwitchWithoutDefault() {
        instrumentAndCompileWithGrover(["SwitchEnum.groovy": '''enum Color { RED, GREEN }
class SwitchEnum {
    static String classify(Color c) {
        return switch (c) {
            case Color.RED -> "warm"
            case Color.GREEN -> "cool"
        }
    }
    static void main(String[] args) {
        assert classify(Color.RED) == "warm"
        assert classify(Color.GREEN) == "cool"
        assert classify(Color.GREEN) == "cool"
    }
}'''])
        runWithAsserts("SwitchEnum")

        assertClassify("SwitchEnum.groovy", "SwitchEnum", { MethodInfo m ->
            m.hitCount == 3 &&
            assertStatement(m, at(5, 31, 5, 37), hits(1)) &&
            assertStatement(m, at(6, 33, 6, 39), hits(2)) &&
            noStatementOnLine(m, 7)
        })
    }

    /** A branch in the selector is recorded once, not once per transformation of the switch expression */
    @GroovyVersionStart("6.0.0")
    void testSelectorBranchCountedOnce() {
        instrumentAndCompileWithGrover(["SwitchSelector.groovy": '''class SwitchSelector {
    static String classify(String s) {
        return switch (s ?: "none") {
            case "none" -> "empty"
            default -> s
        }
    }
    static void main(String[] args) {
        assert classify(null) == "empty"
        assert classify("a") == "a"
    }
}'''])
        runWithAsserts("SwitchSelector")

        assertClassify("SwitchSelector.groovy", "SwitchSelector", { MethodInfo m ->
            m.hitCount == 2 &&
            assertBranch(m, { BranchInfo b -> true }, hits(1, 1)) &&
            assertStatement(m, at(4, 28, 4, 35), hits(1)) &&
            assertStatement(m, at(5, 24, 5, 25), hits(1))
        })
    }

    /** A switch expression nested in a default arm, assigned to a variable */
    @GroovyVersionStart("6.0.0")
    void testNestedSwitchInDefaultArm() {
        instrumentAndCompileWithGrover(["SwitchNested.groovy": '''class SwitchNested {
    static String classify(int a, int b) {
        def r = switch (a) {
            case 1 -> "a1"
            default -> switch (b) {
                case 1 -> "b1"
                default -> "b?"
            }
        }
        return r
    }
    static void main(String[] args) {
        assert classify(1, 0) == "a1"
        assert classify(2, 1) == "b1"
        assert classify(2, 2) == "b?"
    }
}'''])
        runWithAsserts("SwitchNested")

        assertClassify("SwitchNested.groovy", "SwitchNested", { MethodInfo m ->
            m.hitCount == 3 &&
            assertStatement(m, at(4, 23, 4, 27), hits(1)) &&
            assertStatement(m, at(5, 24, 8, 14), hits(2)) &&
            assertStatement(m, at(6, 27, 6, 31), hits(1)) &&
            assertStatement(m, at(7, 28, 7, 32), hits(1))
        })
    }

    /** A switch expression inside a closure */
    @GroovyVersionStart("6.0.0")
    void testSwitchInClosure() {
        instrumentAndCompileWithGrover(["SwitchClosure.groovy": '''class SwitchClosure {
    static List<String> classify(List<Integer> ns) {
        return ns.collect { int n ->
            return switch (n) {
                case 0 -> "zero"
                default -> "many"
            }
        }
    }
    static void main(String[] args) {
        assert classify([0, 1, 2]) == ["zero", "many", "many"]
    }
}'''])
        runWithAsserts("SwitchClosure")

        assertClassify("SwitchClosure.groovy", "SwitchClosure", { MethodInfo m ->
            assertStatement(m, at(5, 27, 5, 33), hits(1)) &&
            assertStatement(m, at(6, 28, 6, 34), hits(2))
        })
    }

    /** Closures within switch expression arms (arrow arms are yield statements) */
    @GroovyVersionStart("6.0.0")
    void testClosureInArm() {
        instrumentAndCompileWithGrover(["SwitchArmClosure.groovy": '''class SwitchArmClosure {
    static List<Integer> classify(int n, List<Integer> xs) {
        return switch (n) {
            case 1 -> xs.collect { it * 2 }
            default -> xs.collect { it * 3 }
        }
    }
    static void main(String[] args) {
        assert classify(1, [1, 2]) == [2, 4]
        assert classify(2, [1]) == [3]
    }
}'''])
        runWithAsserts("SwitchArmClosure")

        assertClassify("SwitchArmClosure.groovy", "SwitchArmClosure", { MethodInfo m ->
            assertStatement(m, at(4, 23, 4, 44), hits(1)) &&
            assertStatement(m, at(5, 24, 5, 45), hits(1)) &&
            assertStatement(m, at(4, 36, 4, 42), hits(2)) &&
            assertStatement(m, at(5, 37, 5, 43), hits(1))
        })
    }

    /**
     * val declarations and rest/map-key multi-assignment (dynamic): the parser and class generator rely on
     * modifiers and node metadata that must survive instrumentation
     */
    @GroovyVersionStart("6.0.0")
    void testValAndDestructuring() {
        instrumentAndCompileWithGrover(["Destructure.groovy": '''class Destructure {
    static String classify(List<Integer> xs, Map<String, Object> m) {
        val first = xs[0]
        def (h, *t) = xs
        def (name: n, age: a) = m
        return "$first $h $t $n $a"
    }
    static void main(String[] args) {
        assert classify([1, 2, 3], [name: 'Al', age: 3]) == '1 1 [2, 3] Al 3'
    }
}'''])
        runWithAsserts("Destructure")

        assertClassify("Destructure.groovy", "Destructure", { MethodInfo m ->
            m.hitCount == 1 && hitOnceOnLines(m, [3, 4, 5, 6])
        })
    }

    /**
     * val declarations and rest/map-key multi-assignment (static): the parser and class generator rely on
     * modifiers and node metadata that must survive instrumentation
     */
    @GroovyVersionStart("6.0.0")
    void testValAndDestructuringStatic() {
        instrumentAndCompileWithGrover(["DestructureStatic.groovy": '''@groovy.transform.CompileStatic class DestructureStatic {
    static String classify(List<Integer> xs, Map<String, Object> m) {
        val first = xs[0]
        def (h, *t) = xs
        def (name: n, age: a) = m
        return "$first $h $t $n $a"
    }
    static void main(String[] args) {
        assert classify([1, 2, 3], [name: 'Al', age: 3]) == '1 1 [2, 3] Al 3'
    }
}'''])
        runWithAsserts("DestructureStatic")

        assertClassify("DestructureStatic.groovy", "DestructureStatic", { MethodInfo m ->
            m.hitCount == 1 && hitOnceOnLines(m, [3, 4, 5, 6])
        })
    }

    /**
     * Opt-in closure packing (GEP-27) hoists closure bodies into methods of the enclosing class;
     * statements inside the instrumented closure body are still recorded
     */
    @GroovyVersionStart("6.0.0")
    void testPackedClosures() {
        instrumentAndCompileWithGrover(["Packed.groovy": '''@groovy.transform.PackedClosures
class Packed {
    static List<Integer> classify(List<Integer> xs) {
        return xs.collect { int x ->
            x * 2
        }
    }
    static void main(String[] args) {
        assert classify([1, 2, 3]) == [2, 4, 6]
    }
}'''])
        runWithAsserts("Packed")

        assertClassify("Packed.groovy", "Packed", { MethodInfo m ->
            m.hitCount == 1 &&
            m.statements.any { StatementInfo s -> s.startLine == 5 && s.hitCount == 3 }
        })
    }
}
