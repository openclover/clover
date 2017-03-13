package com.atlassian.clover.reporters.html.source.groovy;

import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.spi.lang.Language;
import com.atlassian.clover.spi.reporters.html.source.HtmlRenderingSupport;
import com.atlassian.clover.spi.reporters.html.source.SourceRenderer;
import com.atlassian.clover.spi.reporters.html.source.LineRenderInfo;

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
