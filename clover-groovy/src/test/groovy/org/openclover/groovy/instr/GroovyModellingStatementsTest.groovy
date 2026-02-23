package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.buildutil.test.junit.GroovyVersionStart
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.groovy.test.junit.Result

/**
 * Integration tests that detect if the correct OpenClover model is generated for given Groovy code.
 **/
@CompileStatic
class GroovyModellingStatementsTest extends TestBase {

    GroovyModellingStatementsTest(String testName) {
        super(testName)
    }

    GroovyModellingStatementsTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    void testOnlyFieldsWithEmbeddedStatementsAddedToModel() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
             public class Foo {
                def myfieldWithCalculationInit = [{1 + 1}, {2 + 2}]
                def myfieldWithSimpleInit = 1 + 2
                def myfieldWithoutInit
             }
           """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    assertClass f, named("Foo"), { FullClassInfo c ->
                        assertMethod(c, { MethodInfo it -> it.simpleName == "field myfieldWithCalculationInit" }, { MethodInfo m ->
                            "".equals(m.signature.returnType) && m.signature.parameters.length == 0
                        }) &&
                                !c.methods.any(simplyNamed("myfieldWithSimpleInit")) &&
                                !c.methods.any(simplyNamed("myfieldWithoutInit"))
                    }
                }
            }
        }
    }

    void testAnonymousInnerClassInstr() {
        instrumentAndCompileWithGrover(
                ["org/openclover/foo/bar/Foo.groovy":
                         """
              package org.openclover.foo.bar

              new java.util.Timer().schedule(new java.util.TimerTask() {
                  def foo = Boolean.getBoolean('foo') ? new Object() : 2
                  void run() {
                      called = true
                  }
              }, 0)
              sleep 100

            """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, named("org.openclover.foo.bar"), { PackageInfo p ->
                (p.path == "org/openclover/foo/bar/") &&
                        assertFile(p, named("Foo.groovy")) { FullFileInfo f ->
                            (f.packagePath == "org/openclover/foo/bar/Foo.groovy") &&
                                    assertClass(f, named("script@Foo.groovy")) { FullClassInfo c ->
                                        (c.qualifiedName == "org.openclover.foo.bar.script@Foo.groovy") &&
                                                assertMethod(c, simplyNamed("script")) { MethodInfo m ->
                                                    assertEquals("there should be three statements including one in the inner class's method", 3, m.statements.size())
                                                    assertEquals("there should be one branch, the one in the field of the inner class", 1, m.branches.size())
                                                    true
                                                }
                                    }
                        }
            }
        }
    }

    /**
     * See CLOV-1466. After a fix we expect that:
     * 1. code will compile with no errors like "Non static method ... cannot be called from static context"
     * 2. code will run correctly with no "GroovyCastException: Cannot cast object X to ... CoverageRecorder"
     */
    void testCompileStaticWithClosuresAndImplicitThis() {
        Result compilationResult = instrumentAndCompileWithGrover([
                "A.groovy":
                        '''
                    import groovy.transform.CompileStatic
                    @CompileStatic
                    class A {
                        static void main(String[] args) {
                            List<String> myList = [ "Alice", "Bob", "Chris" ]
                            myList.each { String it ->
                                // test simple recorder call: A.$CLV_R$().inc(index)
                                print it
                                // test recorder in a branch: A.$CLV_R$().iget(index)
                                if (it == "Alice") println "!"
                                // test elvisEval()
                                print it ?: "empty"
                                // test safeEval()
                                print it?.length()
                                // test exprEval()
                                println ""
                            }
                        }
                    }''',
        ])
        assertTrue compilationResult.getStdErr() == null || compilationResult.getStdErr().length() == 0
        assertEquals 0, compilationResult.exitCode

        runWithAsserts("A")
    }

    /**
     * The https://jira.codehaus.org/browse/GROOVY-7041 was fixed in 2.3.8
     */
    @GroovyVersionStart("2.3.8")
    void testSpreadOperator() {
        instrumentAndCompileWithGrover([
                "A.groovy":
                        '''@groovy.transform.CompileStatic
                        class A {
                            A() {
                                List<String> myList = [ ]
                                if (myList*.isEmpty().any()) {
                                    println "hello"
                                }
                            }
                        }'''
        ])
    }

    void testComplexityOfBranches() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
             public class Foo {
               public static void main(String[] args) {
                 if (true || (false && true)) {
                 }
                 if ({ true || false }.call()) {
                 }
                 if (getClass()?.getName()) {
                 }
                 if ((getClass()?.getName() ?: "nothing") == "nothing") {
                 }
                 if ((getClass()?.getName() == null ? "nothing" : getClass()?.getName()) == "nothing") {
                 }
               }
             }
           """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    assertClass f, named("Foo"), { FullClassInfo c ->
                        assertMethod c, simplyNamed("main"), { MethodInfo m ->
                            assertBranch(m, at(4, 22, 4, 45), complexity(3)) &&
                                    assertBranch(m, at(6, 22, 6, 46), complexity(2)) &&
                                    assertBranch(m, at(8, 22, 8, 43), complexity(2)) &&
                                    assertBranch(m, at(10, 22, 10, 71), complexity(4)) &&
                                    assertBranch(m, at(12, 22, 12, 102), complexity(6))
                        }
                    }
                }
            }
        }
    }

//    @GroovyVersionStart("2.3.0")
//    @Ignore("CLOV-1960 instrument traits must be implemented")
//    void testStatementsInsideTraits() {
//        instrumentAndCompileWithGrover(["StatementsTrait.groovy":
//                                                '''
//                trait StatementsTrait {
//                    def z = 123
//                    def methodOne() {
//                        def b = 123
//                    }
//                    void methodTwo() {
//                        def c = 123
//                    }
//                }
//            '''])
//
//        assertRegistry db, { Clover2Registry reg ->
//            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
//                assertFile p, named("StatementsTrait.groovy"), { FullFileInfo f ->
//
//                    assertClass (f, { FullClassInfo c -> c.name == "StatementsTrait" }, { FullClassInfo c ->
//                        assertMethod(c, and(simplyNamed("field z"), at(0, 0, 0, 0), complexity(0)), { MethodInfo m ->
//                            m.statements.size() == 0 && m.branches.size() == 0
//                        }) &&
//
//                        assertMethod(c, and(simplyNamed("methodOne"), complexity(1)), { MethodInfo m ->
//                            m.statements.size() == 1 && m.branches.size() == 0 && assertStatement(m, at(0, 0, 0, 0), complexity(1))
//                        }) &&
//
//                        assertMethod(c, and(simplyNamed("methodTwo"), complexity(1)), { MethodInfo m ->
//                            m.statements.size() == 1 && m.branches.size() == 0 && assertStatement(m, at(0, 0, 0, 0), complexity(1))
//                        })
//                    })
//                }
//            }
//        }
//    }

    void testStatementsInsideConstructorsMethodsAndInitializerBlocks() {
        instrumentAndCompileWithGrover(["Statements.groovy":
            '''class StatementsClass {
                    def z = 123
                    StatementsClass() {
                        def a = 123
                    }
                    def methodOne() {
                        def b = 123
                    }
                    void methodTwo() {
                        def c = 123
                    }
                }
                enum StatementsEnum {
                    A, B, C
                    def z = 123
                    StatementsEnum() {
                        def a = 123
                    }
                    def methodOne() {
                        def b = 123
                    }
                    void methodTwo() {
                        def c = 123
                    }
                }
                interface StatementsInterface {
                    def z = 123
                    def methodOne();
                    void methodTwo();
                }
            '''])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("Statements.groovy"), { FullFileInfo f ->

                    assertClass (f, named("StatementsClass"), { FullClassInfo c ->
                        // an artificial method in which we keep field initializer
                        // they contain zero statements, hit counter is attached to a method
                        assertMethod(c, and(simplyNamed("field z"), at(2, 21, 2, 32), complexity(1)), { MethodInfo m ->
                            m.statements.size() == 0 && m.branches.size() == 0
                        }) &&
                        // a constructor
                        assertMethod(c, and(simplyNamed("<init>"), complexity(1)), { MethodInfo m ->
                            m.statements.size() == 1 && m.branches.size() == 0 && assertStatement(m, at(4, 25, 4, 36), complexity(0))
                        }) &&
                        // a regular method
                        assertMethod(c, and(simplyNamed("methodOne"), complexity(1)), { MethodInfo m ->
                            m.statements.size() == 1 && m.branches.size() == 0 && assertStatement(m, at(7, 25, 7, 36), complexity(0))
                        }) &&
                        // a regular method
                        assertMethod(c, and(simplyNamed("methodTwo"), complexity(1)), { MethodInfo m ->
                            m.statements.size() == 1 && m.branches.size() == 0 && assertStatement(m, at(10, 25, 10, 36), complexity(0))
                        })
                    }) &&

                    assertClass (f, named("StatementsEnum"), { FullClassInfo c ->
                        assertMethod(c, and(simplyNamed("field z"), at(15, 21, 15, 32), complexity(1)), { MethodInfo m ->
                            m.statements.size() == 0 && m.branches.size() == 0
                        }) &&
                        assertMethod(c, and(simplyNamed("<init>"), complexity(1)), { MethodInfo m ->
                            // note: enum has one extra statement to initalize it's fields (string, int); so size is 2 instead of 1
                            m.statements.size() == 2 && m.branches.size() == 0 && assertStatement(m, at(17, 25, 17, 36), complexity(0))
                        }) &&
                        assertMethod(c, and(simplyNamed("methodOne"), complexity(1)), { MethodInfo m ->
                            m.statements.size() == 1 && m.branches.size() == 0 && assertStatement(m, at(20, 25, 20, 36), complexity(0))
                        }) &&
                        assertMethod(c, and(simplyNamed("methodTwo"), complexity(1)), { MethodInfo m ->
                            m.statements.size() == 1 && m.branches.size() == 0 && assertStatement(m, at(23, 25, 23, 36), complexity(0))
                        })
                    }) &&

                    // note: we don't instrument interfaces so we don't have information about it's methods
                    assertClass (f, named("StatementsInterface"))
                }
            }
        }
    }

    void testCodeBlockNesting() {
        instrumentAndCompileWithGrover(["BlockNesting.groovy":
'''class BlockNesting {
    void methodOne() {
        int i = 0, j = 0;
        while (i < 10) {
            i++
            if (i < 5) {
                j++
            }
        }
        // blocks and closures
        BLOCK_A: {
            BLOCK_B: {
                BLOCK_C: {
                    int k = 0
                }
            }
        }
        { it ->
            { it2 ->
                { it3 ->
                    int l = 0
                }
            }
        }
    }
}'''], "-Dclover.grover.ast.dump=true")

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("BlockNesting.groovy"), { FullFileInfo f ->
                    assertClass f, named("BlockNesting"), { FullClassInfo c ->
                        assertMethod c, simplyNamed("methodOne"), { MethodInfo m ->
                            m.statements.size() == 14 &&
                            m.branches.size() == 2 &&                 // while + if

                            assertStatement(m, at(3, 9, 3, 18)) &&    // int i = 0
                            assertStatement(m, at(3, 20, 3, 25)) &&   // j = 0
                            assertStatement(m, at(5, 13, 5, 16)) &&   // i++
                            assertStatement(m, at(7, 17, 7, 20)) &&   // j++

                            assertStatement(m, at(11, 18, 17, 10)) && // BLOCK_A
                            assertStatement(m, at(12, 22, 16, 14)) && // BLOCK_B
                            assertStatement(m, at(13, 26, 15, 18)) && // BLOCK_C
                            assertStatement(m, at(14, 21, 14, 30)) && // int k = 0

                            assertStatement(m, at(18, 9, 24, 10)) && // closure 1
                            assertStatement(m, at(19, 13, 23, 14)) && // closure 2
                            assertStatement(m, at(20, 17, 22, 18)) && // closure 3
                            assertStatement(m, at(21, 21, 21, 30))    // int l = 0
                        }
                    }
                }
            }
        }
    }

    void testTryFinallyBlock() {
        instrumentAndCompileWithGrover(["TryFinally.groovy":
                '''class TryFinally {void one() {
                    try { }
                    finally { int i = 0 }
                }}'''])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("TryFinally.groovy"), { FullFileInfo f ->
                    assertClass f, named("TryFinally"), { FullClassInfo c ->
                        assertMethod c, simplyNamed("one"), { MethodInfo m ->
                            m.statements.size() == 2 &&
                            assertStatement(m, at(2, 21, 3, 42)) && // try-finally
                            assertStatement(m, or(at(3, 31, 3, 40), /*groovy2*/at(3, 31, 3, 41)))   // int i = 0
                        }
                    }
                }
            }
        }
    }

    /**
     * Groovy 3.0 introduced EmptyExpression.INSTANCE, for variable declaration when it has no immediate
     * assignment, that expression is immutable and can't be modified during instrumentation.
     * See https://github.com/openclover/clover/issues/121
     */
    void testEmptyExpressionInstance() {
        instrumentAndCompileWithGrover(["EmptyExpression.groovy":
                '''class EmptyExpression {void one() {
                    String[] list
                    int code
                    if (Math.random() > 0.5) {
                        list = ["a", "b", "c"]
                        code = 123
                    }
                }}'''])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("EmptyExpression.groovy"), { FullFileInfo f ->
                    assertClass f, named("EmptyExpression"), { FullClassInfo c ->
                        assertMethod c, simplyNamed("one"), { MethodInfo m ->
                            m.statements.size() == 5 &&
                                    assertStatement(m, at(2, 21, 2, 34)) && // 'String[] list'
                                    assertStatement(m, at(3, 21, 3, 29))    // 'int code'
                        }
                    }
                }
            }
        }
    }

}
