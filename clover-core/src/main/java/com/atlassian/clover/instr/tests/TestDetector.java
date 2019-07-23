package com.atlassian.clover.instr.tests;

import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.registry.entities.Modifiers;
import com.atlassian.clover.spi.lang.Language;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;


public interface TestDetector extends Serializable {
    boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext);

    boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext);

    interface SourceContext {
        Language getLanguage();

        boolean areAnnotationsSupported();

        File getSourceFile();
    }

    interface TypeContext {
        String getPackageName();

        String getTypeName();

        String getSuperTypeName();

        Map<String, List<String>> getDocTags();

        Modifiers getModifiers();
    }

    interface MethodContext {
        MethodSignature getSignature();
    }
}
