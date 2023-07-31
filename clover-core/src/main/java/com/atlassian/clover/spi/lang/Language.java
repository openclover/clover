package com.atlassian.clover.spi.lang;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import static org.openclover.util.Sets.newHashSet;

/**
 * A language that Clover may support.
 */
public interface Language {
    /**
     * @return the language's name
     */
    String getName();

    /**
     * @return the file extensions supported by the language (e.g. ".java")
     */
    Set<String> getFileExtensions();

    /**
     * @return the language constructs supported by the language
     */
    Set<LanguageConstruct> getSupportedConstructs();

    /**
     * Languages supported by Clover out-of-the-box.
     */
    enum Builtin implements Language {
        JAVA(".java", LanguageConstruct.Builtin.METHOD, LanguageConstruct.Builtin.BRANCH, LanguageConstruct.Builtin.STATEMENT),
        GROOVY(".groovy", LanguageConstruct.Builtin.METHOD, LanguageConstruct.Builtin.BRANCH, LanguageConstruct.Builtin.STATEMENT, LanguageConstruct.Builtin.GROOVY_FIELD_EXPRESSION, LanguageConstruct.Builtin.GROOVY_SAFE_METHOD, LanguageConstruct.Builtin.GROOVY_SAFE_ATTRIBUTE, LanguageConstruct.Builtin.GROOVY_SAFE_PROPERTY);

        private final Set<String> extensions;
        private final Set<LanguageConstruct> constructs;

        Builtin(String extension, LanguageConstruct... constructs) {
            this.extensions = Collections.singleton(extension);
            this.constructs = Collections.unmodifiableSet(newHashSet(constructs));
        }

        @Override
        public String getName() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        @Override
        public Set<String> getFileExtensions() {
            return extensions;
        }

        @Override
        public Set<LanguageConstruct> getSupportedConstructs() {
            return constructs;
        }
    }
}
