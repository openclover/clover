package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo

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
                                void testElvisWithPrimitiveByteType() {
                                    byte a = 1, b = 2
                                    byte c = a ?: (byte)(b * b)
                                }
                                void testElvisWithPrimitiveShortType() {
                                    short a = 1, b = 2
                                    short c = a ?: (short)(b * b)
                                }
                                void testElvisWithPrimitiveIntType() {
                                    int a = 1, b = 2
                                    int c = a ?: b * b
                                }
                                void testElvisWithPrimitiveLongType() {
                                    long a = 1, b = 2
                                    long c = a ?: b * b
                                }
                                void testElvisWithPrimitiveCharType() {
                                    char a = '1', b = '2'
                                    char c = a ?: b
                                }
                                void testElvisWithPrimitiveBooleanType() {
                                    boolean a = true, b = false
                                    boolean c = a ?: b || b
                                }
                                void testElvisWithPrimitiveFloatType() {
                                    float a = 0.0, b = 1.0
                                    float c = a ?: (float)(b * b)
                                }
                                void testElvisWithPrimitiveDoubleType() {
                                    double a = 0.0, b = 1.0
                                    double c = a ?: b * b
                                }
                                void testElvisWithNormalType() {
                                    String a = "1", b = "2"
                                    String c = a ?: b + b
                                }
                                void testElvisWithGenericType() {
                                    List<String> l = new ArrayList<String>();
                                    List<String> l2 = (l ?: l) ?: l
                                }
                            }
                        """
                ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("Elvis.groovy"), { FullFileInfo f ->
                    assertClass f, { ClassInfo it -> it.name == "Elvis" }, { FullClassInfo c ->
                        assertMethod(c, and(simplyNamed("testElvisWithDefType")), { MethodInfo m ->
                            assertBranch(m, at(4, 45, 4, 46), complexity(3))
                        }) &&

                        assertMethod(c, and(simplyNamed("testElvisWithPrimitiveByteType")), { MethodInfo m ->
                            assertBranch(m, at(8, 46, 8, 47), complexity(3))
                        }) &&

                        assertMethod(c, and(simplyNamed("testElvisWithPrimitiveShortType")), { MethodInfo m ->
                            assertBranch(m, at(12, 47, 12, 48), complexity(3))
                        }) &&

                        assertMethod(c, and(simplyNamed("testElvisWithPrimitiveIntType")), { MethodInfo m ->
                            assertBranch(m, at(16, 45, 16, 46), complexity(3))
                        }) &&

                        assertMethod(c, and(simplyNamed("testElvisWithPrimitiveLongType")), { MethodInfo m ->
                            assertBranch(m, at(20, 46, 20, 47), complexity(3))
                        }) &&

                        assertMethod(c, and(simplyNamed("testElvisWithPrimitiveCharType")), { MethodInfo m ->
                            assertBranch(m, at(24, 46, 24, 47), complexity(2))
                        }) &&

                        assertMethod(c, and(simplyNamed("testElvisWithPrimitiveBooleanType")), { MethodInfo m ->
                            assertBranch(m, at(28, 49, 28, 50), complexity(3))
                        }) &&

                        assertMethod(c, and(simplyNamed("testElvisWithPrimitiveFloatType")), { MethodInfo m ->
                            assertBranch(m, at(32, 47, 32, 48), complexity(3))
                        }) &&

                        assertMethod(c, and(simplyNamed("testElvisWithPrimitiveDoubleType")), { MethodInfo m ->
                            assertBranch(m, at(36, 48, 36, 49), complexity(3))
                        }) &&

                        assertMethod(c, and(simplyNamed("testElvisWithNormalType")), { MethodInfo m ->
                            assertBranch(m, at(40, 48, 40, 49), complexity(3))
                        }) // &&

                        assertMethod(c, and(simplyNamed("testElvisWithGenericType")), { MethodInfo m ->
                            assertBranch(m, at(44, 56, 44, 57), complexity(2)) // (>l< ?: l) ?: l
                            assertBranch(m, at(44, 55, 44, 63), complexity(3)) // >(l ?: l)< ?: l
                        })
                    }
                }
            }
        }
    }
}
