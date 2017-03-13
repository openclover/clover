package com.atlassian.clover.spi.reporters.html.source;

import com.atlassian.clover.spi.lang.Language;
import com.atlassian.clover.registry.entities.FullFileInfo;

import java.io.Reader;
import java.util.List;

public interface SourceRenderer {
    Language getSupportedLanguage();
    void render(List<LineRenderInfo> linesToRender, Reader sourceReader, FullFileInfo finfo, HtmlRenderingSupport renderingHelper, String emptyCoverageMsg, String tabString, String spaceString) throws Exception;
}
