package com.atlassian.clover.instr.tests;

import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.registry.entities.Modifiers;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class TestSpec implements TestDetector {
    private Pattern pkgPattern;
    private Pattern classAnnotationPattern;
    private Pattern classPattern;
    private Pattern superPattern;
    private Pattern classTagPattern;
    private Pattern methodAnnotationPattern;
    private Pattern methodPattern;
    private Pattern methodReturnTypePattern;
    private Pattern methodTagPattern;

    public TestSpec() {
    }

    public TestSpec(TestSpec spec) {
        pkgPattern = spec.pkgPattern;

        classAnnotationPattern = spec.classAnnotationPattern;
        classPattern = spec.classPattern;
        superPattern = spec.superPattern;
        classTagPattern = spec.classTagPattern;

        methodAnnotationPattern = spec.methodAnnotationPattern;
        methodPattern = spec.methodPattern;
        methodReturnTypePattern = spec.methodReturnTypePattern;
        methodTagPattern = spec.methodTagPattern;


    }

    public void setPkgPattern(Pattern pkgPattern) {
        this.pkgPattern = pkgPattern;
    }

    public void setClassAnnotationPattern(Pattern classAnnotationPattern) {
        this.classAnnotationPattern = classAnnotationPattern;
    }

    public void setClassTagPattern(Pattern classTagPattern) {
        this.classTagPattern = classTagPattern;
    }

    private boolean tagsMatch(Pattern javaDocPattern, Map<String, List<String>> tags) {
        for (String s : tags.keySet()) {
            if (javaDocPattern.matcher(s).matches()) {
                return true;
            }
        }
        return false;
    }

    public void setMethodTagPattern(Pattern methodTagPattern) {
        this.methodTagPattern = methodTagPattern;
    }

    public void setClassPattern(Pattern classPattern) {
        this.classPattern = classPattern;
    }

    public void setSuperPattern(Pattern superPattern) {
        this.superPattern = superPattern;
    }

    public void setMethodAnnotationPattern(Pattern methodAnnotationPattern) {
        this.methodAnnotationPattern = methodAnnotationPattern;
    }

    public void setMethodPattern(Pattern methodPattern) {
        this.methodPattern = methodPattern;
    }

    public void setMethodReturnTypePattern(Pattern methodReturnsPattern) {
        this.methodReturnTypePattern = methodReturnsPattern;
    }

    public boolean methodReturnPatternMatches(String methodReturnType) {
        // ensure that methodReturnType != null; null means that we have a constructor and Clover cannot instrument
        // constructor as a test method (it rewrites original method body into a new private method, which causes
        // compilation failure in case of constructor's body)
        return methodReturnType != null &&
                (methodReturnTypePattern == null || methodReturnTypePattern.matcher(methodReturnType).matches());
    }

    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        return pkgMatches(typeContext.getPackageName()) &&
                classMatches(typeContext.getTypeName()) &&
                superMatches(typeContext.getSuperTypeName()) &&
                classAnnotationMatches(typeContext.getModifiers()) &&
                classTagMatches(typeContext.getDocTags());
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        final MethodSignature signature = methodContext.getSignature();
        return methodMatches(signature.getName()) &&
                methodAnnotationMatches(signature.getModifiers()) &&
                methodReturnPatternMatches(signature.getReturnType()) &&
                methodTagMatches(signature.getTags());
    }

    public boolean pkgMatches(String pkg) {
        return pkgPattern == null || (pkg != null && pkgPattern.matcher(pkg).matches());
    }

    public boolean classAnnotationMatches(Modifiers m) {
        return classAnnotationPattern == null || (m != null && m.containsAnnotation(classAnnotationPattern));
    }

    public boolean classTagMatches(Map<String, List<String>> tags) {
        return classTagPattern == null || (tags != null && !tags.isEmpty() && tagsMatch(classTagPattern, tags));
    }

    public boolean methodTagMatches(Map<String, List<String>> tags) {
        return methodTagPattern == null || (tags != null && !tags.isEmpty() && tagsMatch(methodTagPattern, tags));
    }

    public boolean classMatches(String className) {
        return classPattern == null || (className != null && classPattern.matcher(className).matches());
    }

    public boolean methodAnnotationMatches(Modifiers modifiers) {
        return methodAnnotationPattern == null || (modifiers != null && modifiers.containsAnnotation(methodAnnotationPattern));
    }

    public boolean superMatches(String superName) {
         return superPattern == null || (superName != null && superPattern.matcher(superName).matches());
    }

    public boolean methodMatches(String method) {
        return methodPattern == null || (method != null && methodPattern.matcher(method).matches());
    }
}
