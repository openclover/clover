package com.atlassian.clover.reporters.html.source.groovy;

import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.spi.reporters.html.source.HtmlRenderingSupport;
import com.atlassian.clover.spi.reporters.html.source.LineRenderInfo;
import com.atlassian.clover.spi.reporters.html.source.SourceReportCss;
import com.atlassian.clover.reporters.html.source.java.JavaHtmlSourceRenderer;

import java.util.List;

public class GroovyHtmlSourceRenderer extends JavaHtmlSourceRenderer implements GroovySourceListener {
    private static final String OPEN_REGEXP = "<span class=\"" + SourceReportCss.REGEXP_CLASS + "\">";
    private static final String CLOSE_REGEXP = CLOSE_SPAN;

    public GroovyHtmlSourceRenderer(FullFileInfo fileInfo, List<LineRenderInfo> lineInfo, HtmlRenderingSupport renderingHelper, String emptyCoverageMsg, String tab, String space) {
        super(fileInfo, lineInfo, renderingHelper, emptyCoverageMsg, tab, space);
    }

    @Override
    public void onRegexp(String s) {
        out.append(OPEN_REGEXP);
        out.append(renderingHelper.htmlEscapeStr(s, tab, space));
        out.append(CLOSE_REGEXP);
    }
}
