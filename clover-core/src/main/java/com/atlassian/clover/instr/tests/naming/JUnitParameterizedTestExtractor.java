package com.atlassian.clover.instr.tests.naming;

import com.atlassian.clover.api.registry.Annotation;
import com.atlassian.clover.api.registry.AnnotationValue;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.ModifiersInfo;
import com.atlassian.clover.instr.tests.TestAnnotationNames;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.registry.entities.Modifier;
import com.atlassian.clover.registry.entities.StringifiedAnnotationValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * <p>***For Junit 4 Parameterized Tests***</p>
 *
 * <p>Returns a test name template of the test from a
 * {@code @Parameters(name = "{a} is equal {b}")} annotation.</p>
 *
 * <p>Requirements:</p>
 * <ul>
 * <li>class is annotated with "{@code @RunWith(org.junit.runners.Parameterized.class)}"</li>
 * <li>class contains a "{@code public static Collection<Object[]> data()}" annotated
 * with "@Parameters(name = "{index}: * sort[{0}]={1}")"</li>
 * </ul>
 *
 * <br/>
 * <p>***For Junit 5 Parameterized Tests***</p>
 * <br/>
 * <p>Returns test's display name of the test from a {@code @ParameterizedTest} annotation.</p>
 * <p>Requirements:</p>
 * <ul>
 * <li>The test method is annotated with "{@code @ParameterizedTest} annotation."</li>
 * <li>The test method  must specify at least one {@code @ArgumentsProvider}
 * via {@code @ArgumentsSource} or a corresponding composed annotation"</li>
 * </ul>
 */
public class JUnitParameterizedTestExtractor implements TestNameExtractor {

    private static final String FQN_PARAMETERIZED_CLASS = "org.junit.runners.Parameterized.class";
    private static final String PARAMETERIZED_CLASS = "Parameterized.class";
    private static final String RUN_WITH = "RunWith";
	//JUnit 5 Parameterized Tests
    private static final String FQN_JUPITER_PARAMETERIZED_CLASS = "org.junit.jupiter.params.ParameterizedTest.class";
    private static final String JUPITER_PARAMETERIZED_TEST = "ParameterizedTest";

    @Override
    @Nullable
    public String getTestNameForMethod(@NotNull MethodInfo methodInfo) {
        ClassInfo thisClass = methodInfo.getContainingClass();
        if (thisClass != null && isParameterizedClass(thisClass.getModifiers())) {
            MethodInfo dataMethod = findDataMethod(thisClass);
            if (dataMethod != null) {
                return getParametersNameValue(dataMethod);
            }
        } else if (thisClass != null) {
            return getJunit5ParamTestMethodName(methodInfo);
        }

        return null;
    }

    public static boolean isParameterizedClass(ModifiersInfo modifiers) {
        for (Annotation annotation : modifiers.getAnnotation(RUN_WITH)) {
            String className = getDefaultValue(annotation);
            if (className != null && (FQN_PARAMETERIZED_CLASS.equals(className)
                    || PARAMETERIZED_CLASS.equals(className)) ) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static MethodInfo findDataMethod(ClassInfo classInfo) {
        for (MethodInfo methodInfo : classInfo.getMethods()) {
            int mask = methodInfo.getSignature().getModifiers().getMask();
            if (methodInfo.getSimpleName().equals("data") && ((mask & Modifier.PUBLIC) != 0)
                    && ((mask & Modifier.STATIC) != 0)) {
                // seems to be this method, return it
                return methodInfo;
            }
        }

        return null;
    }

    @Nullable
    private static String getDefaultValue(Annotation annotation) {
        AnnotationValue defaultValue = annotation.getAttribute("value");
        if (defaultValue instanceof StringifiedAnnotationValue) {
            return ((StringifiedAnnotationValue)defaultValue).getValue();
        }
        return null;
    }

    @Nullable
    private static String getParametersNameValue(MethodInfo dataMethod) {
        Collection<Annotation> parametersAnno = dataMethod.getSignature().getModifiers().getAnnotation("Parameters");
        if (!parametersAnno.isEmpty()) {
            Annotation parameters = parametersAnno.iterator().next(); // grab first one
            AnnotationValue nameValue = parameters.getAttribute("name");
            if (nameValue instanceof StringifiedAnnotationValue) {
                return ((StringifiedAnnotationValue)nameValue).getValue();
            }
        }
        return null;
    }

    /**
     * Checks if the given {@code methodSignature} parameter is annotated with {@code @ParameterizedTest} annotation.
     *
     * @param methodSignature Method to check for {@code @ParameterizedTest} annotation
     * @return {@code true} if the given method is annoted with {@code @ParameterizedTest} annotation else {@code false}
     */
    public static boolean isJnit5ParameterizedTest(@NotNull MethodSignature methodSignature) {
        Map<String, Collection<Annotation>> annotationsMap = methodSignature.getAnnotations();
        return (annotationsMap.containsKey(TestAnnotationNames.JUNIT5_PARAMETERIZED_ANNO_NAME)
                || annotationsMap.containsKey(TestAnnotationNames.JUNIT5_FQ_PARAMETERIZED_ANNO_NAME));
    }

    /**
     * Returns ParameterizedTest method name by reading name value of ParaeterizedTest annotion.
     *
     * @param methodInfo The method to check for Junit5 parameter display name, can't be null.
     * @return the parameterized test display name if the given method is annotated with {@code @ParameterizedTest} else
     * {@code null}.
     */
    @Nullable
    private static String getJunit5ParamTestMethodName(@NotNull MethodInfo methodInfo) {
        Collection<Annotation> jupiterParameterizedAnnos = methodInfo.getSignature().getModifiers().getAnnotation(JUPITER_PARAMETERIZED_TEST);
        if (jupiterParameterizedAnnos.size() > 0) {
            Annotation parameters = jupiterParameterizedAnnos.iterator().next(); // grab first one
            AnnotationValue nameValue = parameters.getAttribute("name");
            if (nameValue instanceof StringifiedAnnotationValue) {
                return ((StringifiedAnnotationValue) nameValue).getValue();
            }
        }
        return null;
    }	
	
}
