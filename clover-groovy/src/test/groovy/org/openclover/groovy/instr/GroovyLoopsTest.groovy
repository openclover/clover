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
                    assertClass(f, simplyNamed("DoWhileLoop"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->
                            // Expect 1 statement for variable, 1 for do-while body, 1 for condition
                            m.statements.size() == 3 && m.branches.size() == 1
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
                    assertClass(f, simplyNamed("WhileLoop"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->
                            // Expect 1 statement for variable, 1 for while body, 1 for condition
                            m.statements.size() == 3 && m.branches.size() == 1
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
                    assertClass(f, simplyNamed("ClassicForLoop"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->
                            // 1 statement for 'for' body, 1 for condition
                            m.statements.size() == 1 && m.branches.size() == 1
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
                        for (int i = 0, int j = 0; i < 5, j < 100; i++, j++) {
                            println i, j
                        }
                    }
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("ClassicForLoop.groovy"), { FullFileInfo f ->
                    assertClass(f, simplyNamed("ClassicForLoopWithCommas"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->
                            // 1 statement for 'for' body, 1 for condition
                            m.statements.size() == 2 && m.branches.size() == 1
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
                    assertClass(f, simplyNamed("ClassicForLoopWithMultiAssignment"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->
                            // 1 statement for 'for' body, 1 for condition
                            m.statements.size() == 2 && m.branches.size() == 1
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
                    assertClass(f, simplyNamed("EnhancedForLoop"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("loopMethod"), { MethodInfo m ->
                            // Expect at least 1 statement for for body, 1 for collection
                            m.statements.size() == 2 && m.branches.size() == 1
                        })
                    })
                }
            }
        }
    }

}
