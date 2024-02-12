package org.openclover.core.spi.reporters.html.source;

import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.spi.lang.Language;

import java.io.Reader;
import java.util.List;

public interface SourceRenderer {
    Language getSupportedLanguage();
    void render(List<LineRenderInfo> linesToRender, Reader sourceReader, FullFileInfo finfo, HtmlRenderingSupport renderingHelper, String emptyCoverageMsg, String tabString, String spaceString) throws Exception;
}
