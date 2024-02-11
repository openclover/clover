package org.openclover.core.lang;

import org.openclover.core.services.ServiceLocator;
import org.openclover.core.services.ServiceNotAvailableException;
import org.openclover.core.spi.lang.Language;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.openclover.runtime.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Sets.newHashSet;

public class Languages {
    private static final Map<String, LanguageConstruct> LANGUAGE_CONSTRUCTS = newHashMap();
    private static final Set<Language> LANGUAGES = newHashSet();

    static {
        Collections.addAll(LANGUAGES, Language.Builtin.values());

        for (LanguageConstruct.Builtin construct : LanguageConstruct.Builtin.values()) {
            LANGUAGE_CONSTRUCTS.put(construct.getId(), construct);
        }

        for (Language language : ServiceLocator.load(Language.class, Languages.class.getClassLoader())) {
            try {
                LANGUAGES.add(language);

                for (LanguageConstruct construct : language.getSupportedConstructs()) {
                    LANGUAGE_CONSTRUCTS.put(construct.getId(), construct);
                }
            } catch (ServiceNotAvailableException e) {
                Logger.getInstance().warn("Failed to register a 3rd party language support", e);
            }
        }
    }

    public static Set<Language> getLanguages() {
        return Collections.unmodifiableSet(LANGUAGES);
    }

    public static LanguageConstruct lookupConstruct(String id) {
        LanguageConstruct construct = LANGUAGE_CONSTRUCTS.get(id);
        if (construct == null) {
            throw new IllegalArgumentException("The language construct with id \"" + id + "\" could not be found.");
        }
        return construct;
    }
}
