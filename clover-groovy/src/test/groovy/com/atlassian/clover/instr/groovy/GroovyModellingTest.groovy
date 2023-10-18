package com.atlassian.clover.instr.groovy

import com.atlassian.clover.api.registry.MethodInfo
import com.atlassian.clover.cfg.instr.InstrumentationConfig
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.util.ChecksummingReader
import groovy.transform.CompileStatic

/**
 * Integration tests that detect if the correct Clover model is generated for given Groovy code.
 **/
@CompileStatic
class GroovyModellingTest extends TestBase {

    GroovyModellingTest(String testName) {
        super(testName)
    }

    GroovyModellingTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
    }

    void testNoConfigMeansNoInstrumentation() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
                public class Foo {
                  public void barVoid() {}
                }
            """], "", [], { null })

        assertFalse db.exists()
    }

    void testFileChecksumIsCalculated() {
        String fooContents = """
          public class Foo {
            public void barVoid() {}
          }
        """

        instrumentAndCompileWithGrover(["Foo.groovy": fooContents])

        int checksum = (int) new ChecksummingReader(new StringReader(fooContents)).with { readLines(); getChecksum() }
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, isDefaultPackage, { FullPackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    f.checksum == checksum
                }
            }
        }
    }


    void testFullyQualifiedNames() {
        instrumentAndCompileWithGrover(
                ["com/atlassian/foo/bar/Foo.groovy":
                         """
              package com.atlassian.foo.bar

              public class Foo {
                public void barVoid() {}
              }
            """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, named("com.atlassian.foo.bar"), { FullPackageInfo p ->
                (p.path == "com/atlassian/foo/bar/") &&
                        assertFile(p, named("Foo.groovy")) { FullFileInfo f ->
                            (f.packagePath == "com/atlassian/foo/bar/Foo.groovy") &&
                                    assertClass(f, named("Foo")) { FullClassInfo c ->
                                        (c.qualifiedName == "com.atlassian.foo.bar.Foo") &&
                                                assertMethod(c, simplyNamed("barVoid")) { MethodInfo m ->
                                                    m.qualifiedName == "com.atlassian.foo.bar.Foo.barVoid"
                                                }
                                    }
                        }
            }
        }
    }


    void testMethodLevelInstr() {
        instrumentAndCompileWithGrover(
                ["com/atlassian/foo/bar/Foo.groovy":
                         """
              package com.atlassian.foo.bar

              public class Foo {
                public void barVoid() {
                    println 'hello'
                }
              }
            """],
                "",
                [],
                { InstrumentationConfig it ->
                    it.instrLevelStrategy = "method"; it
                })

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, named("com.atlassian.foo.bar"), { FullPackageInfo p ->
                (p.path == "com/atlassian/foo/bar/") &&
                        assertFile(p, named("Foo.groovy")) { FullFileInfo f ->
                            (f.packagePath == "com/atlassian/foo/bar/Foo.groovy") &&
                                    assertClass(f, named("Foo")) { FullClassInfo c ->
                                        (c.qualifiedName == "com.atlassian.foo.bar.Foo") &&
                                                assertMethod(c, simplyNamed("barVoid")) { MethodInfo m ->
                                                    assertEquals("there should be no statements", 0, m.statements.size())
                                                    assertEquals("there should be no branches", 0, m.branches.size())
                                                    true
                                                }
                                    }
                        }
            }
        }
    }

}
