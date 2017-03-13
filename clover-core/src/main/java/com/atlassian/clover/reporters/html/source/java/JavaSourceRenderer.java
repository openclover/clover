package com.atlassian.clover.reporters.html.source.java;

import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.spi.lang.Language;
import com.atlassian.clover.spi.reporters.html.source.HtmlRenderingSupport;
import com.atlassian.clover.spi.reporters.html.source.LineRenderInfo;
import com.atlassian.clover.spi.reporters.html.source.SourceRenderer;

import java.io.Reader;
import java.util.List;

public class JavaSourceRenderer implements SourceRenderer {
    @Override
    public Language getSupportedLanguage() {
        return Language.Builtin.JAVA;
    }

    @Override
    public void render(List<LineRenderInfo> linesToRender, Reader sourceReader, FullFileInfo finfo, HtmlRenderingSupport renderingHelper, String emptyCoverageMsg, String tabString, String spaceString) throws Exception {
        new JavaTokenTraverser().traverse(
            sourceReader, finfo, new JavaHtmlSourceRenderer(finfo, linesToRender, renderingHelper, emptyCoverageMsg, tabString, spaceString));
    }
}
