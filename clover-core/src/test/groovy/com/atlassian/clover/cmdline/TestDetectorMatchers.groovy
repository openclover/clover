package com.atlassian.clover.cmdline

import com.atlassian.clover.instr.java.JavaMethodContext
import com.atlassian.clover.instr.java.JavaTypeContext
import com.atlassian.clover.instr.tests.TestDetector
import com.atlassian.clover.registry.entities.AnnotationImpl
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import com.atlassian.clover.spi.lang.Language
import org.hamcrest.Description
import org.hamcrest.Factory
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class TestDetectorMatchers {

    @Factory
    static Matcher<TestDetector> fileMatches(String fileName) {
        return new FileMatcher(fileName)
    }

    @Factory
    static Matcher<TestDetector> classMatches(String className, String packageName, String annotation, String superClass, String tag) {
        return new ClassMatcher(className, packageName, annotation, superClass, tag)
    }

    @Factory
    static Matcher<TestDetector> methodMatches(String methodName, String annotation, String returnType, String tag) {
        return new MethodMatcher(methodName, annotation, returnType, tag)
    }
}

class FileMatcher extends TypeSafeMatcher<TestDetector> {
    private String fileName

    FileMatcher(String fileName) {
        this.fileName = fileName
    }

    @Override
    protected boolean matchesSafely(TestDetector testDetector) {
        return testDetector.isTypeMatch(new SimpleFileSourceContext(fileName), new EmptyTypeContext())
    }

    @Override
    void describeTo(Description description) {
        description.appendText("match path ").appendValue(fileName)
    }
}

class ClassMatcher extends TypeSafeMatcher<TestDetector> {
    private String className, packageName, annotation, superClass, tag

    ClassMatcher(String className, String packageName, String annotation, String superClass, String tag) {
        this.className = className
        this.packageName = packageName
        this.annotation = annotation
        this.superClass = superClass
        this.tag = tag
    }

    @Override
    protected boolean matchesSafely(TestDetector testDetector) {
        Modifiers modifiers
        if (annotation == null) {
            modifiers = null
        } else {
            AnnotationImpl[] annotations = new AnnotationImpl[1]
            annotations[0] = new AnnotationImpl(annotation)
            modifiers = Modifiers.createFrom(0, annotations)
        }
        Map<String, String> tags
        if (tag == null) {
            tags = null
        } else {
            tags = new HashMap<String, String>()
            tags.put(tag, "")
        }

        return testDetector.isTypeMatch(new SimpleFileSourceContext("foo"),
                new JavaTypeContext(
                        tags,
                        modifiers,
                        packageName,
                        className,
                        superClass))
    }

    @Override
    void describeTo(Description description) {
        description.appendText("match the class signature <")
        if (packageName != null) {
            description.appendText(String.format("package %s; ", packageName))
        }
        if (tag != null) {
            description.appendText(String.format("/** @%s */ ", tag))
        }
        if (annotation != null) {
            description.appendText(String.format("@%s ", annotation))
        }
        if (className != null) {
            description.appendText(String.format("class %s", className))
        }
        if (superClass != null) {
            description.appendText(String.format(" extends %s", superClass))
        }
        description.appendText(">")
    }
}

class MethodMatcher extends TypeSafeMatcher<TestDetector> {
    private String methodName, annotation, returnType, tag

    MethodMatcher(String methodName, String annotation, String returnType, String tag) {
        this.methodName = methodName
        this.annotation = annotation
        this.returnType = returnType
        this.tag = tag
    }

    @Override
    protected boolean matchesSafely(TestDetector testDetector) {
        Map<String, List<String>> tags = null
        if (tag != null) {
            tags = new HashMap<String, List<String>>()
            tags.put(tag, Collections.emptyList())
        }

        Modifiers modifiers = null
        if (annotation != null) {
            modifiers = Modifiers.createFrom(0, new AnnotationImpl(annotation))
        }

        MethodSignature signature = new MethodSignature(null, null, null, tags, modifiers,
                methodName, null, returnType, null, null)

        return testDetector.isMethodMatch(
                new SimpleFileSourceContext("foo"),
                JavaMethodContext.createFor(signature))
    }

    @Override
    void describeTo(Description description) {
        description.appendText("match the method signature <")
        if (tag != null) {
            description.appendText(String.format("/** @%s */ ", tag))
        }
        if (annotation != null) {
            description.appendText(String.format("@%s ", annotation))
        }
        if (returnType != null) {
            description.appendText(String.format("%s ", returnType))
        }
        if (methodName != null) {
            description.appendText(methodName)
        }
        description.appendText(">")
    }
}

class SimpleFileSourceContext implements TestDetector.SourceContext {

    String fileName

    SimpleFileSourceContext(String fileName) {
        this.fileName = fileName
    }

    @Override
    Language getLanguage() {
        return Language.Builtin.JAVA
    }

    @Override
    boolean areAnnotationsSupported() {
        return false
    }

    @Override
    File getSourceFile() {
        return new File(fileName);
    }
}

class EmptyTypeContext extends JavaTypeContext {

    EmptyTypeContext() {
        super(null, null, null, null, null)
    }
}