package com.atlassian.clover.ant.tasks

import clover.com.google.common.collect.Lists
import clover.com.google.common.collect.Maps
import com.atlassian.clover.instr.java.JavaMethodContext
import com.atlassian.clover.instr.java.JavaTypeContext
import com.atlassian.clover.instr.tests.DefaultTestDetector
import com.atlassian.clover.instr.tests.NoTestDetector
import com.atlassian.clover.instr.tests.TestDetector
import com.atlassian.clover.registry.entities.AnnotationImpl
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import com.atlassian.clover.spec.instr.test.AndSpec
import com.atlassian.clover.spec.instr.test.OrSpec
import com.atlassian.clover.spec.instr.test.TestClassSpec
import com.atlassian.clover.spec.instr.test.TestMethodSpec
import junit.framework.TestCase

class TestSourceSetTest extends TestCase {
    public Modifiers mods
    public TestClassSpec nameSetSpec
    public TestClassSpec pkgSetSpec
    public TestClassSpec javaDocSpec

    void setUp() {
        mods = new Modifiers()

        nameSetSpec = new TestClassSpec()
        nameSetSpec.setName(".*Test")
        pkgSetSpec = new TestClassSpec()
        pkgSetSpec.setPackage("com.atlassian.test.*")
        javaDocSpec = new TestClassSpec()
        javaDocSpec.setTag("testng\\.test")
    }

    void testEmpty() {
        TestSourceSet testSources = new TestSourceSet()
        testSources.validate()
        assertTrue(testSources.getDetector() instanceof DefaultTestDetector)
    }

    void testDisabled() {
        TestSourceSet testSources = new TestSourceSet()
        testSources.setEnabled(false)
        testSources.validate()
        assertTrue(testSources.getDetector() instanceof NoTestDetector)
    }

    /**
     * Tests a config with just a single &lt;testsources/&gt; fileset.
     */
    void testSingleTestSources() {
        // <testsources><testclass name=".*Test"></testclass></testsources>
        TestSourceSet testSources = new TestSourceSet()
        testSources.addConfiguredTestClass(nameSetSpec)

        testSources.validate()

        final TestDetector testDetector = testSources.getDetector()
        assertNotNull(testDetector)
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.test", "MatchingTest", null)))
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.test", "TestMatching", null)))
    }

    void testSingleJavaDocClass() {
        // <testsources>
        //     <testclass javadoc="testng\.testng"/>
        // <testsources>        
        TestSourceSet testSources = new TestSourceSet()
        testSources.addConfiguredTestClass(javaDocSpec)
        List values = Lists.newArrayList()
        values.add("value")

        testSources.validate()

        Map tags = Maps.newHashMap()
        tags.put("thisShouldNotMatch", values)

        final TestDetector testDetector = testSources.getDetector()
        assertNotNull(testDetector)
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(tags, mods, "com.atlassian.test", "MatchingTest", null)))

        tags.put("testng.test", values)
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(tags, mods, "com.atlassian.test", "TestMatching", null)))
    }

    void testSingleJavaDocMethod() {
        // <testsources>
        //     <testclass>
        //         <testmethod javadoc="testng\.test"/>
        //     </testclass>
        // </testsources>
        TestMethodSpec javaDocMethodSpec = new TestMethodSpec()
        javaDocMethodSpec.setTag("testng\\.test")
        TestClassSpec classSpec = new TestClassSpec()

        classSpec.addConfiguredTestMethod(javaDocMethodSpec)

        TestSourceSet testSources = new TestSourceSet()
        testSources.addConfiguredTestClass(classSpec)
        testSources.validate()

        Map tags = Maps.newHashMap()
        tags.put("thisShouldNotMatch", "value")
        MethodSignature noMatch = new MethodSignature(null, null, null, tags, null, "Test", null, "void", null, null)

        final TestDetector testDetector = testSources.getDetector()
        assertNotNull(testDetector)
        assertFalse(testDetector.isMethodMatch(null, JavaMethodContext.createFor(noMatch)))
        tags.put("testng.test", "value")
        MethodSignature match = new MethodSignature(null, null, null, tags, null, "Test", null, "void", null, null)
        assertTrue(testDetector.isMethodMatch(null, JavaMethodContext.createFor(match)))

    }

    void testMultipleTestSources() {
        // <testsources>
        //      <testclass name=".*Test"></testclass>
        //      <testclass package="com.atlassian.test.*"></testclass>
        // </testsources>
        TestSourceSet testSources = new TestSourceSet()
        testSources.addConfiguredTestClass(nameSetSpec)
        testSources.addConfiguredTestClass(pkgSetSpec)
        testSources.validate()
        assertOr(testSources)
    }

    void testOr() {
        // <testsources>
        //      <or>
        //          <testclass name=".*Test"></testclass>
        //          <testclass package="com.atlassian.test.*"></testclass>
        //      </or>
        // </testsources>
        TestSourceSet testSources = new TestSourceSet()

        OrSpec or = new OrSpec()
        or.addConfiguredTestClass(nameSetSpec)
        or.addConfiguredTestClass(pkgSetSpec)
        testSources.addConfiguredOr(or)
        testSources.validate()
        assertOr(testSources)
    }

    void testAnd() {
        // <testsources>
        //      <and>
        //          <testclass name=".*Test"></testclass>
        //          <testclass package="com.atlassian.test.*"></testclass>
        //      </and>
        // </testsources>
        TestSourceSet testSources = new TestSourceSet()

        AndSpec and = new AndSpec()
        and.addConfiguredTestClass(nameSetSpec)
        and.addConfiguredTestClass(pkgSetSpec)
        testSources.addConfiguredAnd(and)
        testSources.validate()
        assertAnd(testSources)
    }

    void testMultipleMethodSpecs() {
        // <testsources>
        //      <testclass name=".*Test">
        //          <testmethod name="^should.*"/>
        //          <testmethod name="^must.*"/>
        //          <testmethod annotation="Specification"/>
        //      </testclass>
        //      <testclass package="com.atlassian.test.*"></testclass>
        // </testsources>
        TestSourceSet testSources = new TestSourceSet()

        TestMethodSpec shouldMethod = new TestMethodSpec()
        TestMethodSpec mustMethod = new TestMethodSpec()
        TestMethodSpec specMethod = new TestMethodSpec()
        shouldMethod.setName("^should.*")
        mustMethod.setName("^must.*")
        specMethod.setAnnotation("Specification")
        
        nameSetSpec.addConfiguredTestMethod(shouldMethod)
        nameSetSpec.addConfiguredTestMethod(mustMethod)
        nameSetSpec.addConfiguredTestMethod(specMethod)
        testSources.addConfiguredTestClass(nameSetSpec)
        testSources.validate()

        final TestDetector testDetector = testSources.getDetector()
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.test", "RealTest", null)))
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.test", "XXX", null)))

        MethodSignature noMatch = new MethodSignature(null, null, null, "dontMatch", null, "void", null, null)
        MethodSignature should = new MethodSignature(null, null, null, "shouldMatch", null, "void", null, null)
        MethodSignature must = new MethodSignature(null, null, null, "mustMatch", null, "void", null, null)
        mods.addAnnotation(new AnnotationImpl("Specification"))
        MethodSignature specification = new MethodSignature(null, null, null, null, mods, "nonMatchingMethodName", null, "void", null, null)

        assertTrue(testDetector.isMethodMatch(null, JavaMethodContext.createFor(should)))
        assertTrue(testDetector.isMethodMatch(null, JavaMethodContext.createFor(must)))
        assertTrue(testDetector.isMethodMatch(null, JavaMethodContext.createFor(specification)))
        assertFalse(testDetector.isMethodMatch(null, JavaMethodContext.createFor(noMatch)))
    }
    

    private void assertOr(TestSourceSet testSources) {
        // As the default boolean strategy is OR, this will match any class:
        // in the com.atlassian.test.* package, OR any class ending in .*Test
        final TestDetector testDetector = testSources.getDetector()
        assertNotNull(testDetector)
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.test", "XXX", null)))
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.blah", "RealTest", null)))
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.blah", "XXX", null)))
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.test", "RealTest", null)))
    }

    private void assertAnd(TestSourceSet testSources) {
        // Match any class if it is:
        // in the com.atlassian.test.* package, AND any ends in .*Test
        final TestDetector testDetector = testSources.getDetector()
        assertNotNull(testDetector)
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.test", "XXX", null)))
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.blah", "RealTest", null)))
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.blah", "XXX", null)))
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "com.atlassian.test", "RealTest", null)))
    }

}
