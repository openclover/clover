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
 * Integration tests verifying runtime hit counts recorded by Clover instrumentation.
 *
 * Groovy 3+ constructs (annotated with @GroovyVersionStart): do-while, lambda expressions,
 *   method references, and mixed stream chains that contain both.
 *
 * Column positions in at(line, startCol, endLine, endCol) are 1-indexed; endCol is exclusive.
 * They match the actual whitespace present in the Groovy source strings below.
 */
@CompileStatic
class Groovy3CoverageRecordingTest extends TestBase {

    Groovy3CoverageRecordingTest(String testName) {
        super(testName)
    }

    Groovy3CoverageRecordingTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    // -----------------------------------------------------------------------
    // Do-while loop (Groovy 3+)
    //
    //   line 3:  "        int i = 0"              col 9, end 18 ("int i = 0" = 9 chars)
    //   line 4:  "        do {"                   col 9 = 'd'
    //   line 5:  "            i++"                col 13, end 16 ("i++" = 3 chars)
    //   line 6:  "        } while (i < 3)"
    //             col 9 = '}'; "} while (i < 3)" = 15 chars → end col 24
    //             col 18 = 'i' (start of "i < 3" = 5 chars → end 23)
    // -----------------------------------------------------------------------
    @GroovyVersionStart("3.0.0")
    void testDoWhileLoopHitCounts() {
        instrumentAndCompileWithGrover(["DoWhile.groovy": '''class DoWhile {
    static void main(String[] args) {
        int i = 0
        do {
            i++
        } while (i < 3)
        assert i == 3
    }
}'''])
        runWithAsserts("DoWhile")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("DoWhile.groovy"), { FullFileInfo f ->
                        assertClass f, named("DoWhile"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                m.branches.size() == 1 &&
                                assertStatement(m, at(3, 9, 3, 18), hits(1)) &&     // int i = 0
                                assertStatement(m, at(4, 9, 6, 24), hits(1)) &&     // do-while block
                                // body: 3 iterations (i goes 0→1→2→3)
                                assertStatement(m, at(5, 13, 5, 16), hits(3)) &&
                                // condition "i < 3": true 2 times (continue), false 1 time (exit at i=3)
                                assertBranch(m, at(6, 18, 6, 23), hits(2, 1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Lambda expression body hit counts (Groovy 3+)
    //
    // filter's lambda "(Integer n) -> n > 1" is applied to all 3 elements [1,2,3];
    // its body expression "n > 1" is a separate statement within main() and is
    // hit once per element tested — 3 times total.
    // Two elements pass (2 and 3), so the stream's count() returns 2.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("3.0.0")
    void testLambdaBodyHitCounts() {
        instrumentAndCompileWithGrover(["LambdaRuntime.groovy": '''class LambdaRuntime {
    static void main(String[] args) {
        def nums = [1, 2, 3]
        long count = nums.stream().filter((Integer n) -> n > 1).count()
        assert count == 2
    }
}'''])
        runWithAsserts("LambdaRuntime")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("LambdaRuntime.groovy"), { FullFileInfo f ->
                        assertClass f, named("LambdaRuntime"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                // 4 statements: def nums, long count (outer), lambda body "n > 1", assert
                                m.hitCount == 1 &&
                                m.statements.size() == 4 &&
                                // lambda body "n > 1" hit 3 times — once per element in [1, 2, 3]
                                assertStatement(m, { StatementInfo s -> s.hitCount == 3 }, hits(3)) &&
                                // the other 3 statements (def nums, outer line, assert) are hit once
                                m.statements.findAll { StatementInfo s -> s.hitCount == 1 }.size() == 3
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Lambda with partial stream: filter then map (Groovy 3+)
    //
    // filter body "(Integer n) -> n % 2 == 0" is checked for all 5 elements → hit 5 times.
    // map lambda "(Integer n) -> n * 10" is applied only to the 2 even elements → hit 2 times.
    // Verifies that consecutive lambdas in a chain have independent hit counts.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("3.0.0")
    void testLambdaBodyHitCountsWithPartialFilter() {
        instrumentAndCompileWithGrover(["LambdaFilter.groovy": '''class LambdaFilter {
    static void main(String[] args) {
        def nums = [1, 2, 3, 4, 5]
        def result = nums.stream()
                .filter((Integer n) -> n % 2 == 0)
                .map((Integer n) -> n * 10)
                .toList()
        assert result == [20, 40]
    }
}'''])
        runWithAsserts("LambdaFilter")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("LambdaFilter.groovy"), { FullFileInfo f ->
                        assertClass f, named("LambdaFilter"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                // 5 statements: def nums, def result (outer), filter body, map body, assert
                                m.hitCount == 1 &&
                                m.statements.size() == 5 &&
                                // filter body: applied to all 5 elements
                                assertStatement(m, { StatementInfo s -> s.hitCount == 5 }, hits(5)) &&
                                // map body: applied only to the 2 even elements (2, 4)
                                assertStatement(m, { StatementInfo s -> s.hitCount == 2 }, hits(2))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Method reference — unbound instance call: Integer::toString (Groovy 3+)
    //
    // Dynamic path: buildDynamicClosure generates:
    //   { Object[] $CLV_args$ -> R.inc(N); (Integer.&"toString").call($CLV_args$) }
    // The closure is invoked once per stream element.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("3.0.0")
    void testUnboundInstanceMethodReferenceHitCounts() {
        instrumentAndCompileWithGrover(["InstanceMethodRef.groovy": '''class InstanceMethodRef {
    static void main(String[] args) {
        def nums = [1, 2, 3]
        def strs = nums.stream().map(Integer::toString).toList()
        assert strs == ["1", "2", "3"]
    }
}'''])
        runWithAsserts("InstanceMethodRef")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("InstanceMethodRef.groovy"), { FullFileInfo f ->
                        assertClass f, named("InstanceMethodRef"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                // 4 statements: def nums, def strs (outer), method-ref closure, assert
                                m.hitCount == 1 &&
                                m.statements.size() == 4 &&
                                // closure invoked once per element in [1, 2, 3]
                                assertStatement(m, { StatementInfo s -> s.hitCount == 3 }, hits(3)) &&
                                m.statements.findAll { StatementInfo s -> s.hitCount == 1 }.size() == 3
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Method reference — empty stream: hit count must be 0 (not 1)
    // -----------------------------------------------------------------------
    @GroovyVersionStart("3.0.0")
    void testMethodReferenceHitCountZeroOnEmptyStream() {
        instrumentAndCompileWithGrover(["EmptyStreamMethodRef.groovy": '''class EmptyStreamMethodRef {
    static void main(String[] args) {
        def nums = []
        def strs = nums.stream().map(Integer::toString).toList()
        assert strs.isEmpty()
    }
}'''])
        runWithAsserts("EmptyStreamMethodRef")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("EmptyStreamMethodRef.groovy"), { FullFileInfo f ->
                        assertClass f, named("EmptyStreamMethodRef"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                m.statements.size() == 4 &&
                                // stream is empty → closure body never invoked → hitCount == 0
                                assertStatement(m, { StatementInfo s -> s.hitCount == 0 }, hits(0))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Method reference — static call: Integer::toBinaryString (Groovy 3+)
    //
    // Dynamic path: buildDynamicClosure generates:
    //   { Object[] $CLV_args$ -> R.inc(N); (Integer.&"toBinaryString").call($CLV_args$) }
    // MethodClosure dispatches statically at runtime.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("3.0.0")
    void testStaticMethodReferenceHitCounts() {
        instrumentAndCompileWithGrover(["StaticMethodRef.groovy": '''class StaticMethodRef {
    static void main(String[] args) {
        def nums = [5, 3, 7]
        def bins = nums.stream().map(Integer::toBinaryString).toList()
        assert bins == ["101", "11", "111"]
    }
}'''])
        runWithAsserts("StaticMethodRef")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("StaticMethodRef.groovy"), { FullFileInfo f ->
                        assertClass f, named("StaticMethodRef"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                // 4 statements: def nums, def bins (outer), static-method-ref closure, assert
                                m.hitCount == 1 &&
                                m.statements.size() == 4 &&
                                // closure invoked once per element in [5, 3, 7]
                                assertStatement(m, { StatementInfo s -> s.hitCount == 3 }, hits(3)) &&
                                m.statements.findAll { StatementInfo s -> s.hitCount == 1 }.size() == 3
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Method reference — constructor call: Random::new (Groovy 3+)
    //
    // Dynamic path: buildDynamicClosure generates:
    //   { Object[] $CLV_args$ -> R.inc(N); (Random.&"new").call($CLV_args$) }
    // Used as Function<Integer,Random> (stream element = seed), invoked 3 times.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("3.0.0")
    void testConstructorReferenceHitCounts() {
        instrumentAndCompileWithGrover(["ConstructorRef.groovy": '''class ConstructorRef {
    static void main(String[] args) {
        def seeds = [1L, 2L, 3L]
        def randoms = seeds.stream().map(Random::new).toList()
        assert randoms.size() == 3
        assert randoms[0] instanceof Random
    }
}'''])
        runWithAsserts("ConstructorRef")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("ConstructorRef.groovy"), { FullFileInfo f ->
                        assertClass f, named("ConstructorRef"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                // 5 statements: def seeds, def randoms (outer), ctor-ref closure, assert size, assert[0]
                                m.hitCount == 1 &&
                                m.statements.size() == 5 &&
                                // closure invoked once per element in [1L, 2L, 3L]
                                assertStatement(m, { StatementInfo s -> s.hitCount == 3 }, hits(3)) &&
                                m.statements.findAll { StatementInfo s -> s.hitCount == 1 }.size() == 4
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Bound method reference: s::toUpperCase (Groovy 3+)
    //
    // Bound refs have a non-ClassExpression receiver and cannot be safely wrapped.
    // They must NOT register a separate statement — the outer declaration statement
    // is recorded but the bound ref itself is skipped.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("3.0.0")
    void testBoundMethodReferenceNotInstrumented() {
        instrumentAndCompileWithGrover(["BoundMethodRef.groovy": '''class BoundMethodRef {
    static void main(String[] args) {
        String s = "hello"
        def fn = s::toUpperCase
        assert fn() == "HELLO"
    }
}'''])
        runWithAsserts("BoundMethodRef")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("BoundMethodRef.groovy"), { FullFileInfo f ->
                        assertClass f, named("BoundMethodRef"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                // 3 statements: String s = ..., def fn = s::toUpperCase, assert
                                // s::toUpperCase is a bound ref — no separate sub-statement registered
                                m.hitCount == 1 &&
                                m.statements.size() == 3 &&
                                m.statements.findAll { StatementInfo s -> s.hitCount == 1 }.size() == 3
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Method reference — array constructor: String[]::new (Groovy 3+)
    //
    // Dynamic path: buildDynamicClosure generates:
    //   { Object[] $CLV_args$ -> R.inc(N); (String[].&"new").call($CLV_args$) }
    // Applied to 3 sizes → closure hit 3 times.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("3.0.0")
    void testArrayConstructorReferenceHitCounts() {
        instrumentAndCompileWithGrover(["ArrayConstructorRef.groovy": '''class ArrayConstructorRef {
    static void main(String[] args) {
        def sizes = [2, 3, 4]
        def arrays = sizes.stream().map(String[]::new).toList()
        assert arrays[0].length == 2
        assert arrays[1].length == 3
        assert arrays[2].length == 4
    }
}'''])
        runWithAsserts("ArrayConstructorRef")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("ArrayConstructorRef.groovy"), { FullFileInfo f ->
                        assertClass f, named("ArrayConstructorRef"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                // 6 statements: def sizes, def arrays (outer), array-ctor closure,
                                //               assert[0], assert[1], assert[2]
                                m.hitCount == 1 &&
                                // array-constructor closure invoked once per element in [2, 3, 4]
                                assertStatement(m, { StatementInfo s -> s.hitCount == 3 }, hits(3))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Mixed chain: lambda + method reference (Groovy 3+)
    //
    // filter lambda body "(Integer n) -> n % 2 == 0": checked for all 5 elements → 5 hits.
    // map method reference Integer::toString closure: applied to the 2 even elements → 2 hits.
    // Verifies independent hit counters for nested statements in the same chain.
    // -----------------------------------------------------------------------
    @GroovyVersionStart("3.0.0")
    void testLambdaAndMethodReferenceInSameChainHitCounts() {
        instrumentAndCompileWithGrover(["StreamChain.groovy": '''class StreamChain {
    static void main(String[] args) {
        def nums = [1, 2, 3, 4, 5]
        def result = nums.stream().filter((Integer n) -> n % 2 == 0).map(Integer::toString).toList()
        assert result == ["2", "4"]
    }
}'''])
        runWithAsserts("StreamChain")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("StreamChain.groovy"), { FullFileInfo f ->
                        assertClass f, named("StreamChain"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                // 5 statements: def nums, def result (outer), filter-lambda body,
                                //               method-ref closure, assert
                                m.hitCount == 1 &&
                                m.statements.size() == 5 &&
                                // filter lambda body: all 5 elements tested
                                assertStatement(m, { StatementInfo s -> s.hitCount == 5 }, hits(5)) &&
                                // method reference closure: only 2 elements pass the filter (2, 4)
                                assertStatement(m, { StatementInfo s -> s.hitCount == 2 }, hits(2)) &&
                                m.statements.findAll { StatementInfo s -> s.hitCount == 1 }.size() == 3
                            })
                        }
                    }
                })
    }
}
