package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.buildutil.test.junit.GroovyVersionStart
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo

@CompileStatic
class GroovyOperatorsTest extends TestBase {
    GroovyOperatorsTest(String testName) {
        super(testName)
    }

    GroovyOperatorsTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }


    /**
     * The https://jira.codehaus.org/browse/GROOVY-7041 was fixed in 2.3.8
     */
    @GroovyVersionStart("2.3.8")
    void testSpreadOperator() {
        // test '*.'
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

    @GroovyVersionStart("3.0.0")
    void testNotInOperator() {
        // test !in
        instrumentAndCompileWithGrover([
                "NotInOperator.groovy":
                        '''
            class NotInOperator {
                static boolean test() {
                    def nums = [1, 2, 3]
                    return 4 !in nums
                }
            }
            '''
        ])
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("NotInOperator.groovy"), { FullFileInfo f ->
                    assertClass(f, simplyNamed("NotInOperator"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("test"), { MethodInfo m ->
                            m.statements.size() == 2
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testNotInstanceOfOperator() {
        // test !instanceof
        instrumentAndCompileWithGrover([
                "NotInstanceOfOperator.groovy":
                        '''
            class NotInstanceOfOperator {
                static boolean test() {
                    def obj = "hello"
                    return !(obj instanceof Integer)
                }
            }
            '''
        ])
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("NotInstanceOfOperator.groovy"), { FullFileInfo f ->
                    assertClass(f, simplyNamed("NotInstanceOfOperator"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("test"), { MethodInfo m ->
                            m.statements.size() == 2
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testElvisAssignmentOperator() {
        // test '?='
        instrumentAndCompileWithGrover([
                "ElvisAssignmentOperator.groovy":
                        '''
            class ElvisAssignmentOperator {
                static String elvisAssignment(int number) {
                    number ?= 10
                }
            }
            '''
        ])
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("ElvisAssignmentOperator.groovy"), { FullFileInfo f ->
                    assertClass(f, simplyNamed("ElvisAssignmentOperator"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("elvisAssignment"), { MethodInfo m ->
                            m.statements.size() == 1
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testIdentityComparisonOperators() {
        // test '===' and '!=='
        instrumentAndCompileWithGrover([
                "IdentityComparisonOperators.groovy":
                        '''
            class IdentityComparisonOperators {
                static boolean test() {
                    def a = "hello"
                    def b = "hello"
                    return a === b && a !== null
                }
            }
            '''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("IdentityComparisonOperators.groovy"), { FullFileInfo f ->
                    assertClass(f, simplyNamed("IdentityComparisonOperators"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("test"), { MethodInfo m ->
                            m.statements.size() == 3
                        })
                    })
                }
            }
        }
    }
}
