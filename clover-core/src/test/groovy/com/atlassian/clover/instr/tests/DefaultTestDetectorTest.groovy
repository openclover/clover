package com.atlassian.clover.instr.tests

import clover.com.google.common.collect.Maps
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig
import com.atlassian.clover.instr.java.FileStructureInfo
import com.atlassian.clover.instr.java.InstrumentationState
import com.atlassian.clover.instr.java.JavaMethodContext
import com.atlassian.clover.instr.java.JavaTypeContext
import com.atlassian.clover.registry.PersistentAnnotationValue
import com.atlassian.clover.registry.entities.AnnotationImpl
import com.atlassian.clover.registry.entities.ArrayAnnotationValue
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import com.atlassian.clover.registry.entities.Parameter
import com.atlassian.clover.registry.entities.StringifiedAnnotationValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import java.lang.reflect.Modifier

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class DefaultTestDetectorTest {
    private InstrumentationState state

    @Rule
    public TestName testName = new TestName()

    @Before
    void setUp() {
        state = new InstrumentationState(null, null, new FileStructureInfo(new File(testName.methodName)), new JavaInstrumentationConfig())
    }

    @Test
    void testCtorsForClassesWithNamesContainingTest() {
        DefaultTestDetector detector = new DefaultTestDetector()

        MethodSignature method = new MethodSignature(null, null, null, "Test", null, null, null, null)
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))

        method = new MethodSignature(null, null, null, "testIt", null, null, null, null)
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))
    }

    @Test
    void testSuperClassContainsTestAndMethodStartsWithTest() {
        DefaultTestDetector detector = new DefaultTestDetector()

        assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, null, null, "DifferentName", "TestCase")))
        assertFalse(detector.isTypeMatch(state, new JavaTypeContext(null, null, null, "DifferentName", "NotTestCase")))
        assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, null, null, "MyUnitTest", "SuperClass")))
    }
    
    @Test
    void testJUnit38() {
        DefaultTestDetector detector = new DefaultTestDetector()
        testJUnit3(detector)
        state.getCfg().setSourceLevel("1.3")
        assertFalse(detector.isTypeMatch(state, new JavaTypeContext(null, null, "", "SimpleTest", "foo")))
    }

    @Test
    void testJUnit4() {
        DefaultTestDetector detector = new DefaultTestDetector()
        testJUnit3(detector)
        testMethodAnnotations(detector, TestAnnotationNames.ORG_JUNIT_NAME)

        state.getCfg().setSourceLevel("1.5")
        MethodSignature method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        method.setModifiers(Modifier.PUBLIC)
        method.getModifiers().addAnnotation(new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        method.getModifiers().addAnnotation(new AnnotationImpl(TestAnnotationNames.IGNORE_ANNO_NAME))
        method.getModifiers().clearAnnotations()
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))
        method.getModifiers().addAnnotation(new AnnotationImpl(TestAnnotationNames.JUNIT_TEST_ANNO_NAME))
        method.getModifiers().addAnnotation(new AnnotationImpl(TestAnnotationNames.JUNIT_IGNORE_ANNO_NAME))
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))
    }

    @Test
    void testJUnit5() {
        DefaultTestDetector detector = new DefaultTestDetector()
        testJUnit3(detector)
        testMethodAnnotations(detector, TestAnnotationNames.ORG_JUNIT5_NAME)

        state.getCfg().setSourceLevel("1.5")
        MethodSignature method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        // No visibility modifier is provided to be package private.
        method.getModifiers().addAnnotation(new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        method.getModifiers().addAnnotation(new AnnotationImpl(TestAnnotationNames.IGNORE_ANNO_NAME))
        method.getModifiers().clearAnnotations()
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))
        method.getModifiers().addAnnotation(new AnnotationImpl(TestAnnotationNames.JUNIT5_TEST_ANNO_NAME))
        method.getModifiers().addAnnotation(new AnnotationImpl(TestAnnotationNames.JUNIT5_IGNORE_ANNO_NAME))
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))
    }

    @Test
    void testTestNG() {
        DefaultTestDetector detector = new DefaultTestDetector()
        testMethodAnnotations(detector, "org.testng.annotations")
        state.getCfg().setSourceLevel("1.5")

        Map tags = Maps.newHashMap()
        tags.put("test", Collections.singletonList(""))
        MethodSignature method = new MethodSignature(null, null, null, tags, new Modifiers(), "checkFoo", null, "void", null, null)
        method.setModifiers(Modifier.PUBLIC)
        assertTrue(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))
        Modifiers mods = new Modifiers()
        mods.addAnnotation(    new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, mods, "", "CheckFoo", null)))
        mods.clearAnnotations()
        mods.addAnnotation(new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME))
        assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, mods, "", "CheckFoo", null)))

        // tagged test with params
        Parameter[] params = new Parameter[2]
        for (int i = 0; i < params.length; i++) {
            params[i] = new Parameter("type"+i,"ident"+i)
        }

        tags = Maps.newHashMap()
        tags.put("test", Collections.singletonList(""))
        method = new MethodSignature(null, null, null, tags, new Modifiers(), "checkFoo", null, "void", params, null)
        method.setModifiers(Modifier.PUBLIC)
        assertTrue(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))
    }

    @Test
    void testSpockFrameworkPositiveMatchFullyQualifiedName() {
        DefaultTestDetector detector = new DefaultTestDetector()

        // positive match - fully qualified name, all attributes
        AnnotationImpl featureAnno = new AnnotationImpl(TestAnnotationNames.SPOCK_METHOD_FQ_ANNO_NAME)
        featureAnno.put("parameterNames", new ArrayAnnotationValue())
        featureAnno.put("name", new StringifiedAnnotationValue("this is my test"))
        featureAnno.put("ordinal", new StringifiedAnnotationValue("0"))
        featureAnno.put("line", new StringifiedAnnotationValue("123"))
        featureAnno.put("blocks", new ArrayAnnotationValue())

        Modifiers featureMods = new Modifiers()
        featureMods.setMask(Modifier.PUBLIC)
        featureMods.addAnnotation(featureAnno)

        MethodSignature featureMethod = new MethodSignature("theSpockFeature", null, "void", null, null, featureMods)
        assertTrue(detector.isMethodMatch(state, JavaMethodContext.createFor(featureMethod)))
    }

    @Test
    void testSpockFrameworkPositiveMatchShortName() {
        DefaultTestDetector detector = new DefaultTestDetector()

        // positive match - basic class name (manually annotated?), no attributes
        Modifiers featureMods2 = new Modifiers()
        featureMods2.setMask(Modifier.PUBLIC)
        featureMods2.addAnnotation(new AnnotationImpl(TestAnnotationNames.SPOCK_METHOD_ANNO_NAME))
        MethodSignature featureMethod2 = new MethodSignature("theSpockFeature", null, "void", null, null, featureMods2)
        assertTrue(detector.isMethodMatch(state, JavaMethodContext.createFor(featureMethod2)))
    }

    @Test
    void testSpockFrameworkNegativeMatch() {
        DefaultTestDetector detector = new DefaultTestDetector()

        // negative match
        MethodSignature nonFeatureMethod = new MethodSignature("theSpockFeature", null, "void", null, null, new Modifiers())
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(nonFeatureMethod)))
    }

    @Test
    void testJDK14NoExpectedAnnotations() {
        state.getCfg().setSourceLevel("1.4")

        //No expected annotations
        MethodSignature method = new MethodSignature(null, null, null, "testFoo", null, "void", null, null)
        method.setModifiers(Modifier.PUBLIC)

        List expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(0, expectedExceptions.size())
    }

    @Test
    void testJDK15NoExpectedAnnotations() {
        state.getCfg().setSourceLevel("1.5")

        //No expected annotations
        MethodSignature method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        Modifiers modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME))

        List expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(0, expectedExceptions.size())

        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(0, expectedExceptions.size())

        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.JUNIT_TEST_ANNO_NAME))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(0, expectedExceptions.size())
    }

    @Test
    void testSingleExpectedAnnotations() {
        state.getCfg().setSourceLevel("1.5")

        //@org.testng.annotations.Test(expectedExceptions=Foo.class)
        MethodSignature method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        Modifiers modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME))
            .put(TestAnnotationNames.EXPECTED_EXCEPTIONS_ATTR_NAME, new StringifiedAnnotationValue("Foo.class"))

        List expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("Foo"))

        //@org.testng.annotations.Test(expectedExceptions={org.bar.Foo.class})
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME))
            .put(
                TestAnnotationNames.EXPECTED_EXCEPTIONS_ATTR_NAME,
                new ArrayAnnotationValue(
                        [ new StringifiedAnnotationValue("org.bar.Foo.class") ] as PersistentAnnotationValue[]))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("org.bar.Foo"))

        //@Test(expectedExceptions={Foo.class})
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
            .put(
                TestAnnotationNames.EXPECTED_EXCEPTIONS_ATTR_NAME,
                new ArrayAnnotationValue(
                        [ new StringifiedAnnotationValue("Foo.class") ] as PersistentAnnotationValue[]))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("Foo"))

        //@Test(expectedExceptions=org.bar.Foo.class)
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
            .put(TestAnnotationNames.EXPECTED_EXCEPTIONS_ATTR_NAME, new StringifiedAnnotationValue("org.bar.Foo.class"))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("org.bar.Foo"))

        //@org.testng.annotations.ExpectedExceptions(Foo.class)
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME))
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_EXPECTED_ANNO_NAME))
            .put(TestAnnotationNames.VALUE_ATTR_NAME, new StringifiedAnnotationValue("Foo.class"))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("Foo"))

        //@ExpectedExceptions(org.bar.Foo.class)
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME))
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TESTNG_EXPECTED_ANNO_NAME))
            .put(TestAnnotationNames.VALUE_ATTR_NAME, new StringifiedAnnotationValue("org.bar.Foo.class"))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("org.bar.Foo"))

        //@org.springframework.test.annotation.ExpectedException(Foo.class)
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.SPRING_FQ_EXPECTED_ANNO_NAME))
            .put(TestAnnotationNames.VALUE_ATTR_NAME, new StringifiedAnnotationValue("Foo.class"))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("Foo"))

        //@ExpectedException(org.bar.Foo.class)
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.SPRING_FQ_EXPECTED_ANNO_NAME))
            .put(TestAnnotationNames.VALUE_ATTR_NAME, new StringifiedAnnotationValue("org.bar.Foo.class"))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("org.bar.Foo"))

        //@Test(expected=Foo.class)
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
            .put(TestAnnotationNames.EXPECTED_ATTR_NAME, new StringifiedAnnotationValue("Foo.class"))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("Foo"))

        //@junit.org.Test(expected=Foo.class)
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.JUNIT_TEST_ANNO_NAME))
            .put(TestAnnotationNames.EXPECTED_ATTR_NAME, new StringifiedAnnotationValue("Foo.class"))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("Foo"))

        //@junit.org.Test(expected={Foo.class})
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.JUNIT_TEST_ANNO_NAME))
            .put(TestAnnotationNames.EXPECTED_ATTR_NAME, new StringifiedAnnotationValue("Foo.class"))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("Foo"))

        //@com.googlecode.instinct.marker.annotate.Specification(expectedException=Foo.class)
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.INSTINCT_SPECIFICATION_ANNO_NAME))
            .put(TestAnnotationNames.EXPECTED_EXCEPTION_ATTR_NAME, new StringifiedAnnotationValue("Foo.class"))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("Foo"))

        //@Specification(expectedException=Foo.class)
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.SPECIFICATION_ANNO_NAME))
            .put(TestAnnotationNames.EXPECTED_EXCEPTION_ATTR_NAME, new StringifiedAnnotationValue("Foo.class"))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(1, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("Foo"))
    }

    @Test
    void testMultipleExpectedAnnotations() {
        state.getCfg().setSourceLevel("1.5")

        //@org.testng.annotations.Test(expectedExceptions={org.bar.Foo.class,Bar.class})
        MethodSignature method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        Modifiers modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        AnnotationImpl anno = addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME))
        anno.put(
            TestAnnotationNames.EXPECTED_EXCEPTIONS_ATTR_NAME,
            new ArrayAnnotationValue(
                [
                    new StringifiedAnnotationValue("org.bar.Foo.class"),
                    new StringifiedAnnotationValue("Bar.class")
                ] as PersistentAnnotationValue[]))

        List expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(2, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("org.bar.Foo"))
        assertTrue(expectedExceptions.contains("Bar"))

        //@Test(expectedExceptions={org.bar.Foo.class,Bar.class})
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        anno = addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        anno.put(
            TestAnnotationNames.EXPECTED_EXCEPTIONS_ATTR_NAME,
            new ArrayAnnotationValue(
                [
                    new StringifiedAnnotationValue("org.bar.Foo.class"),
                    new StringifiedAnnotationValue("Bar.class")
                ] as PersistentAnnotationValue[]))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(2, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("org.bar.Foo"))
        assertTrue(expectedExceptions.contains("Bar"))

        //@Test @org.testng.annotations.ExpectedExceptions({org.bar.Foo.class,Bar.class})
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        anno = addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_EXPECTED_ANNO_NAME))
        anno.put(
            TestAnnotationNames.VALUE_ATTR_NAME,
            new ArrayAnnotationValue(
                [
                    new StringifiedAnnotationValue("org.bar.Foo.class"),
                    new StringifiedAnnotationValue("Bar.class")
                ] as PersistentAnnotationValue[]))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(2, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("org.bar.Foo"))
        assertTrue(expectedExceptions.contains("Bar"))

        //@Test @ExpectedExceptions(value={org.bar.Foo.class,Bar.class})
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        anno = addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TESTNG_EXPECTED_ANNO_NAME))
        anno.put(
            TestAnnotationNames.VALUE_ATTR_NAME,
            new ArrayAnnotationValue(
                [
                    new StringifiedAnnotationValue("org.bar.Foo.class"),
                    new StringifiedAnnotationValue("Bar.class")
                ] as PersistentAnnotationValue[]))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(2, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("org.bar.Foo"))
        assertTrue(expectedExceptions.contains("Bar"))

        //@Test @org.springframework.test.annotation.ExpectedException({org.bar.Foo.class,Bar.class})
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        anno = addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.SPRING_FQ_EXPECTED_ANNO_NAME))
        anno.put(
            TestAnnotationNames.VALUE_ATTR_NAME,
            new ArrayAnnotationValue(
                [
                    new StringifiedAnnotationValue("org.bar.Foo.class"),
                    new StringifiedAnnotationValue("Bar.class")
                ] as PersistentAnnotationValue[]))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(2, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("org.bar.Foo"))
        assertTrue(expectedExceptions.contains("Bar"))

        //@Test @ExpectedException(value={org.bar.Foo.class,Bar.class})
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        modifiers.addAnnotation(new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        anno = addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.SPRING_EXPECTED_ANNO_NAME))
        anno.put(
            TestAnnotationNames.VALUE_ATTR_NAME,
            new ArrayAnnotationValue(
                [
                    new StringifiedAnnotationValue("org.bar.Foo.class"),
                    new StringifiedAnnotationValue("Bar.class")
                ] as PersistentAnnotationValue[]))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(2, expectedExceptions.size())
        assertTrue(expectedExceptions.contains("org.bar.Foo"))
        assertTrue(expectedExceptions.contains("Bar"))
    }

    private static AnnotationImpl addAnnotation(Modifiers modifiers, AnnotationImpl anno) {
        modifiers.addAnnotation(anno)
        return anno
    }

    @Test
    void testWeirdExpectedAnnotationStructure() {
        state.getCfg().setSourceLevel("1.5")

        //@org.testng.annotations.Test(expectedExceptions={"foo","Bar.class"})
        MethodSignature method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        Modifiers modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        AnnotationImpl anno = addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME))
        anno.put(
            TestAnnotationNames.EXPECTED_EXCEPTIONS_ATTR_NAME,
            new ArrayAnnotationValue(
                [
                    new StringifiedAnnotationValue("\"foo\""),
                    new StringifiedAnnotationValue("\"Bar.class\"")
                ] as PersistentAnnotationValue[]))

        List expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(0, expectedExceptions.size())

        //@org.testng.annotations.Test(
        // expectedExceptions={
        //  (!(true)) == false ? Bar.class : Foo.class,
        //     FOO.DEBUG?Foo.class:Bar.class,
        //  FOO.DEBUG?true:false,
        //  @Foo})
        method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        modifiers = method.getModifiers()
        modifiers.setMask(Modifier.PUBLIC)
        anno = addAnnotation(modifiers, new AnnotationImpl(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME))
        anno.put(
            TestAnnotationNames.EXPECTED_EXCEPTIONS_ATTR_NAME,
            new ArrayAnnotationValue(
                [
                    new StringifiedAnnotationValue("(!(true)) == false ? Bar.class : Foo.class"),
                    new StringifiedAnnotationValue("FOO.DEBUG?Foo.class:Bar.class"),
                    new StringifiedAnnotationValue("FOO.DEBUG?Foo.class:Bar.class"),
                    new StringifiedAnnotationValue("FOO.DEBUG?true:false"),
                    new AnnotationImpl("Foo")
                ] as PersistentAnnotationValue[]))

        expectedExceptions = Arrays.asList(ExpectedExceptionMiner.extractExpectedExceptionsFor(method, true))
        assertEquals(0, expectedExceptions.size())
    }

    private void testMethodAnnotations(DefaultTestDetector detector, String pkg) {
        MethodSignature method = new MethodSignature(null, null, null, "checkFoo", null, "void", null, null)
        method.getModifiers().addAnnotation(new AnnotationImpl(TestAnnotationNames.TEST_ANNO_NAME))
        method.setModifiers(Modifier.PUBLIC)
        state.getCfg().setSourceLevel("1.5")
        assertTrue(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))

        method.setModifiers(Modifier.PRIVATE)
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))

        method.setModifiers(Modifier.PUBLIC|Modifier.ABSTRACT)
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))

        method.getModifiers().clearAnnotations()

        method.setModifiers(Modifier.PUBLIC)
        method.getModifiers().addAnnotation(new AnnotationImpl(pkg + ".Test"))
        assertTrue(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))
    }

    private void testJUnit3(TestDetector detector) {
        state.getCfg().setSourceLevel("1.3")
        assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, null, "", "SimpleTest", "TestCase")))
        assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, null, "", "TestSimple", "TestCase")))
        assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, null, "", "SimpleTest", "junit.framework.TestCase")))

        assertTrue(detector.isTypeMatch(state, new JavaTypeContext(null, null, "", "Simple", "TestCase")))

        MethodSignature method = new MethodSignature(null, null, null, "testSomething", null, "void", null, null)

        method.setModifiers(Modifier.PUBLIC)
        assertTrue(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))

        method.setModifiers(Modifier.PRIVATE)
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))

        method.setModifiers(Modifier.PUBLIC|Modifier.ABSTRACT)
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))

        method.setName("notTestingAnything")
        assertFalse(detector.isMethodMatch(state, JavaMethodContext.createFor(method)))
    }

}
