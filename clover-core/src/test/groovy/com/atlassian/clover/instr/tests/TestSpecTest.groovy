package com.atlassian.clover.instr.tests

import clover.com.google.common.collect.Lists
import clover.com.google.common.collect.Maps
import com.atlassian.clover.instr.java.JavaMethodContext
import com.atlassian.clover.instr.java.JavaTypeContext
import com.atlassian.clover.registry.entities.AnnotationImpl
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import org.junit.Before
import org.junit.Test

import java.util.regex.Pattern

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class TestSpecTest {

    TestSpec spec
    Modifiers mods
    Map<String, List<String>> tags

    @Before
    void setUp() {
        spec = new TestSpec()
        mods = new Modifiers()
        tags = Maps.newHashMap()
    }

    @Test
    void testIsClassMatchOnPackage() throws Exception {
        spec.setPkgPattern(Pattern.compile("com.cenqua.testme.*"))
        assertFalse(spec.isTypeMatch(null, new JavaTypeContext(null, mods, "com.cenqua.test", "className", "superclass")))
        assertTrue(spec.isTypeMatch(null, new JavaTypeContext(null, mods, "com.cenqua.testme", "className", "superclass")))
    }

    @Test
    void testIsClassMatchOnTags() {

        spec.setClassTagPattern(Pattern.compile("javadoctest"))
        List<String> values = Lists.newArrayList()
        values.add("value")

        // test with null tags
        assertFalse(spec.isTypeMatch(null, new JavaTypeContext(null, null, null, null, null)))

        // empty tags
        assertFalse(spec.isTypeMatch(null, new JavaTypeContext(tags, null, null, null, null)))

        // unmatching tag
        tags.put("unmatching", values)
        assertFalse(spec.isTypeMatch(null, new JavaTypeContext(tags, null, null, null, null)))

        // unmatching and matching tag
        tags.put("javadoctest", values)
        assertTrue(spec.isTypeMatch(null, new JavaTypeContext(tags, null, null, null, null)))

    }

    @Test
    void testIsClassMatchOnAnnotation() {
        spec.setClassAnnotationPattern(Pattern.compile("annotation"))
        // test with null mods. Happens on line #83 of JavaRecognizer.java
        assertFalse(spec.isTypeMatch(null, new JavaTypeContext(null, null, "non.matching.pkg", "NonMatchingClassName", "NonMatchingSuperClass")))

        assertFalse(spec.isTypeMatch(null, new JavaTypeContext(null, mods, "non.matching.pkg", "NonMatchingClassName", "NonMatchingSuperClass")))
        mods.addAnnotation(new AnnotationImpl("annotation"))
        assertTrue(spec.isTypeMatch(null, new JavaTypeContext(null, mods, "non.matching.pkg", "NonMatchingClassName", "NonMatchingSuperClass")))
    }

    @Test
    void testIsClassMatchOnClassName() {
        spec.setClassPattern(Pattern.compile("ClassName"))
        assertFalse(spec.isTypeMatch(null, new JavaTypeContext(null, mods, "non.matching.pkg", "NonMatchingClassName", "NonMatchingSuperClass")))
        assertTrue(spec.isTypeMatch(null, new JavaTypeContext(null, mods, "non.matching.pkg", "ClassName", "NonMatchingSuperClass")))
    }

    @Test
    void testIsClassMatchOnSUPER() {
        spec.setSuperPattern(Pattern.compile("SuperClassName"))
        assertFalse(spec.isTypeMatch(null, new JavaTypeContext(null, mods, "non.matching.pkg", "NonMatchingClassName", "NonMatchingSuperClass")))
        assertTrue(spec.isTypeMatch(null, new JavaTypeContext(null, mods, "non.matching.pkg", "ClassName", "SuperClassName")))
    }

    @Test
    void testIsMethodMatchOnMethodSig() {
        spec.setMethodPattern(Pattern.compile(".*methodName.*"))
        MethodSignature sig = new MethodSignature(null, null, null, "nonMatchingMethod", null, "void", null, null)
        
        assertFalse(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))
        sig = new MethodSignature(null, null, null,"methodName", null, "void", null, null)
        assertTrue(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))
    }

    @Test
    void testIsMethodMatchOnAnnotation() {
        spec.setMethodAnnotationPattern(Pattern.compile("ThisIsATest"))
        mods.addAnnotation(new AnnotationImpl("ThisIsNotATest"))
        MethodSignature sig = new MethodSignature(null, null, null, null, mods, "nonMatchingMethod", null, "void", null, null)

        assertFalse(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))

        mods.addAnnotation(new AnnotationImpl("ThisIsATest"))
        assertTrue(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))
    }

    @Test
    void testIsMethodMatchOnTags() {
        spec.setMethodTagPattern(Pattern.compile("testng\\.test"))
        List<String> value = Lists.newArrayList()
        value.add("value")

        // test with null tags             //note: return type cannot be null
        MethodSignature sig = new MethodSignature(null, null, null, null, null, null,  null, "void", null, null)
        assertFalse(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))

        // empty tags
        sig = new MethodSignature(null, null, null, tags, null, null,  null, "void", null, null)
        assertFalse(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))

        // unmatching tag
        tags.put("unmatching", value)
        sig = new MethodSignature(null, null, null, tags, null, null, null, "void", null, null)
        assertFalse(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))

        // unmatching and matching tag
        tags.put("testng.test", value)
        sig = new MethodSignature(null, null, null, tags, null, null, null, "void", null, null)
        assertTrue(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))
    }

    @Test
    void testIsMethodMatchOnReturnType() {
        spec.setMethodReturnTypePattern(Pattern.compile("void"))
        MethodSignature sig = new MethodSignature(null, null, null, null, mods, "methodReturningAString", null, "String", null, null)
        assertFalse(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))
        sig = new MethodSignature(null, null, null, null, mods, "methodReturningVoid", null, "void", null, null);        
        assertTrue(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))

        mods.addAnnotation(new AnnotationImpl("TestAnnotation"))
        spec.setMethodAnnotationPattern(Pattern.compile("TestAnnotation"))
        assertTrue(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))

        spec.setMethodPattern(Pattern.compile("methodReturningVoid"))
        assertTrue(spec.isMethodMatch(null, JavaMethodContext.createFor(sig)))
    }

}
