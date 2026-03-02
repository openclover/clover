package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.buildutil.test.junit.GroovyVersionStart
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo

@CompileStatic
class GroovyLambdasTest extends TestBase {
    GroovyLambdasTest(String testName) {
        super(testName)
    }

    GroovyLambdasTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    @GroovyVersionStart("3.0.0")
    void testDifferentJavaLikeLambdaForms() {
        instrumentAndCompileWithGrover([
                "LambdaJavaForms.groovy":
                        '''class LambdaJavaForms {
                    // types and braces
                    def add = (int x, int y) -> { def z = y; return x + z }
                    // single expression, no braces
                    def sub = (int x, int y) -> x - y
                    // no types, single expression
                    def mult = (x, y) -> x * y
                    // single param, no types, no parens
                    def isEven = n -> n % 2 == 0
                    // no args, just return value
                    def theAnswer = () -> 42
                    // lambda with statement block
                    def checkMath = () -> { assert 1 + 1 == 2 }
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("LambdaJavaForms.groovy"), { FullFileInfo f ->
                    assertClass(f, named("LambdaJavaForms"), { FullClassInfo c ->

                        assertField_add(c) &&
                                assertField_sub(c) &&
                                assertField_mult(c) &&
                                assertField_isEven(c) &&
                                assertField_theAnswer(c) &&
                                assertField_checkMath(c)
                    })
                }
            }
        }
    }

    private boolean assertField_add(FullClassInfo c) {
        return assertMethod(c, simplyNamed("field add"), { MethodInfo m ->

            m.statements.size() == 2 &&
                    assertStatement(m, at(3, 51, 3, 60)) && // def z = y
                    assertStatement(m, at(3, 62, 3, 74)) // return x + z

        })
    }

    private boolean assertField_sub(FullClassInfo c) {
        assertMethod(c, simplyNamed("field sub"), { MethodInfo m ->
            m.statements.size() == 1 &&
                    assertStatement(m, at(5, 49, 5, 54)) // x - y

        })
    }

    private boolean assertField_mult(FullClassInfo c) {
        assertMethod(c, simplyNamed("field mult"), { MethodInfo m ->
            m.statements.size() == 1 &&
                    assertStatement(m, at(7, 42, 7, 47)) // x * y
        })
    }

    private boolean assertField_isEven(FullClassInfo c) {
        assertMethod(c, simplyNamed("field isEven"), { MethodInfo m ->
            m.statements.size() == 1 &&
                    assertStatement(m, at(9, 39, 9, 49)) // n % 2 == 0
        })
    }

    private boolean assertField_theAnswer(FullClassInfo c) {
        assertMethod(c, simplyNamed("field theAnswer"), { MethodInfo m ->
            m.statements.size() == 1 &&
                    assertStatement(m, at(11, 43, 11, 45)) // 42
        })
    }

    private boolean assertField_checkMath(FullClassInfo c) {
        assertMethod(c, simplyNamed("field checkMath"), { MethodInfo m ->
            m.statements.size() == 1 &&
                    assertStatement(m, at(13, 45, 13, 62)) // assert 1 + 1 == 2
        })
    }

    @GroovyVersionStart("3.0.0")
    void testLambdaWithDefaultParam() {
        // note: there's no java equivalent for default params
        instrumentAndCompileWithGrover([
                "LambdaWithDefaultParam.groovy":
                        '''class LambdaWithDefaultParam {
                    def addWithDefault = (int x, int y = 100) -> x + y
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("LambdaWithDefaultParam.groovy"), { FullFileInfo f ->
                    assertClass(f, named("LambdaWithDefaultParam"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("field addWithDefault"), { MethodInfo m ->
                            m.statements.size() == 1 &&
                                    assertStatement(m, at(2, 66, 2, 71)) // x + y
                        })
                    })
                }
            }
        }
    }

}
