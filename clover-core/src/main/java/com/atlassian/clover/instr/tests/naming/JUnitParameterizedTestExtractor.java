package com.atlassian.clover.instr.tests.naming;

import com.atlassian.clover.api.registry.Annotation;
import com.atlassian.clover.api.registry.AnnotationValue;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.ModifiersInfo;
import com.atlassian.clover.registry.entities.Modifier;
import com.atlassian.clover.registry.entities.StringifiedAnnotationValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Returns a test name template of the test from a @Parameters(name = "{a} is equal {b}") annotation
 * Requirements:
 *  - class is annotated with "@RunWith(org.junit.runners.Parameterized.class)"
 *  - class contains a "public static Collection<Object[]> data()" annotated with
 *    "@Parameters(name = "{index}: sort[{0}]={1}")"
 */
public class JUnitParameterizedTestExtractor implements TestNameExtractor {

    private static final String FQN_PARAMETERIZED_CLASS = "org.junit.runners.Parameterized.class";
    private static final String PARAMETERIZED_CLASS = "Parameterized.class";
    private static final String RUN_WITH = "RunWith";

    @Override
    @Nullable
    public String getTestNameForMethod(@NotNull MethodInfo methodInfo) {
        ClassInfo thisClass = methodInfo.getContainingClass();
        if (thisClass != null && isParameterizedClass(thisClass.getModifiers())) {
            MethodInfo dataMethod = findDataMethod(thisClass);
            if (dataMethod != null) {
                return getParametersNameValue(dataMethod);
            }
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
}
