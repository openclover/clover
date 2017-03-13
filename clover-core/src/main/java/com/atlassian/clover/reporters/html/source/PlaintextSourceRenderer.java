package com.atlassian.clover.reporters.html.source;

import com.atlassian.clover.spi.lang.Language;
import com.atlassian.clover.spi.reporters.html.source.HtmlRenderingSupport;
import com.atlassian.clover.spi.reporters.html.source.LineRenderInfo;
import com.atlassian.clover.spi.reporters.html.source.SourceRenderer;
import com.atlassian.clover.registry.entities.FullFileInfo;

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
