package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.core.CloverDatabase
import org.openclover.core.CodeType
import org.openclover.core.CoverageDataSpec
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo

/**
 * Integration tests verifying runtime hit counts recorded by Clover instrumentation.
 * Groovy 2+ constructs: classic for loop, while loop, enhanced for loop, if/else, try/catch/finally.
 *
 * Column positions in at(line, startCol, endLine, endCol) are 1-indexed; endCol is exclusive.
 * They match the actual whitespace present in the Groovy source strings below.
 */
@CompileStatic
class Groovy2CoverageRecordingTest extends TestBase {

    Groovy2CoverageRecordingTest(String testName) {
        super(testName)
    }

    Groovy2CoverageRecordingTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    // -----------------------------------------------------------------------
    // Classic for loop
    //
    // Source layout (1-indexed columns):
    //   line 3:  "        for (int i = 0; i < 3; i++) {"
    //             col 9 = 'f'  (for keyword)
    //             col 25 = 'i' (start of branch "i < 3"), col 30 exclusive (5 chars)
    //   line 4:  "            println i"
    //             col 13 = 'p', col 22 exclusive ("println i" = 9 chars)
    //   line 5:  "        }"  → for statement ends: col 9, col 10 exclusive
    // -----------------------------------------------------------------------
    void testClassicForLoopHitCounts() {
        instrumentAndCompileWithGrover(["ForLoop.groovy": '''class ForLoop {
    static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            println i
        }
    }
}'''])
        runWithAsserts("ForLoop")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("ForLoop.groovy"), { FullFileInfo f ->
                        assertClass f, named("ForLoop"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                m.statements.size() == 2 &&
                                m.branches.size() == 1 &&
                                // for statement itself is entered once
                                assertStatement(m, at(3, 9, 5, 10), hits(1)) &&
                                // loop body: 3 iterations (i = 0, 1, 2)
                                assertStatement(m, at(4, 13, 4, 22), hits(3)) &&
                                // condition "i < 3": true 3 times (continues), false 1 time (exits at i=3)
                                assertBranch(m, at(3, 25, 3, 30), hits(3, 1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // While loop
    //
    //   line 3:  "        int count = 0"          col 9, end 23 (14 chars)
    //   line 4:  "        while (count < 2) {"
    //             col 9 = 'w', col 16 = 'c' (start of "count < 2"), col 25 exclusive (9 chars)
    //   line 5:  "            count++"            col 13, end 20 (7 chars)
    //   line 6:  "        }"                      col 9, col 10 exclusive
    // -----------------------------------------------------------------------
    void testWhileLoopHitCounts() {
        instrumentAndCompileWithGrover(["WhileLoop.groovy": '''class WhileLoop {
    static void main(String[] args) {
        int count = 0
        while (count < 2) {
            count++
        }
    }
}'''])
        runWithAsserts("WhileLoop")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("WhileLoop.groovy"), { FullFileInfo f ->
                        assertClass f, named("WhileLoop"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                m.statements.size() == 3 &&
                                m.branches.size() == 1 &&
                                assertStatement(m, at(3, 9, 3, 22), hits(1)) &&     // int count = 0 (13 chars)
                                assertStatement(m, at(4, 9, 6, 10), hits(1)) &&     // while statement
                                // body: 2 iterations (count = 0 → 1 → 2)
                                assertStatement(m, at(5, 13, 5, 20), hits(2)) &&
                                // condition "count < 2": true 2 times, false 1 time (exits at count=2)
                                assertBranch(m, at(4, 16, 4, 25), hits(2, 1))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Enhanced for loop
    //
    //   line 3:  "        for (def x in [1, 2, 3, 4]) {"  col 9 = 'f'
    //   line 4:  "            println x"   col 13, end 22 (9 chars)
    //   line 5:  "        }"               col 9, col 10 exclusive
    // -----------------------------------------------------------------------
    void testEnhancedForLoopHitCounts() {
        instrumentAndCompileWithGrover(["EnhancedFor.groovy": '''class EnhancedFor {
    static void main(String[] args) {
        for (def x in [1, 2, 3, 4]) {
            println x
        }
    }
}'''])
        runWithAsserts("EnhancedFor")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("EnhancedFor.groovy"), { FullFileInfo f ->
                        assertClass f, named("EnhancedFor"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                m.statements.size() == 2 &&
                                m.branches.size() == 0 &&
                                assertStatement(m, at(3, 9, 5, 10), hits(1)) &&
                                // body: executed 4 times — one per list element
                                assertStatement(m, at(4, 13, 4, 22), hits(4))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // If / else branches
    //
    //   line 3:  "        if (1 + 1 == 2) {"
    //             col 9 = 'i', col 13 = '1' (start of condition), col 23 exclusive (10 chars)
    //   line 4:  "            println "true""    col 13, end 27 (14 chars)
    //   line 6:  "            println "false""   col 13, end 28 (15 chars)
    //   line 7:  "        }"  col 9, col 10 exclusive → if/else statement ends
    // -----------------------------------------------------------------------
    void testIfElseHitCounts() {
        instrumentAndCompileWithGrover(["IfElse.groovy": '''class IfElse {
    static void main(String[] args) {
        if (1 + 1 == 2) {
            println "true"
        } else {
            println "false"
        }
    }
}'''])
        runWithAsserts("IfElse")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("IfElse.groovy"), { FullFileInfo f ->
                        assertClass f, named("IfElse"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                m.statements.size() == 3 &&
                                m.branches.size() == 1 &&
                                assertStatement(m, at(3, 9, 7, 10), hits(1)) &&     // if statement
                                assertStatement(m, at(4, 13, 4, 27), hits(1)) &&    // true branch: taken
                                assertStatement(m, at(6, 13, 6, 28), hits(0)) &&    // false branch: not taken
                                // condition "1 + 1 == 2": always true
                                assertBranch(m, at(3, 13, 3, 23), hits(1, 0))
                            })
                        }
                    }
                })
    }

    // -----------------------------------------------------------------------
    // Try / catch / finally
    //
    //   line 3:  "        try {"                  col 9 = 't'
    //   line 4:  "            throw new RuntimeException("boom")"
    //             col 13, end 47 ("throw new RuntimeException("boom")" = 34 chars)
    //   line 6:  "            println "caught""   col 13, end 29 (16 chars)
    //   line 8:  "            println "finally""  col 13, end 30 (17 chars)
    //   line 9:  "        }"  col 9, col 10 exclusive → try block ends
    // -----------------------------------------------------------------------
    void testTryCatchFinallyHitCounts() {
        instrumentAndCompileWithGrover(["TryCatch.groovy": '''class TryCatch {
    static void main(String[] args) {
        try {
            throw new RuntimeException("boom")
        } catch (RuntimeException e) {
            println "caught"
        } finally {
            println "finally"
        }
    }
}'''])
        runWithAsserts("TryCatch")

        assertPackage(
                CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION),
                isDefaultPackage,
                { PackageInfo p ->
                    assertFile p, named("TryCatch.groovy"), { FullFileInfo f ->
                        assertClass f, named("TryCatch"), { FullClassInfo c ->
                            assertMethod(c, simplyNamed("main"), { MethodInfo m ->
                                m.hitCount == 1 &&
                                assertStatement(m, at(3, 9, 9, 10), hits(1)) &&     // try block
                                assertStatement(m, at(4, 13, 4, 47), hits(1)) &&    // throw
                                assertStatement(m, at(6, 13, 6, 29), hits(1)) &&    // println "caught"
                                assertStatement(m, at(8, 13, 8, 30), hits(1))       // println "finally"
                            })
                        }
                    }
                })
    }

}
