package com.atlassian.clover.instr.groovy

import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.api.registry.MethodInfo
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.test.junit.Result
import com.atlassian.clover.cfg.instr.InstrumentationConfig
import com.atlassian.clover.CodeType
import com.atlassian.clover.registry.entities.FullProjectInfo

import static com.atlassian.clover.groovy.utils.TestUtils.assertStringContains

/**
 * Integration tests that detect if the correct coverage is recorded for given Groovy code. All code samples are executed via PSVM methods.
 */
public class GroovyCoverageTest extends TestBase {
    public GroovyCoverageTest(methodName, specificName, groovyAllJar) {
        super(methodName, specificName, groovyAllJar);
    }

    public GroovyCoverageTest(String testName) {
        super(testName);
    }

    public void testMethodEntry() {
        instrumentAndCompileWithGrover(
            ["Foo.groovy":
            """
                public class Foo {
                  public static void main(String[] args) {
                    new Foo().with { 5.times { bar() } }
                  }
                  public void bar() {
                    println "Foobar!"
                  }
                }
              """])

        runWithAsserts("Foo")


        assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("Foo.groovy"), {FullFileInfo f ->
                assertClass f, named("Foo"), {FullClassInfo c ->
                    assertMethod (c, simplyNamed("main"), hits(1)) &&
                    assertMethod (c, simplyNamed("bar"), hits(5))
                }
            }
        }
    }


    public void testUncoveredElementMetric() {
        instrumentAndCompileWithGrover(
            ["Foo.groovy":
            """
                public class Foo {
                  static Integer foobar = 0;

                  public static void main(String[] args) {
                    new Foo().with { 5.times { println "foo" } }
                  }
                }
              """])

        runWithAsserts("Foo")


        assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("Foo.groovy"), {FullFileInfo f ->
                assertClass f, named("Foo"), {FullClassInfo c ->
                    assertMethod (c, simplyNamed("main"), hits(1)) &&
                    assertMethod (c, simplyNamed("field foobar"), uncoveredElements(0)) &&
                    assertMethod (c, simplyNamed("field foobar"), pcUncoveredElements(-1f))
                }
            }
        }
    }

    public void testCtorsRecognisedAndCtorChainingPreserved() {
        instrumentAndCompileWithGrover(
            ["Foo.groovy":
            """
                public class Foo {
                  public static void main(String[] args) {
                    new Foo("A foo")
                    new Bar()
                    new Bar(1)
                    new Bar("A bar")
                  }

                  public Foo(String name) {
                  }
                }

                public class Bar extends Foo {
                  public Bar(int i) {
                    this(Integer.toString(i))
                    println i
                  }

                  public Bar() {
                    super("A bar")
                  }

                  public Bar(String name) {
                    super(name)
                  }
                }
              """])

        runWithAsserts("Foo")


        assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("Foo.groovy"), {FullFileInfo f ->
                assertClass(f, named("Foo"), {FullClassInfo c ->
                    assertMethod(c, simplyNamed("main"), hits(1)) &&
                    assertMethod(c, named("<init>(String) : void"), hits(4))
                }) &&
                assertClass(f, named("Bar"), {FullClassInfo c ->
                    assertMethod(c, named("<init>() : void"), hits(1)) &&
                    assertMethod(c, named("<init>(int) : void"), hits(1)) &&
                    assertMethod(c, named("<init>(String) : void"), hits(2))
                })
            }
        }
    }

    public void testMethodEntryAcrossClasses() {
        instrumentAndCompileWithGrover(
            ["Foo.groovy":
            """
                public class Foo {
                  public static void main(String[] args) {
                    new Bar().with { 5.times { bar() } }
                  }
                }
              """,
             "Bar.groovy":
             """
                public class Bar {
                  public void bar() {
                    println "Foobar!"
                  }
                }
              """])

        runWithAsserts("Foo")

        assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("Foo.groovy"), {FullFileInfo f ->
                assertClass f, named("Foo"), {FullClassInfo c ->
                    assertMethod c, simplyNamed("main"), hits(1)
                }
            }
            assertFile p, named("Bar.groovy"), {FullFileInfo f ->
                assertClass f, named("Bar"), {FullClassInfo c ->
                    assertMethod c, simplyNamed("bar"), hits(5)
                }
            }
        }
    }

    public void testTestThatDeclaresAVar() {
        instrumentAndCompileWithGrover(
            ["FooTest.groovy":
            """
                public class FooTest {

                  public static void main(String[] args) {
                    new FooTest().testMe()
                  }

                  public void testMe() {
                    def foo = 123
                    println "Foo is: \${foo}"
                  }
                }
              """,
             ])

        runWithAsserts("FooTest")

        assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("FooTest.groovy"), {FullFileInfo f ->
                assertClass f, named("FooTest"), {FullClassInfo c ->
                    assertMethod c, simplyNamed("main"), hits(1)
                }
            }
        }
    }

    public void testMethodStatementExecution() {
        instrumentAndCompileWithGrover(
            ["Foo.groovy":
            """
              public class Foo {
                public static void main(String[] args) {
                  println "Hello, world"
                  5.times { println "Hello, world" }
                  for (i in [1,2,3,4]) {
                    switch (i) {
                        case 1:
                            println "one"
                            break
                        case 2:
                            println "two"
                            break
                        case 10:
                            println "ten"
                            break
                        default:
                            println "default \${i}"
                    }
                  }
                  if (true == true) {
                    println "true"
                  } else {
                    println "false"
                  }
                  int count = 2
                  while (count--) {
                    println count
                  }
                  try {
                    throw new Exception()
                  } catch (Throwable t) {
                    println "Caught"
                  } finally {
                    println "Finally"
                  }
                  synchronized(new Object()) {
                    println "synchronized"
                  }
                  assert "a" == "a"
                  def a;
                  println(a == null ? "null" : "not null")
                  if (1 + 1 == 2)
                    println "true"
                  else
                    println "false"
                  while (1 + 1 != 2)
                    println "will not get here"
                  for (int z = 5; z < 0; z++)
                    println "will get here 5 times"
                  return
                }
              }
            """])

        Result result = runWithAsserts("Foo")
        assertEquals 0, result.getStdErr().length()

        assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("Foo.groovy"), {FullFileInfo f ->
                assertClass f, named("Foo"), {FullClassInfo c ->
                    assertMethod c, simplyNamed("main"), {MethodInfo m ->
                        m.hitCount == 1 &&
                        assertStatement(m, at(4, 19, 4, 41), hits(1)) &&     //println
                        assertStatement(m, at(5, 19, 5, 53), hits(1)) &&     //5.times statement
                        assertStatement(m, at(5, 29, 5, 52), hits(5)) &&     //5.times block
                        assertStatement(m, at(6, 19, 20, 20), hits(1)) &&    //for statement
                        assertStatement(m, at(7, 21, 19, 22), hits(4)) &&    //switch statement
                        assertStatement(m, at(9, 29, 9, 42), hits(1)) &&     //println "one"
                        assertStatement(m, at(12, 29, 12, 42), hits(1)) &&   //println "two"
                        assertStatement(m, at(15, 29, 15, 42), hits(0)) &&   //println "ten"
                        assertStatement(m, at(18, 29, 18, 51), hits(2)) &&   //println "default \${i}"
                        assertStatement(m, at(21, 19, 25, 20), hits(1)) &&   //if statement
                        assertBranch(m, at(21, 23, 21, 35), hits(1, 0)) &&  //if true == true
                        assertStatement(m, at(22, 21, 22, 35), hits(1)) &&   //println "true"
                        assertStatement(m, at(24, 21, 24, 36), hits(0)) &&   //prinltn "false"
                        assertStatement(m, at(26, 19, 26, 32), hits(1)) &&   //int count = 2
                        assertStatement(m, at(27, 19, 29, 20), hits(1)) &&   //while ...
                        assertBranch(m, at(27, 26, 27, 33), hits(2, 1)) &&   //while expression
                        assertStatement(m, at(28, 21, 28, 34), hits(2)) &&   //println count
                        assertStatement(m, at(30, 19, 36, 20), hits(1)) &&   //try ...
                        assertStatement(m, at(31, 21, 31, 42), hits(1)) &&   //throw new Exception()
                        assertStatement(m, at(33, 21, 33, 37), hits(1)) &&   //prinltn "Caught"
                        assertStatement(m, at(35, 21, 35, 38), hits(1)) &&   //prinltn "Finally"
                        assertStatement(m, at(37, 19, 39, 20), hits(1)) &&   //synchronized (...) {...}
                        assertStatement(m, at(38, 21, 38, 43), hits(1)) &&   //println "synchronized"
                        assertStatement(m, at(40, 19, 40, 36), hits(1)) &&   //assert "a" == "a"
                        assertStatement(m, at(42, 19, 42, 59), hits(1)) &&   //println(...)
                        assertBranch(m, at(42, 27, 42, 36), hits(1, 0)) &&   //a == null ? "null" : "not null"
                        assertStatement(m, at(43, 19, 46, 36), hits(1)) &&   //if ...
                        assertBranch(m, at(43, 23, 43, 33), hits(1, 0)) &&   //if (1 + 1 == 2)
                        assertStatement(m, at(44, 21, 44, 35), hits(1)) &&   //println "true"
                        assertStatement(m, at(46, 21, 46, 36), hits(0)) &&   //println "false"

                        assertStatement(m, at(47, 19, 48, 48), hits(1)) &&   //while ...
                        assertBranch(m, at(47, 26, 47, 36), hits(0, 1)) &&   //while (1 + 1 != 2)
                        assertStatement(m, at(48, 21, 48, 48), hits(0)) &&   //println "will not get here"

                        assertStatement(m, at(47, 19, 48, 48), hits(1)) &&   //for (int z = 5; z < 0; z++)
                        assertBranch(m, at(47, 26, 47, 36), hits(0, 1)) &&   //z < 0
                        assertStatement(m, at(48, 21, 48, 48), hits(0)) &&   //println "will get here 5 times"

                        assertStatement(m, at(51, 19, 51, 25), hits(1))      //return
                    }
                }
            }
        }
    }

    /**
     * Test succeeds on Groovy 1.6.5 or higher. The "switch as expression" was implemented in 1.6.5:
     * http://jira.codehaus.org/browse/GROOVY-3789
     */
    public void testImplicitReturnsArePreserved() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy": """
                    public class Foo {
                        def implicitReturns(boolean b) {
                            switch (b) {
                                case true:
                                    new Integer(10)
                                    break    // we shall not have recorder.inc() before break as it would change return value
                                case false:
                                    new String("ABC")
                                    break    // we shall not have recorder.inc() before break as it would change return value
                            }
                        }

                        def explicitReturns(boolean b) {
                            switch (b) {
                                case true:
                                    return new Integer(10)
                                case false:
                                    return new String("ABC")
                            }
                        }

                        public static void main(String[] args) {
                            println "implicitReturns" + new Foo().implicitReturns(false)
                            println "implicitReturns" + new Foo().implicitReturns(true)
                            println "explicitReturns" + new Foo().explicitReturns(false)
                            println "explicitReturns" + new Foo().explicitReturns(true)
                        }
                    }
            """])

        Result result = runWithAsserts("Foo")

        // check execution log
        assertStringContains("implicitReturns10", result.stdOut, false)
        assertStringContains("implicitReturnsABC", result.stdOut, false)
        assertStringContains("explicitReturns10", result.stdOut, false)
        assertStringContains("explicitReturnsABC", result.stdOut, false)

        // check coverage
        FullProjectInfo projectInfo = CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION)
        assertPackage projectInfo, { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("Foo.groovy"), {FullFileInfo f ->
                assertClass f, named("Foo"), {FullClassInfo c ->
                    assertMethod(c, simplyNamed("implicitReturns"), {MethodInfo m ->
                        m.hitCount == 2 &&
                        m.statements.size() == 3 &&
                                assertStatement(m, at(4, 29, 11, 30), hits(2)) &&    // entire switch block
                                assertStatement(m, at(6, 37, 6, 52), hits(1)) &&     // new Integer(10)
                                assertStatement(m, at(9, 37, 9, 54), hits(1))        // new String("abc")
                    }) &&

                    assertMethod(c, simplyNamed("explicitReturns"), {MethodInfo m ->
                        m.hitCount == 2 &&
                        m.statements.size() == 3 &&
                                assertStatement(m, at(15, 29, 20, 30), hits(2)) &&     // entire switch block
                                assertStatement(m, at(17, 37, 17, 59), hits(1)) &&     // case true, return new Integer(10)
                                assertStatement(m, at(19, 37, 19, 61), hits(1))        // case false, return new String("abc")
                    })
                }
            }
        }
    }


    //Tests that it all works if we have to construct a block but have no enclosing method from which to get the variable scope
    public void testNonBlockIfStatementsInNonMethods() {
        instrumentAndCompileWithGrover(
            ["Foo.groovy":
            """
              public class Foo {
                public static def foo = {
                  if (1 + 1 == 2)
                    "1 + 1 == 2"
                  for(i in [1,2,3])
                    println i
                  for(int i = 0; i < 3; i++)
                    println i
                }
                public static void main(String[] args) {
                  println foo()
                }
              }
            """])


        runWithAsserts("Foo")

        assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("Foo.groovy"), {FullFileInfo f ->
                assertClass f, named("Foo"), {FullClassInfo c ->
                    assertMethod c, simplyNamed("field foo"), {MethodInfo m ->
                        m.hitCount == 1 &&
                        assertStatement(m, at(4, 19, 5, 33), hits(1)) &&     //if (1 + 1 == 2) ...
                        assertBranch(m, at(4, 23, 4, 33), hits(1, 0)) &&        //(1 + 1 == 2)
                        assertStatement(m, at(5, 21, 5, 33), hits(1)) &&     //"1 + 1 == 2"
                        assertStatement(m, at(6, 19, 7, 30), hits(1)) &&     //for (i in [1,2,3]) ...
                        assertStatement(m, at(7, 21, 7, 30), hits(3)) &&     //println i
                        assertStatement(m, at(8, 19, 9, 30), hits(1)) &&     //for (int i = 0; i < 3; i++) ...
                        assertBranch(m, at(8, 34, 8, 39), hits(3, 1)) &&      //i < 3
                        assertStatement(m, at(9, 21, 9, 30), hits(3))        //println i
                    }
                }
            }
        }
    }

    public void testClosureStatementsInSurprisingPlaces() {
        instrumentAndCompileWithGrover(
            ["Foo.groovy":
            """
              public class Foo {
                public static void main(String[] args) {
                  println new Foo().bar().call().call()
                }

                public def bar(printer = { println "Hello, \$it" }) {
                  printer.call("World")

                  for (i in [1,2,3,4]) {
                    switch (i) {
                        case 1:
                            println "one"
                            break
                        case {1 + 1}.call():
                            println "two"
                            break
                        default:
                            println "default \${i}"
                    }
                  }
                  for (val in [{true}, {false}]) {
                      if (val.call()) {
                        println "true"
                      } else {
                        println "false"
                      }
                  }
                  int count = 2
                  while ({count--}.call()) {
                    println count
                  }
                  try {
                    throw {new Exception()}.call()
                  } catch (Throwable t) {
                    println "caught \${t}"
                  }
                  synchronized({new Object()}.call()) {
                    println "synchronized"
                  }
                  assert {"a"}.call() == {"a"}.call()
                  return { println "Adieu"; { -> "Goodbye" } }
                }
              }
            """])

        runWithAsserts("Foo")

        assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("Foo.groovy"), {FullFileInfo f ->
                assertClass f, named("Foo"), {FullClassInfo c ->
                    assertMethod(c, simplyNamed("main"), {MethodInfo m ->
                        m.hitCount == 1 &&
                        assertStatement(m, at(4, 19, 4, 56), hits(1))      //new Foo().bar().call()
                    }) &&
                    assertMethod(c, simplyNamed("bar"), {MethodInfo m ->
                        m.hitCount == 1 &&
                        assertStatement(m, at(7, 44, 7, 65), hits(1)) &&       //println "Hello, \$it"
                        assertStatement(m, at(15, 31, 15, 36), hits(3)) &&     //1 + 1 - evaled 3 times because the first value matches on 1
                        assertStatement(m, at(22, 33, 22, 37), hits(1)) &&     //true
                        assertStatement(m, at(22, 41, 22, 46), hits(1)) &&     //false
                        assertStatement(m, at(30, 27, 30, 34), hits(3)) &&     //count--
                        assertStatement(m, at(34, 28, 34, 43), hits(1)) &&     //new Exception()
                        assertStatement(m, at(38, 33, 38, 45), hits(1)) &&     //new Object()
                        assertStatement(m, at(41, 27, 41, 30), hits(1)) &&     //"a"
                        assertStatement(m, at(41, 43, 41, 46), hits(1)) &&     //"a"
                        assertStatement(m, at(42, 28, 42, 43), hits(1)) &&     //println "Adieu"
                        assertStatement(m, at(42, 50, 42, 60), hits(1))      //"Goodbye"
                    })
                }
            }
        }
    }

    public void testTestSlicesRecorded() {
        instrumentAndCompileWithGrover(
            ["FooTest.groovy":
            """
              public class FooTest {
                public static void main(String[] args) {
                  new FooTest().testBar1()
                  new FooTest().testBar2()
                }

                public void testBar1() {
                  bar1()
                }

                public void bar1() {
                  println "bar1"
                }

                public void testBar2() {
                  bar2()
                }

                public void bar2() {
                  println "bar2"
                }
              }
            """])

        runWithAsserts("FooTest")

        CloverDatabase db = CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec())
        assertPackage db.getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("FooTest.groovy"), {FullFileInfo f ->
                assertClass(f, named("FooTest"), {FullClassInfo c ->
                    assertTestCase(c, { it.qualifiedName == "FooTest.testBar1"}, { it.isSuccess() }) &&
                    assertTestCase(c, { it.qualifiedName == "FooTest.testBar2"}, { it.isSuccess() }) &&
                    assertMethod(c, simplyNamed("main"), {MethodInfo m ->
                        m.hitCount == 1 &&
                        assertStatement(m, at(4, 19, 4, 43), hits(1)) &&       //new FooTest().testBar1()
                        assertStatement(m, at(5, 19, 5, 43), hits(1))          //new FooTest().testBar2()
                    }) &&
                    assertMethod(c, simplyNamed("testBar1"), {MethodInfo m ->
                        m.isTest() &&
                        m.hitCount == 1 &&
                        assertStatement(m, at(9, 19, 9, 25), and(hits(1), hitByTest(db, c, "FooTest.testBar1"), notHitByTest(db, c, "FooTest.testBar2"))) //bar1()
                    }) &&
                    assertMethod(c, simplyNamed("testBar2"), {MethodInfo m ->
                        m.isTest() &&
                        m.hitCount == 1 &&
                        assertStatement(m, at(17, 19, 17, 25), and(hits(1), hitByTest(db, c, "FooTest.testBar2"), notHitByTest(db, c, "FooTest.testBar1"))) //bar2()
                    }) &&
                    assertMethod(c, simplyNamed("bar1"), {MethodInfo m ->
                        !m.isTest() &&
                        m.hitCount == 1 &&
                        assertHitByTest(m, db, c, "FooTest.testBar1") &&
                        assertNotHitByTest(m, db, c, "FooTest.testBar2") &&
                        assertStatement(m, at(13, 19, 13, 33), and(hits(1), hitByTest(db, c, "FooTest.testBar1"), notHitByTest(db, c, "FooTest.testBar2"))) //prinltn "bar1"
                    }) &&
                    assertMethod(c, simplyNamed("bar2"), {MethodInfo m ->
                        !m.isTest() &&
                        m.hitCount == 1 &&
                        assertHitByTest(m, db, c, "FooTest.testBar2") &&
                        assertNotHitByTest(m, db, c, "FooTest.testBar1") &&
                        assertStatement(m, at(21, 19, 21, 33), and(hits(1), hitByTest(db, c, "FooTest.testBar2"), notHitByTest(db, c, "FooTest.testBar1"))) //println "bar2"
                    })
                })
            }
        }
    }

    public void testExpectedExceptionsHandled() {
        instrumentAndCompileWithGrover(
            ["FooTest.groovy":
            """
              public class FooTest {
                public static void main(String[] args) {
                  try {
                    new FooTest().testExpectedException()
                  } catch (Exception e) { println e; }
                  try {
                    new FooTest().testUnexpectedException()
                  } catch (Exception e) { println e; }
                  new FooTest().testNoException()
                }

                public void testNoException() {
                    println "testNoException"
                }

                @org.testng.annotations.ExpectedExceptions(FooException)
                public void testExpectedException() {
                  println "testExpectedException"; throw new FooException()
                }

                public void testUnexpectedException() {
                  println "testUnexpectedException"; throw new FooException()
                }
              }

              public class FooException extends Exception {}
            """,
            "ExpectedExceptions.groovy":
            """
              package org.testng.annotations

              public @interface ExpectedExceptions {
                public Class value();
              }
            """])

        runWithAsserts("FooTest")

        CloverDatabase db = CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec())
        assertPackage db.getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("FooTest.groovy"), {FullFileInfo f ->
                assertClass(f, named("FooTest"), {FullClassInfo c ->
                    assertTestCase(c, { it.qualifiedName == "FooTest.testNoException"}, { it.isSuccess() }) &&
                    assertTestCase(c, { it.qualifiedName == "FooTest.testExpectedException"}, { it.isSuccess() }) &&
                    assertTestCase(c, { it.qualifiedName == "FooTest.testUnexpectedException"}, { !it.isSuccess() })
                })
            }
        }
    }

    public void testFieldsWithInitialisers() {
        instrumentAndCompileWithGrover(
            ["Foo.groovy":
            """
                public class Foo {
                  def bar = {
                    println "Foobar!"
                  }
                  int someInt = 1 + 2
                  static def foo = [null, 1].collect { it?.toString() }

                  public static void main(String[] args) {
                    new Foo().with { 5.times { bar(someInt) } }
                    println foo
                  }
                }
              """])

        runWithAsserts("Foo")


        assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("Foo.groovy"), {FullFileInfo f ->
                assertClass f, named("Foo"), {FullClassInfo c ->
                    assertMethod(c, simplyNamed("field bar"), and(hits(1), at(3, 19, 5, 20), {MethodInfo m ->
                      assertStatement(m, at(4, 21, 4, 38), hits(5))
                    })) &&
                    assertMethod(c, simplyNamed("field someInt"), hits(1)) && 
                    assertMethod(c, and(simplyNamed("field foo"), at(7, 19, 7, 72)), and(hits(1), { MethodInfo m->
                        assertStatement(m, at(7, 56, 7, 71), hits(2)) &&
                        assertBranch(m, at(7, 56, 7, 58), hits(1, 1))
                    }))
                }
            }
        }
    }

    public void testExpressionBranches() {
        instrumentAndCompileWithGrover(
            ["Foo.groovy":
            """
                public class Foo {
                  static def foo = System.getProperty("foo.bar").with { it.equals(null) ? null : "not null" }
                  static def foo2 = [null, 1].collect { it?.toString() }
                  static def foo3 = System.getProperty("foo.bar")?.toString()
                  static def foo4 = System.getProperty("foo.bar") ?: "not set"
                  static def foo5 = System.getProperty("foo.bar") == null ? "set" : "not set"

                  public static void main(String[] args) {
                    println foo
                    println foo2
                    println foo3
                    println foo4
                    println foo5
                  }
                }
              """])

        runWithAsserts("Foo")


        assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
            assertFile p, named("Foo.groovy"), {FullFileInfo f ->
                assertClass f, named("Foo"), {FullClassInfo c ->
                    assertMethod(c, and(simplyNamed("field foo"), at(3, 19, 3, 110)), and(hits(1), { MethodInfo m->
                        assertStatement(m, at(3, 73, 3, 109), hits(1)) &&
                        assertBranch(m, at(3, 73, 3, 89), hits(1, 0))
                    })) &&
                    assertMethod(c, and(simplyNamed("field foo2"), at(4, 19, 4, 73)), and(hits(1), { MethodInfo m->
                        assertStatement(m, at(4, 57, 4, 72), hits(2)) &&
                        assertBranch(m, at(4, 57, 4, 59), hits(1, 1))
                    })) &&
                    assertMethod(c, and(simplyNamed("field foo3"), at(5, 19, 5, 78)), and(hits(1), { MethodInfo m->
                        assertBranch(m, at(5, 37, 5, 66), hits(0, 1))
                    })) &&
                    assertMethod(c, and(simplyNamed("field foo4"), at(6, 19, 6, 79)), and(hits(1), { MethodInfo m->
                        assertBranch(m, at(6, 37, 6, 67), hits(0, 1))
                    })) &&
                    assertMethod(c, and(simplyNamed("field foo5"), at(7, 19, 7, 94)), and(hits(1), { MethodInfo m->
                        assertBranch(m, at(7, 37, 7, 74), hits(1, 0))
                    }))
                }
            }
        }
    }

    public void testThreadedFlushing() {
         instrumentAndCompileWithGrover(
             ["FooTest.groovy":
             """
                 public class FooTest {
                   public static void main(String[] args) {
                     println "Hello"
                     Thread.sleep(2000)
                   }
                 }
               """,
              ], "", [], { it.flushPolicy = InstrumentationConfig.THREADED_FLUSHING ; it.flushInterval = 1 ; it})

         runWithAsserts("FooTest")

         assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
             assertFile p, named("FooTest.groovy"), {FullFileInfo f ->
                 assertClass f, named("FooTest"), {FullClassInfo c ->
                     assertMethod(c, simplyNamed("main"), and(hits(1), { MethodInfo m ->
                         assertStatement(m, at(4, 22, 4, 37), hits(1))
                     }))
                 }
             }
         }
     }

    public void testIntervalFlushing() {
         instrumentAndCompileWithGrover(
             ["FooTest.groovy":
             """
                 public class FooTest {
                   public static void main(String[] args) {
                     println "Hello"
                     Thread.sleep(2000)
                   }
                 }
               """,
              ], "", [], { it.flushPolicy = InstrumentationConfig.INTERVAL_FLUSHING ; it.flushInterval = 1 ; it})

         runWithAsserts("FooTest")

         assertPackage CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getModel(CodeType.APPLICATION), { it.isDefault() }, {FullPackageInfo p ->
             assertFile p, named("FooTest.groovy"), {FullFileInfo f ->
                 assertClass f, named("FooTest"), {FullClassInfo c ->
                     assertMethod(c, simplyNamed("main"), and(hits(1), { MethodInfo m ->
                         assertStatement(m, at(4, 22, 4, 37), hits(1))
                     }))
                 }
             }
         }
     }

    public GroovyCoverageTest(String methodName, String specificName, File groovyAllJar) {
        super(methodName, specificName, groovyAllJar)
    }
}