package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.groovy.test.junit.Result

@CompileStatic
class GroovySafeEvalTest extends TestBase {

    GroovySafeEvalTest(String testName) {
        super(testName)
    }

    GroovySafeEvalTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    /**
     * Test whether our wrapper for the safe navigation operator (?.) returns correct type (and not just an Object)
     * so that the @CompileStatic transformation can properly determine exact types (instead of relying on Groovy's
     * meta-object protocol).
     */
    void testSafeEvalWithCompileStatic() {
        safeEvalAndCompileStatic(true)
    }

    /**
     * Test whether our wrapper for the safe navigation operator (?.) returns correct type (and not just an Object)
     * and whether groovyc is able to match a method signature. Should work in all groovy versions.
     */
    void testSafeEvalWithoutCompileStatic() {
        safeEvalAndCompileStatic(false)
    }

    protected void safeEvalAndCompileStatic(boolean withCompileStatic) {
        instrumentAndCompileWithGrover(
                [
                        "C.groovy":
                                (withCompileStatic ? "@groovy.transform.CompileStatic " : "") +
                                        """class C {
                                class A { }
                                class B { A a }
                                class D {
                                    String getValue() { return null }
                                }

                                void testSafeEvalWithField() {
                                    B b = new B()
                                    A a = b?.a
                                }
                                void testSafeEvalWithClosure() {
                                    Object o = { println "hello" }?.getOwner()
                                }
                                void testSafeEvalWithMethod() {
                                    String s = "abc"?.toString()
                                }
                                void testSafeEvalWithProperty() {
                                    D d = new D();
                                    String v = d?.value
                                }
                            }
                        """
                ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("C.groovy"), { FullFileInfo f ->
                    assertClass f, { ClassInfo it -> it.name == "C" }, { FullClassInfo c ->
                        assertMethod(c, and(simplyNamed("testSafeEvalWithField")), { MethodInfo m ->
                            assertBranch(m, at(10, 43, 10, 44), complexity(1))
                        }) &&

                                assertMethod(c, and(simplyNamed("testSafeEvalWithClosure")), { MethodInfo m ->
                                    assertBranch(m, at(13, 48, 13, 67), complexity(1))
                                }) &&

                                assertMethod(c, and(simplyNamed("testSafeEvalWithMethod")), { MethodInfo m ->
                                    assertBranch(m, at(16, 48, 16, 53), complexity(1))
                                }) &&

                                assertMethod(c, and(simplyNamed("testSafeEvalWithProperty")), { MethodInfo m ->
                                    assertBranch(m, at(20, 48, 20, 49), complexity(1))
                                })
                    }
                }
            }
        }
    }

    void testEvalSafelyWithGenericTypes() {
        Result compilationResult = instrumentAndCompileWithGrover(([
                "B.groovy":
                        '''
                class B {
                    private static <T extends CharSequence> T testMethod(T charSequence) {
                        return (T) charSequence?.subSequence(0, 1)
                    }
                    public static void main(String[] args) {
                        def myDate = "my beauty string"
                        B.testMethod()
                    }
                }
            '''
        ]))

        assertTrue compilationResult.getStdErr() == null || compilationResult.getStdErr().length() == 0
        assertEquals 0, compilationResult.exitCode
        runWithAsserts("B")
    }

    void testEvalSafelyWithGenericTypesClosure() {
        Result compilationResult = instrumentAndCompileWithGrover(([
                "A.groovy":
                        '''
                class A {
                    private static <T> T testMethod(Closure<T> closure) {
                        return (T) closure?.call()
                    }
                    public static void main(String[] args) {
                        A.testMethod({})
                    }
                }
            '''
        ]))

        assertTrue compilationResult.getStdErr() == null || compilationResult.getStdErr().length() == 0
        assertEquals 0, compilationResult.exitCode
        runWithAsserts("A")
    }
}
