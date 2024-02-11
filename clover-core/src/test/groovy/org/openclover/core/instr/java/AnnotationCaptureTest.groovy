package org.openclover.core.instr.java

import com.atlassian.clover.api.registry.Annotation
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig
import com.atlassian.clover.cfg.instr.java.SourceLevel
import com.atlassian.clover.instr.java.InstrumentationSource
import com.atlassian.clover.instr.java.InstrumentationState
import com.atlassian.clover.instr.java.Instrumenter
import com.atlassian.clover.instr.java.StringInstrumentationSource
import com.atlassian.clover.instr.tests.TestDetector
import com.atlassian.clover.registry.entities.AnnotationImpl
import com.atlassian.clover.util.FileUtils
import com.atlassian.clover.registry.entities.ArrayAnnotationValue
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.StringifiedAnnotationValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertSame

class AnnotationCaptureTest {
    private JavaInstrumentationConfig config
    private File workingDir

    @Rule
    public TestName testName = new TestName()

    @Before
    void setUp() throws Exception {
        workingDir = File.createTempFile(getClass().getName() + "." + testName.methodName, "tmp")
        workingDir.delete()
        workingDir.mkdir()
        workingDir.deleteOnExit()

        config = new JavaInstrumentationConfig()
        config.setSourceLevel(SourceLevel.JAVA_8)
        config.setInitstring(workingDir.getAbsolutePath() + File.separatorChar + "coverage.db")
    }

    @After
    void tearDown() throws Exception {
        FileUtils.deltree(workingDir)
    }

    @Test
    void testNoAnnotation() throws Exception {
        compile("public class MyClass { public void aMethod() {}}", new BaseMockTestDetector() {
            public boolean isTypeMatch(TestDetector.SourceContext sourceContext, TestDetector.TypeContext typeContext) {
                assertEquals("There should be no class annotations", typeContext.getModifiers().getAnnotations().size(), 0)
                return false
            }
            public boolean isMethodMatch(TestDetector.SourceContext sourceContext, TestDetector.MethodContext methodContext) {
                assertEquals("There should be no method annotation", methodContext.getSignature().getModifiers().getAnnotations().size(), 0)
                return false
            }
        })
    }

    @Test
    void testSingleSimpleClassAndMethodAnnotation() throws Exception {
        compile("public @Foo class MyClass { public @Bar void aMethod() {}}", new BaseMockTestDetector() {
            public boolean isTypeMatch(TestDetector.SourceContext sourceContext, TestDetector.TypeContext typeContext) {
                assertEquals("There should be one class annotation",
                        typeContext.getModifiers().getAnnotations().size(), 1)
                assertEquals("There should be one class annotation, called Foo",
                        typeContext.getModifiers().getAnnotations().get("Foo").iterator().next().getName(), "Foo")
                return false
            }
            public boolean isMethodMatch(TestDetector.SourceContext sourceContext, TestDetector.MethodContext methodContext) {
                assertEquals("There should be one method annotation",
                        methodContext.getSignature().getModifiers().getAnnotations().size(), 1)
                assertEquals("There should be one method annotation, called Bar",
                        methodContext.getSignature().getModifiers().getAnnotations().get("Bar").iterator().next().getName(),
                        "Bar")
                return false
            }
        })
    }

    @Test
    void testMultipleClassAnnotations() throws Exception {
        compile("public @Foo @Bar class MyClass { public void aMethod() {}}", new BaseMockTestDetector() {
            public boolean isTypeMatch(TestDetector.SourceContext sourceContext, TestDetector.TypeContext typeContext) {
                assertEquals("There should be two class annotations",
                        typeContext.getModifiers().getAnnotations().size(), 2)
                assertEquals("The first class annotation should be called Foo",
                        typeContext.getModifiers().getAnnotations().get("Foo").iterator().next().getName(), "Foo")
                assertEquals("The second class annotation should be called Bar",
                        typeContext.getModifiers().getAnnotations().get("Bar").iterator().next().getName(), "Bar")
                return false
            }
            public boolean isMethodMatch(TestDetector.SourceContext sourceContext, TestDetector.MethodContext methodContext) {
                assertEquals("There should be no method annotation", methodContext.getSignature().getModifiers().getAnnotations().size(), 0)
                return false
            }
        })
    }

    @Test
    void testNestedClassAnnotation() throws Exception {
        compile("public @Foo(attr=@Bar) class MyClass { public void aMethod() {}}", new BaseMockTestDetector() {
            public boolean isTypeMatch(TestDetector.SourceContext sourceContext, TestDetector.TypeContext typeContext) {
                assertEquals("There should be one class annotations", typeContext.getModifiers().getAnnotations().size(), 1)

                // grab the first value from a collection (there should be exactly one)
                Annotation outerAnnotation = typeContext.getModifiers().getAnnotations().get("Foo").iterator().next()
                assertEquals("The annotation should be called Foo", outerAnnotation.getName(), "Foo")
                assertEquals("There should be one nested class annotation", outerAnnotation.getAttributes().size(), 1)
                assertTrue("There should be one nested class annotation attribute with name 'attr'", outerAnnotation.getAttributes().containsKey("attr"))
                assertSame("There should be one nested class annotation attribute with value of type Annotation", outerAnnotation.getAttributes().get("attr").getClass(), AnnotationImpl.class)
                assertEquals("There should be one nested class annotation attribute of type Annotation without any attributes", ((AnnotationImpl) outerAnnotation.getAttributes().get("attr")).getAttributes().size(), 0)
                assertEquals("There should be one nested class annotation attribute of type Annotation with name of 'Bar'", ((AnnotationImpl) outerAnnotation.getAttributes().get("attr")).getName(), "Bar")
                return false
            }
            public boolean isMethodMatch(TestDetector.SourceContext sourceContext, TestDetector.MethodContext methodContext) {
                assertEquals("There should be no method annotation", methodContext.getSignature().getModifiers().getAnnotations().size(), 0)
                return false
            }
        })
    }

    @Test
    void testAnnotationWithArrayValues() throws Exception {
        compile("public @Foo(attr={0,1,2}) class MyClass { public void aMethod() {}}", new BaseMockTestDetector() {
            public boolean isTypeMatch(TestDetector.SourceContext sourceContext, TestDetector.TypeContext typeContext) {
                assertEquals("There should be one class annotations", typeContext.getModifiers().getAnnotations().size(), 1)

                // grab the first value from a collection (there should be exactly one)
                Annotation outerAnnotation = typeContext.getModifiers().getAnnotations().get("Foo").iterator().next()
                assertEquals("The annotation should be called Foo", outerAnnotation.getName(), "Foo")
                assertEquals("There should be one class annotation value", outerAnnotation.getAttributes().size(), 1)
                assertTrue("There should be one class annotation attribute with name 'attr'", outerAnnotation.getAttributes().containsKey("attr"))
                assertSame("There should be one class annotation attribute with value of type ArrayAnnotationValue", outerAnnotation.getAttributes().get("attr").getClass(), ArrayAnnotationValue.class)
                ArrayAnnotationValue attrAnnotationValue = (ArrayAnnotationValue) outerAnnotation.getAttributes().get("attr")
                assertEquals("There should be one class annotation attribute of type ArrayAnnotationValue with three values", attrAnnotationValue.getValues().size(), 3)
                assertEquals("There should be one class annotation attribute of type ArrayAnnotationValue with first value \"0\"", ((StringifiedAnnotationValue)attrAnnotationValue.getValues().get(0)).getValue(), "0")
                assertEquals("There should be one class annotation attribute of type ArrayAnnotationValue with first value \"1\"", ((StringifiedAnnotationValue)attrAnnotationValue.getValues().get(1)).getValue(), "1")
                assertEquals("There should be one class annotation attribute of type ArrayAnnotationValue with first value \"2\"", ((StringifiedAnnotationValue)attrAnnotationValue.getValues().get(2)).getValue(), "2")
                return false
            }
            public boolean isMethodMatch(TestDetector.SourceContext sourceContext, TestDetector.MethodContext methodContext) {
                assertEquals("There should be no method annotation", methodContext.getSignature().getModifiers().getAnnotations().size(), 0)
                return false
            }
        })
    }

    @Test
    void testAnnotationWithTernaryExpression() throws Exception {
        compile("public @Foo(attr=1==1?true:false) class MyClass { public void aMethod() {}}", new BaseMockTestDetector() {
            public boolean isTypeMatch(TestDetector.SourceContext sourceContext, TestDetector.TypeContext typeContext) {
                assertEquals("There should be one class annotations", typeContext.getModifiers().getAnnotations().size(), 1)

                // grab the first value from a collection (there should be exactly one)
                Annotation outerAnnotation = typeContext.getModifiers().getAnnotations().get("Foo").iterator().next()
                assertEquals("The annotation should be called Foo", outerAnnotation.getName(), "Foo")
                assertEquals("There should be one class annotation value", outerAnnotation.getAttributes().size(), 1)
                assertTrue("There should be one class annotation attribute with name 'attr'", outerAnnotation.getAttributes().containsKey("attr"))
                assertSame("There should be one class annotation attribute with value of type StringifiedAnnotationValue", outerAnnotation.getAttributes().get("attr").getClass(), StringifiedAnnotationValue.class)
                StringifiedAnnotationValue attrAnnotationValue = (StringifiedAnnotationValue ) outerAnnotation.getAttributes().get("attr")
                assertEquals("There should be one class annotation attribute of type StringifiedAnnotationValue with value \"1==1?true:false\"", attrAnnotationValue.getValue(), "1==1?true:false")
                return false
            }
            public boolean isMethodMatch(TestDetector.SourceContext sourceContext, TestDetector.MethodContext methodContext) {
                assertEquals("There should be no method annotation", methodContext.getSignature().getModifiers().getAnnotations().size(), 0)
                return false
            }
        })
    }

    @Test
    void testAnnotationWithArrayValuesForDefault() throws Exception {
        compile("public @Foo({0,1,2}) class MyClass { public void aMethod() {}}", new BaseMockTestDetector() {
            public boolean isTypeMatch(TestDetector.SourceContext sourceContext, TestDetector.TypeContext typeContext) {
                assertEquals("There should be one class annotations", typeContext.getModifiers().getAnnotations().size(), 1)

                // grab the first value from a collection (there should be exactly one)
                Annotation outerAnnotation = typeContext.getModifiers().getAnnotations().get("Foo").iterator().next()
                assertEquals("The annotation should be called Foo", outerAnnotation.getName(), "Foo")
                assertEquals("There should be one class annotation value", outerAnnotation.getAttributes().size(), 1)
                assertTrue("There should be one class annotation attribute with name 'value'", outerAnnotation.getAttributes().containsKey("value"))
                assertSame("There should be one class annotation attribute with value of type ArrayAnnotationValue", outerAnnotation.getAttributes().get("value").getClass(), ArrayAnnotationValue.class)
                ArrayAnnotationValue attrAnnotationValue = (ArrayAnnotationValue) outerAnnotation.getAttributes().get("value")
                assertEquals("There should be one class annotation attribute of type ArrayAnnotationValue with three values", attrAnnotationValue.getValues().size(), 3)
                assertEquals("There should be one class annotation attribute of type ArrayAnnotationValue with first value \"0\"", ((StringifiedAnnotationValue)attrAnnotationValue.getValues().get(0)).getValue(), "0")
                assertEquals("There should be one class annotation attribute of type ArrayAnnotationValue with first value \"1\"", ((StringifiedAnnotationValue)attrAnnotationValue.getValues().get(1)).getValue(), "1")
                assertEquals("There should be one class annotation attribute of type ArrayAnnotationValue with first value \"2\"", ((StringifiedAnnotationValue)attrAnnotationValue.getValues().get(2)).getValue(), "2")
                return false
            }
            public boolean isMethodMatch(TestDetector.SourceContext sourceContext, TestDetector.MethodContext methodContext) {
                assertEquals("There should be no method annotation", methodContext.getSignature().getModifiers().getAnnotations().size(), 0)
                return false
            }
        })
    }

    @Test
    void testAnnotationWithHodgePodgeValues() throws Exception {
        compile("public @Foo(@Bar(a={0,1,2},b=@Bar2(1==1?true:false),c=\"123\")) class MyClass { public void aMethod() {}}", new BaseMockTestDetector() {
            public boolean isTypeMatch(TestDetector.SourceContext sourceContext, TestDetector.TypeContext typeContext) {
                assertEquals("There should be one class annotations", typeContext.getModifiers().getAnnotations().size(), 1)

                // grab the first value from a collection (there should be exactly one)
                Annotation outerAnnotation = typeContext.getModifiers().getAnnotations().get("Foo").iterator().next()
                assertEquals("The annotation should be called Foo", outerAnnotation.getName(), "Foo")
                assertEquals("There should be one class annotation value", outerAnnotation.getAttributes().size(), 1)
                assertTrue("There should be one class annotation attribute with name 'value'", outerAnnotation.getAttributes().containsKey("value"))
                assertSame("There should be one class annotation attribute with value of type Annotation", outerAnnotation.getAttributes().get("value").getClass(), AnnotationImpl.class)
                AnnotationImpl valueAnnotationValue = (AnnotationImpl) outerAnnotation.getAttributes().get("value")
                assertEquals("There should be one class annotation attribute of type Annotation with name \"Bar\"", valueAnnotationValue.getName(), "Bar")
                Map barAttributes = valueAnnotationValue.getAttributes()
                assertEquals("There should be one class annotation attribute of type Annotation with three values", barAttributes.size(), 3)
                assertTrue("Attribute names include 'a'", barAttributes.containsKey("a"))
                assertTrue("Attribute names include 'b'", barAttributes.containsKey("b"))
                assertTrue("Attribute names include 'c'", barAttributes.containsKey("c"))
                assertSame("Attribute 'a' is of type ArrayAnnotationValue", barAttributes.get("a").getClass(), ArrayAnnotationValue.class)
                assertSame("Attribute 'b' is of type Annotation", barAttributes.get("b").getClass(), AnnotationImpl.class)
                assertSame("Attribute 'c' is of type StringifiedValue", barAttributes.get("c").getClass(), StringifiedAnnotationValue.class)

                assertSame("Value of attribute 'a' is of type ArrayAnnotationValue", barAttributes.get("a").getClass(), ArrayAnnotationValue.class)
                ArrayAnnotationValue annotationValueOfA = (ArrayAnnotationValue) barAttributes.get("a")
                assertEquals("Value of attribute 'a' is of length 3", annotationValueOfA.getValues().size(), 3)
                assertEquals("Element 0 of value of attribute 'a' is 0", ((StringifiedAnnotationValue)annotationValueOfA.getValues().get(0)).getValue(), "0")
                assertEquals("Element 1 of value of attribute 'a' is 1", ((StringifiedAnnotationValue)annotationValueOfA.getValues().get(1)).getValue(), "1")
                assertEquals("Element 2 of value of attribute 'a' is 2", ((StringifiedAnnotationValue)annotationValueOfA.getValues().get(2)).getValue(), "2")

                assertSame("Value of attribute 'b' is of type Annotation", barAttributes.get("b").getClass(), AnnotationImpl.class)
                AnnotationImpl annotationValueOfB = (AnnotationImpl)barAttributes.get("b")
                assertEquals("Name of annotation value of attribute 'b' is Bar2", annotationValueOfB.getName(), "Bar2")
                assertEquals("Annotation value of attribute 'b' has one attribute", annotationValueOfB.getAttributes().size(), 1)
                assertTrue("Annotation value of attribute 'b' has a single attribute 'value'", annotationValueOfB.getAttributes().containsKey("value"))
                assertEquals("Annotation value of attribute 'b' has a single attribute of value \"1=1?true:false\"", ((StringifiedAnnotationValue)annotationValueOfB.getAttributes().get("value")).getValue(), "1==1?true:false")


                assertEquals("Value of attribute 'c' is \"123\"", ((StringifiedAnnotationValue)barAttributes.get("c")).getValue(), "\"123\"")
                return false
            }
            public boolean isMethodMatch(TestDetector.SourceContext sourceContext, TestDetector.MethodContext methodContext) {
                assertEquals("There should be no method annotation", methodContext.getSignature().getModifiers().getAnnotations().size(), 0)
                return false
            }
        })
    }

    void compile(final String cu, final TestDetector testDetector) throws Exception {
        final File cuFile = File.createTempFile(AnnotationCaptureTest.class.getName(), ".java", workingDir)
        cuFile.deleteOnExit()

        final FileWriter cuFileWriter = new FileWriter(cuFile)
        cuFileWriter.write(cu)
        cuFileWriter.close()

        config.setTestDetector(testDetector)

        final InstrumentationSource input = new StringInstrumentationSource(cuFile, cu)
        final Instrumenter instrumenter = new Instrumenter(config)
        instrumenter.startInstrumentation()
        try {
            instrumenter.instrument(input, new StringWriter(), null)
        } finally {
            try {
                instrumenter.endInstrumentation()
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    static class BaseMockTestDetector implements TestDetector {
        public boolean isTypeMatch(TestDetector.SourceContext sourceContext, TestDetector.TypeContext typeContext) {
            return false
        }

        public boolean isMethodMatch(TestDetector.SourceContext sourceContext, TestDetector.MethodContext methodContext) {
            return false
        }

        public String[] expectedExceptionsFor(InstrumentationState state, MethodSignature sig) {
            return new String[0]
        }
    }
}
