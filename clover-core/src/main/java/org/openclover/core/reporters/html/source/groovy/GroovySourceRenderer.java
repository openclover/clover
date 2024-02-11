package org.openclover.core.reporters.html.source.groovy;

import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.spi.lang.Language;
import org.openclover.core.spi.reporters.html.source.HtmlRenderingSupport;
import org.openclover.core.spi.reporters.html.source.SourceRenderer;
import org.openclover.core.spi.reporters.html.source.LineRenderInfo;

import java.io.Reader;
import java.util.List;

public class GroovySourceRenderer implements SourceRenderer {
    @Override
    public Language getSupportedLanguage() {
        return Language.Builtin.GROOVY;
    }

    @Override
    public void render(List<LineRenderInfo> linesToRender, Reader sourceReader, FullFileInfo finfo, HtmlRenderingSupport renderingHelper, String emptyCoverageMsg, String tabString, String spaceString) throws Exception {
        new GroovySourceTraverser().traverse(
            sourceReader, finfo, new GroovyHtmlSourceRenderer(finfo, linesToRender, renderingHelper, emptyCoverageMsg, tabString, spaceString));
    }
}
