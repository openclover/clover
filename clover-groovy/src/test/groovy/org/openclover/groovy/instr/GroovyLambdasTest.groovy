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
                    assertClass(f, simplyNamed("LambdaJavaForms"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("field_add"), { MethodInfo m ->
                            m.statements.size() == 1
                        }) &&

                        assertMethod(c, simplyNamed("field_sub"), { MethodInfo m ->
                            m.statements.size() == 1
                        }) &&

                        assertMethod(c, simplyNamed("field_mult"), { MethodInfo m ->
                            m.statements.size() == 1
                        }) &&

                        assertMethod(c, simplyNamed("field_isEven"), { MethodInfo m ->
                            m.statements.size() == 1
                        }) &&

                        assertMethod(c, simplyNamed("field_theAnswer"), { MethodInfo m ->
                            m.statements.size() == 1
                        }) &&

                        assertMethod(c, simplyNamed("field_checkMath"), { MethodInfo m ->
                            m.statements.size() == 1
                        })
                    })
                }
            }
        }
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
                    assertClass(f, simplyNamed("LambdaWithDefaultParam"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("field_addWithDefault"), { MethodInfo m ->
                            m.statements.size() == 1
                        })
                    })
                }
            }
        }
    }

}
