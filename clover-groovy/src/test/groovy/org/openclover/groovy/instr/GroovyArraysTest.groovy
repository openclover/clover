package org.openclover.groovy.instr

import groovy.transform.CompileStatic
import org.openclover.buildutil.test.junit.GroovyVersionStart
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo

@CompileStatic
class GroovyArraysTest extends TestBase {

    GroovyArraysTest(String testName) {
        super(testName)
    }

    GroovyArraysTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    void testClassicGroovyArrayInitializer() {
        instrumentAndCompileWithGrover([
                "GroovyStyleArrayInitializer.groovy":
                        '''class GroovyStyleArrayInitializer {
                    void array() {
                        int[] i = [ 0, 1, 2 ]
                    }
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("GroovyStyleArrayInitializer.groovy"), { FullFileInfo f ->
                    assertClass(f, simplyNamed("GroovyStyleArrayInitializer"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("array"), { MethodInfo m ->
                            m.statements.size() == 1
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testJavaStyleArrayInitializer() {
        instrumentAndCompileWithGrover([
                "JavaStyleArrayInitializer.groovy":
                        '''class JavaStyleArrayInitializer {
                    void array() {
                        int[] i = new int[] { 0, 1, 2 }
                    }
                }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("JavaStyleArrayInitializer.groovy"), { FullFileInfo f ->
                    assertClass(f, simplyNamed("JavaStyleArrayInitializer"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("array"), { MethodInfo m ->
                            m.statements.size() == 1
                        })
                    })
                }
            }
        }
    }

    @GroovyVersionStart("3.0.0")
    void testSafeIndexingOnArray() {
        instrumentAndCompileWithGrover([
                "SafeIndexingArray.groovy":
                        '''class SafeIndexingArray {
                static void test() {
                    String[] myArray = ['a', 'b']
                    assert myArray?[1] == 'b'
                    myArray?[1] = 'c'
                    assert myArray?[1] == 'c'

                    String[] nullArray = null
                    assert nullArray?[1] == null
                    nullArray?[1] = 'd'
                    assert nullArray?[1] == null
                }
            }'''
        ])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { PackageInfo p ->
                assertFile p, named("SafeIndexingArray.groovy"), { FullFileInfo f ->
                    assertClass(f, simplyNamed("SafeIndexingArray"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("test"), { MethodInfo m ->
                            m.statements.size() == 8
                        })
                    })
                }
            }
        }
    }
}
