package com.atlassian.clover.instr.tests;

import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.registry.entities.Modifiers;
import com.atlassian.clover.spi.lang.Language;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.io.Serializable;


public interface TestDetector extends Serializable {
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext);
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext);

    public interface SourceContext {
        public Language getLanguage();
        public boolean areAnnotationsSupported();
        public File getSourceFile();
    }

    public interface TypeContext {
        public String getPackageName();
        public String getTypeName();
        public String getSuperTypeName();
        public Map<String, List<String>> getDocTags();
        public Modifiers getModifiers();
    }

    public interface MethodContext {
        public MethodSignature getSignature();
    }
}
