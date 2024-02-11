package org.openclover.core.reporters.html.source;

import org.openclover.core.reporters.html.source.groovy.GroovySourceRenderer;
import org.openclover.core.reporters.html.source.java.JavaSourceRenderer;
import org.openclover.core.services.ServiceLocator;
import org.openclover.core.services.ServiceNotAvailableException;
import org.openclover.core.spi.lang.Language;
import org.openclover.core.spi.reporters.html.source.SourceRenderer;
import org.openclover.runtime.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SourceRendererManager {
    private static final PlaintextSourceRenderer PLAINTEXT_RENDERER = new PlaintextSourceRenderer();
    private static final Map<String, SourceRenderer> RENDERERS;

    static {
        RENDERERS = Collections.unmodifiableMap(new HashMap<String, SourceRenderer>() {{
                registerRenderer(new JavaSourceRenderer());
                registerRenderer(new GroovySourceRenderer());

                for (SourceRenderer sourceRenderer : ServiceLocator.load(SourceRenderer.class, SourceRenderHelper.class.getClassLoader())) {
                    try {
                        registerRenderer(sourceRenderer);
                    } catch (ServiceNotAvailableException e) {
                        Logger.getInstance().warn("Failed to register source renderer - syntax highlighting will not be performed for any of its supported languages", e);
                    }
                }
            }

            private void registerRenderer(SourceRenderer sourceRenderer) {
                Language language = sourceRenderer.getSupportedLanguage();
                for (String extension : language.getFileExtensions()) {
                    Logger.getInstance().debug("Registering renderer " + sourceRenderer.getClass().getName() + " for language " + language + " for file extension " + extension);
                    SourceRenderer existingRenderer = get(extension);
                    if (existingRenderer != null) {
                        throw new IllegalArgumentException("Render already registered for files with extension " + extension + ": " + existingRenderer);
                    } else {
                        put(extension, sourceRenderer);
                    }
                }
            }
        });
    }

    public static PlaintextSourceRenderer getPlaintextRenderer() {
        return PLAINTEXT_RENDERER;
    }

    public static SourceRenderer getRendererForFileExtension(String fileExtension) {
        return RENDERERS.get(fileExtension);
    }
}
