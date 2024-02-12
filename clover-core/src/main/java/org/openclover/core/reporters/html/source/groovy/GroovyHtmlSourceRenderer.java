package org.openclover.core.reporters.html.source.groovy;

import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.reporters.html.source.java.JavaHtmlSourceRenderer;
import org.openclover.core.spi.reporters.html.source.HtmlRenderingSupport;
import org.openclover.core.spi.reporters.html.source.LineRenderInfo;
import org.openclover.core.spi.reporters.html.source.SourceReportCss;

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
