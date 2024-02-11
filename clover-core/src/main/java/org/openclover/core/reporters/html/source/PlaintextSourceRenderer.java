package org.openclover.core.reporters.html.source;

import org.openclover.core.spi.lang.Language;
import org.openclover.core.spi.reporters.html.source.HtmlRenderingSupport;
import org.openclover.core.spi.reporters.html.source.LineRenderInfo;
import org.openclover.core.spi.reporters.html.source.SourceRenderer;
import org.openclover.core.registry.entities.FullFileInfo;

import java.io.Reader;
import java.util.List;

public class PlaintextSourceRenderer implements SourceRenderer {
    @Override
    public Language getSupportedLanguage() {
        return null;
    }

    @Override
    public void render(List<LineRenderInfo> linesToRender, Reader sourceReader, FullFileInfo finfo, HtmlRenderingSupport renderingHelper, String emptyCoverageMsg, String tabString, String spaceString) throws Exception {
        new PlaintextLineTraverser<PlaintextHtmlSourceRenderer>().traverse(
            sourceReader, finfo, new PlaintextHtmlSourceRenderer(linesToRender, renderingHelper, emptyCoverageMsg, tabString, spaceString));
    }
}
