package org.openclover.core.instr.tests;

import org.openclover.core.api.registry.Annotation;
import org.openclover.core.api.registry.AnnotationValue;
import org.openclover.core.registry.entities.AnnotationImpl;
import org.openclover.core.registry.entities.MethodSignature;
import org.openclover.core.registry.entities.StringifiedAnnotationValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openclover.core.util.Sets.newTreeSet;

public class ExpectedExceptionMiner {
    /** Regex to match xdoclet attributes in format 'name = "value"' */
    private static final Pattern XDOCLET_NVP_PATTERN =
        Pattern.compile(
            "\\s*" + //leading whitespace
            "(\\S+)" + //name - capture group 1 holds the name
            "\\s*=\\s*"+ // =
            "\"([^\"]*)\""); // "value" - capture group 2 holds the unquoted text
    private static final Pattern WS_SPLIT_PATTERN =
        Pattern.compile("\\s+");
    private static final Pattern DOT_SPLIT_PATTERN =
        Pattern.compile("\\.");

    public static String[] extractExpectedExceptionsFor(MethodSignature sig, boolean checkTags) {
        final SortedSet<String> exceptionNames = newTreeSet();

        expectedExceptionsFromAnnotations(sig, exceptionNames);

        if (exceptionNames.size() == 0 && checkTags) {
            expectedExceptionsFromJavadoc(sig, exceptionNames);
        }

        return exceptionNames.toArray(new String[0]);
    }

    private static void expectedExceptionsFromJavadoc(MethodSignature sig, SortedSet<String> exceptionNames) {
        final Map<String, List<String>> tags = sig.getTags();
        if (tags != null && tags.size() > 0) {
            extractExpectedExceptions("testng.test", "expectedExceptions", exceptionNames, tags);
            extractExpectedExceptions("testng.expected-exceptions", "value", exceptionNames, tags);
        }
    }

    private static void extractExpectedExceptions(String tagName, String attributeName,
                                                  SortedSet<String> exceptionNames,
                                                  Map<String, List<String>> tags) {
        final List<String> testTagValues = tags.get(tagName);
        if (testTagValues != null) {
            for (String testTagValue : testTagValues) {
                processTagValue(attributeName, exceptionNames, testTagValue);
            }
        }
    }

    private static void processTagValue(String attributeName, SortedSet<String> exceptionNames, String testTagValue) {
        final Matcher testTagValueMatcher = XDOCLET_NVP_PATTERN.matcher(testTagValue);
        if (testTagValueMatcher.find()) {
            String attrName = testTagValueMatcher.group(1);
            String attrValue = testTagValueMatcher.group(2);
            if (attributeName.equals(attrName)) {
                processExceptionNames(exceptionNames, attrValue);
            }
        }
    }

    private static void processExceptionNames(SortedSet<String> exceptionNames, String attrValue) {
        final String[] classNames = WS_SPLIT_PATTERN.split(attrValue);
        for (String className : classNames) {
            if (looksLikeFQClassName(className)) {
                exceptionNames.add(className);
            }
        }
    }

    private static boolean looksLikeFQClassName(final String className) {
        if (className == null) {
            return false;
        }

        final String[] parts = DOT_SPLIT_PATTERN.split(className);
        for (String part : parts) {
            if (!isIdent(part)) {
                return false;
            }
        }

        return true;
    }

    private static void expectedExceptionsFromAnnotations(MethodSignature sig, SortedSet<String> exceptionNames) {
        AnnotationValue expectedAttrValue = null;

        // First try @Test
        final Collection<Annotation> testAnnotations = findTestAnnotation(sig);
        if (testAnnotations != null) {
            expectedAttrValue = extractExpectedAttrValue(sig, testAnnotations);
        }

        // Now try TestNG's @ExpectedExceptions
        if (expectedAttrValue == null) {
            final Collection<Annotation> expectedExceptionsAnnotation = findTestNGExpectedExceptionsAnnotation(sig);
            if (expectedExceptionsAnnotation != null) {
                expectedAttrValue = extractValueAttrValue(sig, expectedExceptionsAnnotation);
            }
        }

        // Now try Spring's @ExpectedException
        if (expectedAttrValue == null) {
            final Collection<Annotation> expectedExceptionsAnnotation = findSpringExpectedExceptionsAnnotation(sig);
            if (expectedExceptionsAnnotation != null) {
                expectedAttrValue = extractValueAttrValue(sig, expectedExceptionsAnnotation);
            }
        }

        // Finally try Instinct's expectedException
        if (expectedAttrValue == null) {
            final Collection<Annotation> specificationAnnotation = findSpecificationAnnotation(sig);
            if (specificationAnnotation != null) {
                expectedAttrValue = extractExpectedAttrValue(sig, specificationAnnotation);
            }
        }

        if (expectedAttrValue != null) {
            extractExpectedExceptions(exceptionNames, expectedAttrValue);
        }
    }

    /**
     * Searches for annotations corresponding to TestNG's @org.testng.annotations.Test or
     * JUnit's @org.junit.Test or any other annotation named @Test
     * @return Collection&lt;AnnotationImpl&gt;
     */
    private static Collection<Annotation> findTestAnnotation(MethodSignature sig) {
        Collection<Annotation> testAnnotation;
        testAnnotation = sig.getModifiers().getAnnotation(TestAnnotationNames.JUNIT_TEST_ANNO_NAME);
        testAnnotation = !testAnnotation.isEmpty() ? testAnnotation : sig.getModifiers().getAnnotation(TestAnnotationNames.TESTNG_FQ_TEST_ANNO_NAME);
        testAnnotation = !testAnnotation.isEmpty() ? testAnnotation : sig.getModifiers().getAnnotation(TestAnnotationNames.TEST_ANNO_NAME);
        return testAnnotation;
    }

    /** @return the annotation corresponding to TestNG's @ExpectedExceptions({Foo.class}) */
    private static Collection<Annotation> findTestNGExpectedExceptionsAnnotation(MethodSignature sig) {
        return findAnnotationValue(sig, TestAnnotationNames.TESTNG_EXPECTED_ANNO_NAME, TestAnnotationNames.TESTNG_FQ_EXPECTED_ANNO_NAME);
    }

    /** @return the annotation corresponding to Spring's @ExpectedExceptions({Foo.class}) */
    private static Collection<Annotation> findSpringExpectedExceptionsAnnotation(MethodSignature sig) {
        return findAnnotationValue(sig, TestAnnotationNames.SPRING_EXPECTED_ANNO_NAME, TestAnnotationNames.SPRING_FQ_EXPECTED_ANNO_NAME);
    }

    private static Collection<Annotation> findAnnotationValue(MethodSignature sig, String expectedAnnoName, String fqExpectedAnnoName) {
        final Collection<Annotation> expectedAnnotation = sig.getModifiers().getAnnotation(expectedAnnoName);
        return !expectedAnnotation.isEmpty() ? expectedAnnotation : sig.getModifiers().getAnnotation(fqExpectedAnnoName);
    }

    /** @return the annotation corresponding to Instinct's @Specification(expectedException=...) */
    private static Collection<Annotation> findSpecificationAnnotation(MethodSignature sig) {
        return findAnnotationValue(sig, TestAnnotationNames.INSTINCT_SPECIFICATION_ANNO_NAME, TestAnnotationNames.SPECIFICATION_ANNO_NAME);
    }

    private static AnnotationValue extractExpectedAttrValue(MethodSignature sig, Collection<Annotation> annotationCollection) {
        AnnotationValue value;
        for (Annotation annotation : annotationCollection) {
            if ((value = extractExpectedAttrValue(sig, annotation)) != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * @return @org.testng.annotations.Test.expectedExceptions or @org.junit.Test.expected values, if present, null otherwise
     */
    private static AnnotationValue extractExpectedAttrValue(MethodSignature sig, Annotation testAnnotation) {
        //TestNG: @Test(expectedExceptions={Foo.class})
        AnnotationValue value = testAnnotation.getAttribute(TestAnnotationNames.EXPECTED_EXCEPTIONS_ATTR_NAME);
        //JUnit: @Test(expected=Foo.class)
        value = value != null ? value : testAnnotation.getAttribute(TestAnnotationNames.EXPECTED_ATTR_NAME);
        //Instinct: @Specification(expectedException=Foo.class)
        return value != null ? value : testAnnotation.getAttribute(TestAnnotationNames.EXPECTED_EXCEPTION_ATTR_NAME);
    }

    private static AnnotationValue extractValueAttrValue(MethodSignature sig, Collection<Annotation> annotationCollection) {
        AnnotationValue value;
        for (final Annotation annotation : annotationCollection) {
            if ((value = annotation.getAttribute(TestAnnotationNames.VALUE_ATTR_NAME)) != null) {
                return value;
            }
        }
        return null;
    }

    private static AnnotationValue extractValueAttrValue(MethodSignature sig, AnnotationImpl expectedExceptionsAnnotation) {
        return expectedExceptionsAnnotation.getAttribute(TestAnnotationNames.VALUE_ATTR_NAME);
    }

    private static void extractExpectedExceptions(SortedSet<String> exceptionNames, AnnotationValue expectedAttrValue) {
        final List<? extends AnnotationValue> exceptionsNameValues = expectedAttrValue.toList();

        for (AnnotationValue exceptionName : exceptionsNameValues) {
            //At this point, ignore anything that's not an stringified annotation as it doesn't match our criteria
            if (exceptionName instanceof StringifiedAnnotationValue) {
                StringifiedAnnotationValue classNameValue = (StringifiedAnnotationValue) exceptionName;
                //It's possible that there may be a constant expression here and we won't
                //try to parse it. e.g. DEBUG==true?FooException.class:BarException.class
                //because we're likely to satisfy 99.99% of out punters with the most simple approach
                String className = stripClassNameFromDotClassExpression(classNameValue);
                if (className != null) {
                    exceptionNames.add(className);
                }
            }
        }
    }

    private static String stripClassNameFromDotClassExpression(final StringifiedAnnotationValue classNameValue) {
        final String[] parts = DOT_SPLIT_PATTERN.split(classNameValue.getValue());
        for (String part : parts) {
            if (!isIdent(part)) {
                return null;
            }
        }

        return
            classNameValue.getValue().endsWith(".class")
                ? classNameValue.getValue().substring(0, classNameValue.getValue().lastIndexOf(".class"))
                : classNameValue.getValue();
    }

    private static boolean isIdent(String ident) {
        if (ident == null
            || ident.length() == 0
            || !Character.isJavaIdentifierStart(ident.charAt(0))) {
            return false;
        }

        for (int i = 1; i < ident.length(); i++) {
            if (!Character.isJavaIdentifierPart(ident.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
