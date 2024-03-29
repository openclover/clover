package org.openclover.ant.tasks

import junit.framework.TestCase
import org.openclover.core.instr.java.JavaMethodContext
import org.openclover.core.instr.java.JavaTypeContext
import org.openclover.core.instr.tests.DefaultTestDetector
import org.openclover.core.instr.tests.NoTestDetector
import org.openclover.core.instr.tests.TestDetector
import org.openclover.core.registry.entities.AnnotationImpl
import org.openclover.core.registry.entities.MethodSignature
import org.openclover.core.registry.entities.Modifiers
import org.openclover.core.spec.instr.test.AndSpec
import org.openclover.core.spec.instr.test.OrSpec
import org.openclover.core.spec.instr.test.TestClassSpec
import org.openclover.core.spec.instr.test.TestMethodSpec

import static org.openclover.core.util.Lists.newArrayList
import static org.openclover.core.util.Maps.newHashMap

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
        pkgSetSpec.setPackage("org.openclover.test.*")
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
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.test", "MatchingTest", null)))
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.test", "TestMatching", null)))
    }

    void testSingleJavaDocClass() {
        // <testsources>
        //     <testclass javadoc="testng\.testng"/>
        // <testsources>        
        TestSourceSet testSources = new TestSourceSet()
        testSources.addConfiguredTestClass(javaDocSpec)
        List<String> values = newArrayList()
        values.add("value")

        testSources.validate()

        Map<String, List<String>> tags = newHashMap()
        tags.put("thisShouldNotMatch", values)

        final TestDetector testDetector = testSources.getDetector()
        assertNotNull(testDetector)
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(tags, mods, "org.openclover.test", "MatchingTest", null)))

        tags.put("testng.test", values)
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(tags, mods, "org.openclover.test", "TestMatching", null)))
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

        Map tags = newHashMap()
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
        //      <testclass package="org.openclover.test.*"></testclass>
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
        //          <testclass package="org.openclover.test.*"></testclass>
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
        //          <testclass package="org.openclover.test.*"></testclass>
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
        //      <testclass package="org.openclover.test.*"></testclass>
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
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.test", "RealTest", null)))
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.test", "XXX", null)))

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
        // in the org.openclover.test.* package, OR any class ending in .*Test
        final TestDetector testDetector = testSources.getDetector()
        assertNotNull(testDetector)
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.test", "XXX", null)))
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.blah", "RealTest", null)))
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.blah", "XXX", null)))
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.test", "RealTest", null)))
    }

    private void assertAnd(TestSourceSet testSources) {
        // Match any class if it is:
        // in the org.openclover.test.* package, AND any ends in .*Test
        final TestDetector testDetector = testSources.getDetector()
        assertNotNull(testDetector)
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.test", "XXX", null)))
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.blah", "RealTest", null)))
        assertFalse(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.blah", "XXX", null)))
        assertTrue(testDetector.isTypeMatch(null, new JavaTypeContext(null, mods, "org.openclover.test", "RealTest", null)))
    }

}
