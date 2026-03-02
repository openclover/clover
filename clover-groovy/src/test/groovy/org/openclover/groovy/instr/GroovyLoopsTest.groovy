package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.buildutil.test.junit.GroovyVersionStart
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo

@CompileStatic
class GroovyLoopsTest extends TestBase {

    GroovyLoopsTest(String testName) {
        super(testName)
    }

    GroovyLoopsTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    @GroovyVersionStart("3.0.0")
    void testDoWhileLoop() {
        instrumentAndCompileWithGrover([
                "DoWhileLoop.groovy":
                        '''class DoWhileLoop {
                    void loopMethod() {
                        int i = 0
                        do {
                            i++
                        } while (i < 5)
                    }
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("DoWhileLoop.groovy"), { FullFileInfo f ->
                    assertClass(f, named("DoWhileLoop"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->

                            assertStatement(m, at(3, 25, 3, 34), complexity(0)) && // int i
                                    assertStatement(m, at(4, 25, 6, 40), complexity(1)) && // do-while with condition
                                    assertStatement(m, at(4, 29, 4, 32), complexity(0)) && // i++
                                    assertBranch(m, at(6, 34, 6, 39)) && // i < 5
                                    m.statements.size() == 3 &&
                                    m.branches.size() == 1
                        })
                    })
                }
            }
        }
    }

    void testWhileLoop() {
        instrumentAndCompileWithGrover([
                "WhileLoop.groovy":
                        '''class WhileLoop {
                    void loopMethod() {
                        int i = 0
                        while (i < 5) {
                            i++
                        }
                    }
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("WhileLoop.groovy"), { FullFileInfo f ->
                    assertClass(f, named("WhileLoop"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->

                            assertStatement(m, at(3, 25, 3, 34), complexity(0)) && // int i
                                    assertStatement(m, at(4, 25, 6, 26), complexity(0)) && // while-do with condition
                                    assertStatement(m, at(5, 29, 5, 32), complexity(0)) && // i++
                                    assertBranch(m, at(4, 32, 4, 37)) && // i < 5
                                    m.statements.size() == 3 &&
                                    m.branches.size() == 1
                        })
                    })
                }
            }
        }
    }

    void testClassicForLoop() {
        instrumentAndCompileWithGrover([
                "ClassicForLoop.groovy":
                        '''class ClassicForLoop {
                    void loopMethod() {
                        for (int i = 0; i < 5; i++) {
                            println i
                        }
                    }
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("ClassicForLoop.groovy"), { FullFileInfo f ->
                    assertClass(f, named("ClassicForLoop"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->

                            assertStatement(m, at(3, 25, 5, 26), complexity(0)) && // for loop
                                    assertStatement(m, at(4, 29, 4, 38), complexity(0)) && // println i
                                    assertBranch(m, at(3, 41, 3, 46)) && // i < 5
                                    m.statements.size() == 2 &&
                                    m.branches.size() == 1
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testClassicForLoopWithCommaSeparatedExpressions() {
        instrumentAndCompileWithGrover([
                "ClassicForLoopWithCommas.groovy":
                        '''class ClassicForLoopWithCommas {
                    void loopMethod() {
                        for (int i = 0, j = 0; i < 5 && j < 100; i++, j++) {
                            println i, j
                        }
                    }
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("ClassicForLoopWithCommas.groovy"), { FullFileInfo f ->
                    assertClass(f, named("ClassicForLoopWithCommas"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->

                            assertStatement(m, at(4, 29, 4, 41), complexity(0)) && // println i, j
                                    assertStatement(m, at(3, 25, 5, 26), complexity(0)) && // for loop
                                    assertBranch(m, at(3, 48, 3, 64)) && // i < 5 && j < 100
                                    m.statements.size() == 2 &&
                                    m.branches.size() == 1
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testClassicForLoopWithMultiAssignment() {
        instrumentAndCompileWithGrover([
                "ClassicForLoopWithMultiAssignment.groovy":
                        '''class ClassicForLoopWithMultiAssignment {
                    void loopMethod() {
                        for (def (int i, int j) = [0, 0]; i < 5; i++) {
                            println i, j
                        }
                    }
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("ClassicForLoopWithMultiAssignment.groovy"), { FullFileInfo f ->
                    assertClass(f, named("ClassicForLoopWithMultiAssignment"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->

                            assertStatement(m, at(4, 29, 4, 41), complexity(0)) && // println i, j
                                    assertStatement(m, at(3, 25, 5, 26), complexity(0)) && // for loop
                                    assertBranch(m, at(3, 59, 3, 64)) && // i < 5
                                    m.statements.size() == 2 &&
                                    m.branches.size() == 1
                        })
                    })
                }
            }
        }
    }

    void testEnhancedForLoop() {
        instrumentAndCompileWithGrover([
                "EnhancedForLoop.groovy":
                        '''class EnhancedForLoop {
                    void loopMethod() {
                        for (def i in [1,2,3]) {
                            println i
                        }
                    }
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("EnhancedForLoop.groovy"), { FullFileInfo f ->
                    assertClass(f, named("EnhancedForLoop"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->

                            assertStatement(m, at(4, 29, 4, 38), complexity(0)) && // println i
                                    assertStatement(m, at(3, 25, 5, 26), complexity(0)) && // for loop
                                    m.statements.size() == 2 &&
                                    m.branches.size() == 0 // TODO the 'in' operator shall be treated as a branch
                        })
                    })
                }
            }
        }
    }

}
