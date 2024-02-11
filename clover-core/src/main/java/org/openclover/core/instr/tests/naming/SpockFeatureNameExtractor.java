package org.openclover.core.instr.tests.naming;

import org.openclover.core.api.registry.Annotation;
import org.openclover.core.api.registry.AnnotationValue;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.instr.tests.TestAnnotationNames;
import org.openclover.core.registry.entities.Modifiers;
import org.openclover.core.registry.entities.StringifiedAnnotationValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Test name extractor for Spock features
 */
public class SpockFeatureNameExtractor implements TestNameExtractor {
    /** Name of the attribute in the {@link TestAnnotationNames#SPOCK_METHOD_FQ_ANNO_NAME} holding static name of the test */
    public static final String NAME_ATTR = "name";

    @Override
    @Nullable
    public String getTestNameForMethod(@NotNull MethodInfo methodInfo) {
        // try to get spock annotation for test method using fully qualified or short name
        Collection<Annotation> featureMetadataAnnotations =
                methodInfo.getSignature().getModifiers().getAnnotation(TestAnnotationNames.SPOCK_METHOD_FQ_ANNO_NAME);
        if (featureMetadataAnnotations.isEmpty()) {
            featureMetadataAnnotations =
                    methodInfo.getSignature().getModifiers().getAnnotation(TestAnnotationNames.SPOCK_METHOD_ANNO_NAME);
        }

        if (!featureMetadataAnnotations.isEmpty()) {
            // grab first annotation, we assume that repeated annotations (java8) are not used for spock
            Annotation featureMetadata = featureMetadataAnnotations.iterator().next();
            AnnotationValue attrValue = featureMetadata.getAttribute(NAME_ATTR);
            if (attrValue != null && attrValue instanceof StringifiedAnnotationValue) {
                return ((StringifiedAnnotationValue) attrValue).getValue();
            }
        }

        // spock stuff not found for this method
        return null;
    }

    public static boolean isClassWithSpecAnnotations(Modifiers modifiers) {
        return modifiers.containsAnnotation(TestAnnotationNames.SPOCK_CLASS_FQ_ANNO_NAME)
                || modifiers.containsAnnotation(TestAnnotationNames.SPOCK_CLASS_ANNO_NAME);
    }
}
