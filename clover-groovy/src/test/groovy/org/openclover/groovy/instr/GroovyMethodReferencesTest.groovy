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
                        '''
            class BasicMethodReferences {
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
                    assertClass(f, simplyNamed("BasicMethodReferences"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("staticRef"), { MethodInfo m ->
                            m.statements.size() == 2
                        })
                        assertMethod(c, simplyNamed("instanceRef"), { MethodInfo m ->
                            m.statements.size() == 2
                        })
                        assertMethod(c, simplyNamed("constantRef"), { MethodInfo m ->
                            m.statements.size() == 2
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
                        '''
            class ConstructorReferences {
                static int randomValue() {
                    def randomGen = Random::new
                    randomGen().nextInt(100)
                }
                static String arrayType() {
                    def arr = [5, 6, 7].stream().toArray(Integer[]::new)
                    arr.class.name
                }
                static String customClassKind() {
                    class Creature { String species }
                    def creator = Creature::new
                    creator("wolf").species
                }
            }
            '''
        ])
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("ConstructorReferences.groovy"), { FullFileInfo f ->
                    assertClass(f, simplyNamed("ConstructorReferences"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("randomValue"), { MethodInfo m ->
                            m.statements.size() == 2
                        })
                        assertMethod(c, simplyNamed("arrayType"), { MethodInfo m ->
                            m.statements.size() == 2
                        })
                        assertMethod(c, simplyNamed("customClassKind"), { MethodInfo m ->
                            m.statements.size() == 2
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testMultiDimensionalArrayConstructorReference() {
        instrumentAndCompileWithGrover([
                "MultiDimArrayConstructorRef.groovy":
                        '''
            class MultiDimArrayConstructorRef {
                static String matrix() {
                    def makeMatrix = Integer[][]::new
                    def grid = makeMatrix(2, 2)
                }
            }
            '''
        ])
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("MultiDimArrayConstructorRef.groovy"), { FullFileInfo f ->
                    assertClass(f, simplyNamed("MultiDimArrayConstructorRef"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("field_matrix"), { MethodInfo m ->
                            m.statements.size() == 1
                        })
                        assertMethod(c, simplyNamed("field_grid"), { MethodInfo m ->
                            m.statements.size() == 2
                        })
                    })
                }
            }
        }
    }

}
