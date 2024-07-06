package org.openclover.groovy.instr

import groovy.transform.CompileStatic

@CompileStatic
class GroovyElvisOperatorTest extends TestBase {
    GroovyElvisOperatorTest(String testName) {
        super(testName)
    }

    GroovyElvisOperatorTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    void testElvisOperatorWithCompileStatic() {
        elvisOperatorAndCompileStatic(true)
    }

    void testElvisOperatorWithoutCompileStatic() {
        elvisOperatorAndCompileStatic(false)
    }

    private void elvisOperatorAndCompileStatic(boolean withCompileStatic) {
        instrumentAndCompileWithGrover(
                [
                        "Elvis.groovy":
                                (withCompileStatic ? "@groovy.transform.CompileStatic " : "") +
                                        """class Elvis {
                                void testElvisWithDefType() {
                                    def a = 1, b = 2
                                    def c = a ?: b * b
                                }
                                void testElvisWithPrimitiveType() {
                                    int a = 1, b = 2
                                    int c = a ?: b * b
                                }
                                void testElvisWithNormalType() {
                                    String a = "1", b = "2"
                                    String c = a ?: b + b
                                }
                                void testElvisWithGenericType() {
                                    List<String> l = new ArrayList<String>();
                                    List<String> l2 = l ?: l
                                }
                            }
                        """
                ])
//
//        assertRegistry db, { Clover2Registry reg ->
//            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
//                assertFile p, named("C.groovy"), { FullFileInfo f ->
//                    assertClass f, { ClassInfo it -> it.name == "C" }, { FullClassInfo c ->
//                        assertMethod(c, and(simplyNamed("testSafeEvalWithField")), { MethodInfo m ->
//                            assertBranch(m, at(10, 43, 10, 44), complexity(1))
//                        }) &&
//
//                                assertMethod(c, and(simplyNamed("testSafeEvalWithClosure")), { MethodInfo m ->
//                                    assertBranch(m, at(13, 48, 13, 67), complexity(1))
//                                }) &&
//
//                                assertMethod(c, and(simplyNamed("testSafeEvalWithMethod")), { MethodInfo m ->
//                                    assertBranch(m, at(16, 48, 16, 53), complexity(1))
//                                }) &&
//
//                                assertMethod(c, and(simplyNamed("testSafeEvalWithProperty")), { MethodInfo m ->
//                                    assertBranch(m, at(20, 48, 20, 49), complexity(1))
//                                })
//                    }
//                }
//            }
//        }
    }
}
