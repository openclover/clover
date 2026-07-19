package org.openclover.core.instr.tests;

import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.core.registry.entities.MethodSignature;
import org.openclover.core.registry.entities.Modifiers;
import org.openclover.core.spi.lang.Language;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public interface TestDetector extends TaggedPersistent {
    boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext);

    boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext);

    /**
     * Only the resolved detector types that can appear in a persisted
     * {@link org.openclover.core.cfg.instr.InstrumentationConfig} override this
     * (see the config's tag table). Non-persistable detectors inherit this and fail
     * fast if something tries to write them.
     */
    @Override
    default void write(TaggedDataOutput out) throws IOException {
        throw new UnsupportedOperationException(getClass().getName() + " is not persistable");
    }

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
