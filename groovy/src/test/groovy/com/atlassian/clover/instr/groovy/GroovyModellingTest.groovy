package com.atlassian.clover.instr.groovy

import com.atlassian.clover.api.registry.MethodInfo
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.util.ChecksummingReader

/**
 * Integration tests that detect if the correct Clover model is generated for given Groovy code.
 **/
public class GroovyModellingTest extends TestBase {
    public GroovyModellingTest(String methodName, String specificName, File groovyAllJar) {
        super(methodName, specificName, groovyAllJar)
    }

    public GroovyModellingTest(String testName) {
        super(testName);
    }

    public void testNoConfigMeansNoInstrumentation() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
                public class Foo {
                  public void barVoid() {}
                }
            """], "", [], { null })

        assertFalse db.exists()
    }

    public void testFileChecksumIsCalculated() {
        String fooContents = """
          public class Foo {
            public void barVoid() {}
          }
        """

        instrumentAndCompileWithGrover(["Foo.groovy": fooContents])

        int checksum = (int) new ChecksummingReader(new StringReader(fooContents)).with { readLines(); getChecksum() }
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, { it.isDefault() }, { FullPackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    f.checksum == checksum
                }
            }
        }
    }


    public void testFullyQualifiedNames() {
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


    public void testMethodLevelInstr() {
        instrumentAndCompileWithGrover(
                ["com/atlassian/foo/bar/Foo.groovy":
                         """
              package com.atlassian.foo.bar

              public class Foo {
                public void barVoid() {
                    println 'hello'
                }
              }
            """], "", [], { it.instrLevelStrategy = "method"; it })

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
