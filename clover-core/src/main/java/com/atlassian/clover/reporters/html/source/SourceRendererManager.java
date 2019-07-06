package com.atlassian.clover.reporters.html.source;

import com.atlassian.clover.reporters.html.source.java.JavaSourceRenderer;
import com.atlassian.clover.services.ServiceLocator;
import com.atlassian.clover.services.ServiceNotAvailableException;
import com.atlassian.clover.spi.lang.Language;
import com.atlassian.clover.spi.reporters.html.source.SourceRenderer;
import com.atlassian.clover.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SourceRendererManager {
    private static final PlaintextSourceRenderer PLAINTEXT_RENDERER = new PlaintextSourceRenderer();
    private static final Map<String, SourceRenderer> RENDERERS;

    static {
        RENDERERS = Collections.unmodifiableMap(new HashMap<String, SourceRenderer>() {{
                registerRenderer(new JavaSourceRenderer());

                //Must use Class.forName() because we can't see this class in this module
                try {
                    registerRenderer((SourceRenderer) Class.forName("com.atlassian.clover.reporters.html.source.groovy.GroovySourceRenderer").getDeclaredConstructor().newInstance());
                } catch (Exception e) {
                    Logger.getInstance().warn("Failed to register the Groovy source renderer - syntax highlighting will not be performed for Groovy code", e);
                }

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
