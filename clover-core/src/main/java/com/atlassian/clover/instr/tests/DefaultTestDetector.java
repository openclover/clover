package com.atlassian.clover.instr.tests;

import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.registry.entities.Modifiers;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
  * default test detector. Will detect test classes &amp; methods for junit3.8, junit 4.x, junit5.x, TestNG 4, Instinct 0.1.6 and Spring 3 Annotations
 */
public class DefaultTestDetector implements TestDetector {
    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        // check annotations first
        if ( (sourceContext.areAnnotationsSupported()
                && typeContext.getModifiers() != null
                && (   typeContext.getModifiers().containsAnnotation(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME)
                    || typeContext.getModifiers().containsAnnotation(TestAnnotationNames.TEST_ANNO_NAME)
                    || typeContext.getModifiers().containsAnnotation(TestAnnotationNames.SPOCK_CLASS_FQ_ANNO_NAME)
                    || typeContext.getModifiers().containsAnnotation(TestAnnotationNames.SPOCK_CLASS_ANNO_NAME)
                )
             )
            || (typeContext.getDocTags() != null && typeContext.getDocTags().containsKey("testng.test"))
           ) {
            return true;
        }
        // and next fallback to class names
        return ((strContains(typeContext.getTypeName(), "test")
                 || strEquals(typeContext.getSuperTypeName(), "TestCase")
                 || strEquals(typeContext.getSuperTypeName(), "junit.framework.TestCase"))
                && (sourceContext.areAnnotationsSupported() || strContains(typeContext.getSuperTypeName(), "test")));
    }

    private boolean strContains(String target, String str) {
        return (target != null && target.toLowerCase().contains(str));
    }

    private boolean strEquals(String target, String str) {
        return target != null && target.equals(str);
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        final MethodSignature signature = methodContext.getSignature();
        if (methodContext != null
                //Concrete methods
                && !Modifier.isAbstract(signature.getModifiersMask())
                //TestNG/JUnit5 do not require public methods
                && !Modifier.isPrivate(signature.getModifiersMask())
                // no ctors
                && signature.getReturnType() != null) {

            // annotations or tags trump other heuristics
            if ( (sourceContext.areAnnotationsSupported() && hasTestAnnotations(signature.getModifiers()))
                    || hasTestTags(signature.getTags()) ) {
                 return true;
            }

            // junit 3.x -textXXX methods
            if (Modifier.isPublic(signature.getModifiersMask()) && signature.getName().startsWith("test") && !signature.hasParams()) {
                return true;
            }
        }

        return false;
    }

    protected boolean hasTestAnnotations(Modifiers modifiers) {
        // return true if method has any of JUnit4, JUnit5, TestNG, Spring, Spock annotations
        return ( modifiers.containsAnnotation(TestAnnotationNames.JUNIT_TEST_ANNO_NAME)
                || modifiers.containsAnnotation(TestAnnotationNames.JUNIT5_TEST_ANNO_NAME)
                || modifiers.containsAnnotation(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME)
                || modifiers.containsAnnotation(TestAnnotationNames.TEST_ANNO_NAME)
                || modifiers.containsAnnotation(TestAnnotationNames.SPOCK_METHOD_FQ_ANNO_NAME)
                || modifiers.containsAnnotation(TestAnnotationNames.SPOCK_METHOD_ANNO_NAME) )
            // but it's not marked as ignored
            && !( modifiers.containsAnnotation(TestAnnotationNames.JUNIT_IGNORE_ANNO_NAME)
                || modifiers.containsAnnotation(TestAnnotationNames.JUNIT5_IGNORE_ANNO_NAME)
                || modifiers.containsAnnotation(TestAnnotationNames.IGNORE_ANNO_NAME)
                || modifiers.containsAnnotation(TestAnnotationNames.DISABLED_ANNO_NAME) );
    }

    protected boolean hasTestTags(Map<String, List<String>> tags) {
        return tags.containsKey("test") || tags.containsKey("testng.test");
    }


}
