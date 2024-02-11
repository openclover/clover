package org.openclover.core.instr.tests

import com.atlassian.clover.instr.java.JavaMethodContext
import com.atlassian.clover.instr.java.JavaTypeContext
import com.atlassian.clover.instr.tests.AggregateTestDetector
import com.atlassian.clover.instr.tests.AndStrategy
import com.atlassian.clover.instr.tests.OrStrategy
import com.atlassian.clover.instr.tests.TestSpec
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import org.junit.Before
import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class AggregateTestDetectorTest {

    TestSpec spec1
    TestSpec spec2
    Modifiers mods

    @Before
    void setUp() {
        mods = new Modifiers()
        spec1 = new TestSpec()
        spec1.setClassPattern(Pattern.compile("ClassName.*"))
        spec2 = new TestSpec()
        spec2.setClassPattern(Pattern.compile(".*Test"))
    }

    @Test
    void testIsEmpty() {
        AggregateTestDetector spec = new AggregateTestDetector(new AndStrategy())
        assertTrue(spec.isEmpty())
        spec.addDetector(new TestSpec())
        assertFalse(spec.isEmpty())
    }

    @Test
    void testAnd() {
        AggregateTestDetector andSpec = new AggregateTestDetector(new AndStrategy())
        assertFalse(andSpec.isTypeMatch(null, new JavaTypeContext(null, null, "com.cenqua.test", "ClassNameX", "superclass")))
        andSpec.addDetector(spec1)
        andSpec.addDetector(spec2)

        // andSpec should now only match ClassName.*Test
        assertFalse(andSpec.isTypeMatch(null, new JavaTypeContext(null, null, "com.cenqua.test", "ClassNameX", "superclass")))
        assertFalse(andSpec.isTypeMatch(null, new JavaTypeContext(null, null, "com.cenqua.test", "XTest", "superclass")))
        assertTrue(andSpec.isTypeMatch(null, new JavaTypeContext(null, null, "com.cenqua.test", "ClassNameXXTest", "superclass")))

        spec2.setMethodPattern(Pattern.compile("test.*"))
        spec1.setMethodReturnTypePattern(Pattern.compile("void"))
        MethodSignature nonMatch = new MethodSignature(null, null, null, null, mods,  "nonMatchingMethod", null, "void", null, null)
        assertFalse(andSpec.isMethodMatch(null, JavaMethodContext.createFor(nonMatch)))

        MethodSignature match = new MethodSignature(null, null, null, null, mods,  "testMatchingMethod", null, "void", null, null)
        assertTrue(andSpec.isMethodMatch(null, JavaMethodContext.createFor(match)))

    }

    @Test
    void testOr() {
        AggregateTestDetector orSpec = new AggregateTestDetector(new OrStrategy())
        assertFalse(orSpec.isTypeMatch(null, new JavaTypeContext(null, null, "com.cenqua.test", "ClassNameX", "superclass")))
        orSpec.addDetector(spec1)
        orSpec.addDetector(spec2)

        // andSpec should match anything begginning with ClassName or ending with Test
        assertTrue(orSpec.isTypeMatch(null, new JavaTypeContext(null, null, "com.cenqua.test", "ClassNameX", "superclass")))
        assertTrue(orSpec.isTypeMatch(null, new JavaTypeContext(null, null, "com.cenqua.test", "XTest", "superclass")))
        assertTrue(orSpec.isTypeMatch(null, new JavaTypeContext(null, null, "com.cenqua.test", "ClassNameXXTest", "superclass")))
        assertFalse(orSpec.isTypeMatch(null, new JavaTypeContext(null, null, "com.cenqua.test", "NoMatchClassName", "superclass")))
        assertFalse(orSpec.isTypeMatch(null, new JavaTypeContext(null, null, "com.cenqua.test", "TestNoMatch", "superclass")))

        spec2.setMethodPattern(Pattern.compile("test.*"))
        spec1.setMethodReturnTypePattern(Pattern.compile("void"))
        MethodSignature nonMatch = new MethodSignature(null, null, null, null, mods,  "nonMatchingMethod", null, "int", null, null)
        assertFalse(orSpec.isMethodMatch(null, JavaMethodContext.createFor(nonMatch)))

        MethodSignature match = new MethodSignature(null, null, null, null, mods,  "nonMatchingMethod", null, "void", null, null)
        assertTrue(orSpec.isMethodMatch(null, JavaMethodContext.createFor(match)))

        MethodSignature match2 = new MethodSignature(null, null, null, null, mods,  "testMatchingMethod", null, "void", null, null)
        assertTrue(orSpec.isMethodMatch(null, JavaMethodContext.createFor(match2)))
    }

}
