package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.buildutil.test.junit.GroovyVersionStart
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo

@CompileStatic
class GroovyMethodReferencesTest extends TestBase {
    GroovyMethodReferencesTest(String testName) {
        super(testName)
    }

    GroovyMethodReferencesTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    @GroovyVersionStart("3.0.0")
    void testBasicMethodReferences() {
        instrumentAndCompileWithGrover([
                "BasicMethodReferences.groovy":
                        '''class BasicMethodReferences {
                static List<String> staticRef() {
                    def nums = [10, 20, 30]
                    nums.stream().map(Integer::toString).toList()
                }
                static List<String> instanceRef() {
                    def words = ['foo', 'bar']
                    words.stream().map(String::toUpperCase).toList()
                }
                static int constantRef() {
                    def getLength = "hello world"::length
                    getLength()
                }
            }
            '''
        ])
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("BasicMethodReferences.groovy"), { FullFileInfo f ->
                    assertClass(f, named("BasicMethodReferences"), { FullClassInfo c ->
                        // Each unbound class-based method ref is replaced with a closure wrapper,
                        // registering its own statement in addition to the enclosing statement.
                        assertMethod(c, simplyNamed("staticRef"), { MethodInfo m ->
                            m.statements.size() == 3  // def nums + stream call + Integer::toString
                        })
                        assertMethod(c, simplyNamed("instanceRef"), { MethodInfo m ->
                            m.statements.size() == 3  // def words + stream call + String::toUpperCase
                        })
                        assertMethod(c, simplyNamed("constantRef"), { MethodInfo m ->
                            // "hello world"::length is a bound ref (non-ClassExpression receiver) — not instrumented
                            m.statements.size() == 2  // def getLength (decl) + getLength()
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testConstructorReferences() {
        instrumentAndCompileWithGrover([
                "ConstructorReferences.groovy":
                        '''class ConstructorReferences {
                class Creature { String species }
                static int randomValue() {
                    def randomGen = Random::new
                    randomGen().nextInt(100)
                }
                static String arrayType() {
                    def arr = [5, 6, 7].stream().toArray(Integer[]::new)
                    arr.class.name
                }
                static String customClassKind() {
                    def creator = Creature::new
                    creator("wolf").species
                }
            }
            '''
        ])
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("ConstructorReferences.groovy"), { FullFileInfo f ->
                    assertClass(f, named("ConstructorReferences"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("randomValue"), { MethodInfo m ->
                            m.statements.size() == 3  // def randomGen (decl) + Random::new + randomGen().nextInt(100)
                        })
                        assertMethod(c, simplyNamed("arrayType"), { MethodInfo m ->
                            m.statements.size() == 3  // def arr (decl) + Integer[]::new + arr.class.name
                        })
                        assertMethod(c, simplyNamed("customClassKind"), { MethodInfo m ->
                            m.statements.size() == 3  // def creator (decl) + Creature::new + creator("wolf").species
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testMultiDimensionalArrayConstructorReference() {
        // Integer[][]::new takes a single int (row count), not two ints
        instrumentAndCompileWithGrover([
                "MultiDimArrayConstructorRef.groovy":
                        '''class MultiDimArrayConstructorRef {
                static def makeMatrix = Integer[][]::new
                static void matrix() {
                    Integer[][] grid = makeMatrix(3)
                }
            }
            '''
        ])
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("MultiDimArrayConstructorRef.groovy"), { FullFileInfo f ->
                    assertClass(f, named("MultiDimArrayConstructorRef"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("field makeMatrix"), { MethodInfo m ->
                            m.statements.size() == 1 &&
                                    assertStatement(m, at(2, 41, 2, 57)) // Integer[][]::new
                        })
                        assertMethod(c, simplyNamed("matrix"), { MethodInfo m ->
                            m.statements.size() == 1 &&
                                    assertStatement(m, at(4, 21, 4, 53)) // Integer[][] grid = makeMatrix(3)
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testMethodReferencesWithCompileStatic() {
        instrumentAndCompileWithGrover([
                "MethodRefsCompileStatic.groovy":
                        '''@groovy.transform.CompileStatic
            class MethodRefsCompileStatic {
                static List<String> staticRef(List<Integer> nums) {
                    return nums.stream().map(Integer::toString).toList()
                }
                static List<String> instanceRef(List<String> words) {
                    return words.stream().map(String::toUpperCase).toList()
                }
            }'''
        ])
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("MethodRefsCompileStatic.groovy"), { FullFileInfo f ->
                    assertClass(f, named("MethodRefsCompileStatic"), { FullClassInfo c ->
                        // Each method reference gets its own statement (wrapped in exprEval),
                        // plus the return statement that contains it.
                        assertMethod(c, simplyNamed("staticRef"), { MethodInfo m ->
                            m.statements.size() == 2  // return stmt + Integer::toString
                        }) &&
                        assertMethod(c, simplyNamed("instanceRef"), { MethodInfo m ->
                            m.statements.size() == 2  // return stmt + String::toUpperCase
                        })
                    })
                }
            }
        }
    }

}
