package com.atlassian.clover.instr.groovy

import com.atlassian.clover.api.registry.Annotation
import com.atlassian.clover.api.registry.MethodInfo
import com.atlassian.clover.cfg.instr.MethodContextDef
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.context.ContextStore
import com.atlassian.clover.context.MethodRegexpContext
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.entities.*

import java.lang.reflect.Modifier
import java.util.regex.Pattern

/**
 * Integration tests that detect if the correct Clover model is generated for given Groovy code.
 **/
public class GroovyModellingMethodsTest extends TestBase {
    public GroovyModellingMethodsTest(String methodName, String specificName, File groovyAllJar) {
        super(methodName, specificName, groovyAllJar)
    }

    public GroovyModellingMethodsTest(String testName) {
        super(testName);
    }

    public void testMethodsFiltered() {
        String fooContents = """
            public class Foo {

              public void barVoid() {
                String str = "foobar"
                println str
              }

              public void keepMe() {
                println "Not filtered"
              }

              def printHello() {
                println "Hello"
              }

              def printGoodbye() {
                println "Goodbye"
              }

            }
          """
        final MethodContextDef context = new MethodContextDef();
        context.regexp = ".*barVoid.*"
        context.name = "barVoid"

        final MethodContextDef groovyContext = new MethodContextDef();
        groovyContext.regexp = "public def printGoodbye\\(\\)" // this is the full regexp for matching def methodName()
        groovyContext.name = "goodbye"

        instrumentAndCompileWithGrover(["Foo.groovy": fooContents], "", [], {
            it.addMethodContext(context); it.addMethodContext(groovyContext); it
        })

        final ContextStore store = new ContextStore()
        store.addMethodContext(new MethodRegexpContext(context.name, Pattern.compile(context.regexp)))
        store.addMethodContext(new MethodRegexpContext(groovyContext.name, Pattern.compile(groovyContext.regexp)))
        ContextSet set = store.createContextSetFilter(context.name)
        set = set.or(store.createContextSetFilter(groovyContext.name))
        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, { it.isDefault() }, { FullPackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    assertClass f, { it.name == "Foo" && it.methods.size() == 4 }, { FullClassInfo c ->
                        assertMethod(c, and(simplyNamed("barVoid")), { it.isFiltered(set) }) &&
                                assertMethod(c, and(simplyNamed("keepMe")), { !it.isFiltered(set) }) &&

                                assertMethod(c, and(simplyNamed("printHello")), { !it.isFiltered(set) }) &&
                                assertMethod(c, and(simplyNamed("printGoodbye")), { it.isFiltered(set) })
                    }
                }
            }
        }
    }


    public void testInterfacesRecognizedAndMethodsNotAddedToRegistry() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
                 public interface Foo {
                   public void barVoid();
                 }
            """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage Clover2Registry.fromFile(db).model.project, { it.isDefault() }, { FullPackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    assertClass f, { it.name == "Foo" && it.isInterface() && it.methods.size() == 0 }
                }
            }
        }
    }

    public void testAnnotationRecognizedAndFieldsNotAddedToRegistry() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
                 public @interface Foo {
                   public int someNumber();
                 }
            """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, { it.isDefault() }, { FullPackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    assertClass f, { it.name == "Foo" && it.isAnnotationType() && it.methods.size() == 0 }
                }
            }
        }
    }

    public void testEnumRecognizedAndMethodsAddedToRegistry() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
                  public enum Foo {
                    ONE, TWO;

                    public void bar() { }
                  }
            """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, { it.isDefault() }, { FullPackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    assertClass f, { it.name == "Foo" && it.isEnum() }, { FullClassInfo c ->
                        assertMethod c, and(simplyNamed("bar"), at(5, 21, 5, 42), {
                            it.signature.returnType == "void"
                        }, { it.signature.parameters.length == 0 })
                    }
                }
            }
        }
    }

    public void testAbstractMethodsNotAddedToRegistry() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
              public abstract class Foo {
                public abstract void bar()
              }
            """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, { it.isDefault() }, { FullPackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    assertClass f, named("Foo"), { it.methods.size() == 0 }
                }
            }
        }
    }

    public void testGroovyMethodSignaturesRecognized() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
             public class Foo {
               public void barVoid() {}
               def barDef(def param1, def param2, def param3) {}
               def barUntypedParams(param1, param2, param3) {}
               def barClosure(Closure c) {}
               def barDefaults(Closure c = {}, m = [:], n = 12) {}
               private Object barPrivateVarArg(Foo... f) {}
               private <T> T barTypeParam(T t) {}
               private <V, T extends List<V>> Map<V, Set<T>> barGenericsFrenzy(Collection<T> t, Map<? extends Serializable, Class<V>> s, Collection<?> q) {}
               @SuppressWarnings("unchecked") @MyAnnotation(someAnno = @MyOtherAnnotation, someNum = 12, someBool = true, someClass = String.class, someArray = [1, 2, 3]) public void barAnnotated() {}
               private int[][][] barArrayFrenzy(String[] a) {}
             }

             public @interface MyOtherAnnotation {}
             public @interface MyAnnotation {
               public MyOtherAnnotation someAnno();
               public int someNum() default 0;
               public boolean someBool();
               public Class<?> someClass();
               public int[] someArray();
             }
           """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, { it.isDefault() }, { FullPackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    assertClass f, named("Foo"), { FullClassInfo c ->
                        assertMethod(c, simplyNamed("barVoid"), { MethodInfo m ->
                            m.signature.returnType == "void" &&
                                    m.signature.parameters.length == 0 &&
                                    m.signature.modifiersMask == Modifier.PUBLIC
                        }) &&

                                assertMethod(c, simplyNamed("barDef"), { MethodInfo m ->
                                    m.signature.returnType == "def" &&
                                            m.signature.parameters.every { Parameter param -> param.name =~ /param[0-9]/ && param.type == "def" } &&
                                            m.signature.modifiersMask == Modifier.PUBLIC
                                }) &&

                                assertMethod(c, simplyNamed("barUntypedParams"), { MethodInfo m ->
                                    m.signature.returnType == "def" &&
                                            m.signature.parameters.every { Parameter param -> param.name =~ /param[0-9]/ && param.type == "def" } &&
                                            m.signature.modifiersMask == Modifier.PUBLIC
                                }) &&

                                assertMethod(c, simplyNamed("barClosure"), { MethodInfo m ->
                                    m.signature.returnType == "def" &&
                                            m.signature.parameters.every { Parameter param -> param.name == "c" && param.type == "Closure" } &&
                                            m.signature.modifiersMask == Modifier.PUBLIC
                                }) &&

                                assertMethod(c, simplyNamed("barDefaults"), { MethodInfo m ->
                                    m.signature.returnType == "def" &&
                                            (m.signature.parameters[0].name == "c" && m.signature.parameters[0].type == "Closure") &&
                                            (m.signature.parameters[1].name == "m" && m.signature.parameters[1].type == "def") &&
                                            (m.signature.parameters[2].name == "n" && m.signature.parameters[2].type == "def") &&
                                            m.signature.modifiersMask == Modifier.PUBLIC
                                }) &&

                                assertMethod(c, simplyNamed("barPrivateVarArg"), { MethodInfo m ->
                                    m.signature.returnType == "Object" &&
                                            m.signature.parameters.every { Parameter param -> param.name == "f" && param.type == "Foo[]" } &&
                                            m.signature.modifiersMask == Modifier.PRIVATE
                                }) &&

                                assertMethod(c, simplyNamed("barTypeParam"), { MethodInfo m ->
                                    m.signature.returnType == "T" &&
                                            m.signature.parameters.every { Parameter param -> param.name == "t" && param.type == "T" } &&
                                            m.signature.modifiersMask == Modifier.PRIVATE &&
                                            m.signature.typeParams == "T"
                                }) &&

                                assertMethod(c, simplyNamed("barGenericsFrenzy"), { MethodInfo m ->
                                    m.signature.returnType == "Map<V, Set<T>>" &&
                                            m.signature.parameters[0].with { param -> param.name == "t" && param.type == "Collection<T>" } &&
                                            m.signature.parameters[1].with { param -> param.name == "s" && param.type == "Map<? extends Serializable, Class<V>>" } &&
                                            m.signature.parameters[2].with { param -> param.name == "q" && param.type == "Collection<?>" } &&
                                            m.signature.modifiersMask == Modifier.PRIVATE &&
                                            m.signature.typeParams == "V, T extends List<V>"
                                }) &&

                                assertMethod(c, simplyNamed("barAnnotated"), { MethodInfo m ->
                                    m.signature.returnType == "void" &&
                                            m.signature.parameters.length == 0 &&
                                            m.signature.modifiersMask == Modifier.PUBLIC &&
                                            m.signature.modifiers.annotations.size() == 2 &&
                                            m.signature.modifiers.annotations.containsKey("java.lang.SuppressWarnings") &&
                                            m.signature.modifiers.annotations.get("java.lang.SuppressWarnings").with { Collection<Annotation> aaa ->
                                                aaa.size() == 1 &&
                                                        aaa.any { Annotation a ->
                                                            a.name == "java.lang.SuppressWarnings" &&
                                                                    a.attributes.size() == 1 &&
                                                                    a.attributes.containsKey("value") &&
                                                                    a.attributes.get("value") instanceof StringifiedAnnotationValue &&
                                                                    (a.attributes.get("value") as StringifiedAnnotationValue).value == "unchecked"
                                                        }
                                            } &&
                                            m.signature.modifiers.annotations.containsKey("MyAnnotation") &&
                                            m.signature.modifiers.annotations.get("MyAnnotation").with { Collection<Annotation> aaa ->
                                                aaa.size() == 1 &&
                                                        aaa.any { Annotation a ->
                                                            a.name == "MyAnnotation" &&
                                                                    a.attributes.size() == 5 &&
                                                                    a.attributes.get("someAnno") instanceof AnnotationImpl &&
                                                                    (a.attributes.get("someAnno") as AnnotationImpl).name == "MyOtherAnnotation" &&
                                                                    a.attributes.get("someNum") instanceof StringifiedAnnotationValue &&
                                                                    (a.attributes.get("someNum") as StringifiedAnnotationValue).value == "12" &&
                                                                    a.attributes.get("someBool") instanceof StringifiedAnnotationValue &&
                                                                    (a.attributes.get("someBool") as StringifiedAnnotationValue).value == "true" &&
                                                                    a.attributes.get("someClass") instanceof StringifiedAnnotationValue &&
                                                                    (a.attributes.get("someClass") as StringifiedAnnotationValue).value == "java.lang.String" &&
                                                                    a.attributes.get("someArray") instanceof ArrayAnnotationValue &&
                                                                    (a.attributes.get("someArray") as ArrayAnnotationValue).values.size() == 3 &&
                                                                    (a.attributes.get("someArray") as ArrayAnnotationValue).values.every {
                                                                        it instanceof StringifiedAnnotationValue
                                                                    } &&
                                                                    ((a.attributes.get("someArray") as ArrayAnnotationValue).values[0] as StringifiedAnnotationValue).value == "1" &&
                                                                    ((a.attributes.get("someArray") as ArrayAnnotationValue).values[1] as StringifiedAnnotationValue).value == "2" &&
                                                                    ((a.attributes.get("someArray") as ArrayAnnotationValue).values[2] as StringifiedAnnotationValue).value == "3"
                                                        }
                                            }
                                }) &&

                                assertMethod(c, simplyNamed("barArrayFrenzy"), { MethodInfo m ->
                                    m.signature.returnType == "int[][][]" &&
                                            m.signature.parameters[0].with { param -> param.name == "a" && param.type == "String[]" } &&
                                            m.signature.modifiersMask == Modifier.PRIVATE
                                })
                    }
                }
            }
        }
    }

    /** Prior to 1.7 methods with default args appear as two or more methods, default variants with -1 for src positions */
    public void testMethodWithDefaultArgAddedToRegistryOnlyOnce() {
        instrumentAndCompileWithGrover(
                ["Foo.groovy":
                         """
              public class Foo {
                public void bar(a = 1) {
                }
              }
            """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, { it.isDefault() }, { FullPackageInfo p ->
                assertFile p, named("Foo.groovy"), { FullFileInfo f ->
                    assertClass f, named("Foo"), { FullClassInfo c ->
                        c.methods.size() == 1 &&
                                assertMethod(c, named("bar(def) : void"))
                    }
                }
            }
        }
    }

    public void testDefaultTestDetection() {
        instrumentAndCompileWithGrover(
                ["Foo1Test.groovy":
                         """
              public class Foo1Test {
                public void testIt() {}
              }
            """,
                 "Foo2.groovy"    :
                         """
              import junit.framework.TestCase

              public class Foo2 extends TestCase {
                public void testIt() {}
              }
            """])

        assertRegistry db, { Clover2Registry reg ->
            assertPackage Clover2Registry.fromFile(db).model.project, { it.isDefault() }, { FullPackageInfo p ->
                assertFile(p, named("Foo1Test.groovy")) { FullFileInfo f ->
                    assertClass f, { it.name == "Foo1Test" && it.isTestClass() }, { FullClassInfo c ->
                        assertMethod c, {
                            it.simpleName == "testIt" && it.signature.returnType == "void" && it.signature.parameters.length == 0 && it.isTest()
                        }
                    }
                } &&
                        assertFile(p, named("Foo2.groovy")) { FullFileInfo f ->
                            assertClass f, named("Foo2"), { FullClassInfo c ->
                                c.isTestClass() &&
                                        assertMethod(c, simplyNamed("testIt")) { MethodInfo m ->
                                            m.signature.returnType == "void" && m.signature.parameters.length == 0 && m.isTest()
                                        }
                            }
                        }
            }
        }
    }

}
