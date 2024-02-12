package org.openclover.core.reporters.html.source;

import org.openclover.core.spi.reporters.html.source.HtmlRenderingSupport;
import org.openclover.core.spi.reporters.html.source.LineRenderInfo;

import java.util.List;

public class PlaintextHtmlSourceRenderer implements SourceListener {
    protected StringBuffer out = new StringBuffer();
    protected final HtmlRenderingSupport renderingHelper;
    protected String emptyCoverageMsg;
    protected final String tab;
    protected final String space;
    protected final List<LineRenderInfo> lineInfo;

    public PlaintextHtmlSourceRenderer(List<LineRenderInfo> lineInfo, HtmlRenderingSupport renderingHelper, String emptyCoverageMsg, String tab, String space) {
        this.lineInfo = lineInfo;
        this.renderingHelper = renderingHelper;
        this.emptyCoverageMsg = emptyCoverageMsg;
        this.tab = tab;
        this.space = space;
    }

    @Override
    public void onChunk(String s) {
        out.append(renderingHelper.htmlEscapeStr(s, tab, space));
    }

    @Override
    public void onNewLine() {
        newLine();
    }

    @Override
    public void onStartDocument() { }

    @Override
    public void onEndDocument() {
        if (out.length() > 0) {
            newLine();
        }
    }

    private void newLine() {
        LineRenderInfo thisLine = new LineRenderInfo(emptyCoverageMsg);
        thisLine.setSrc(out.toString());
        lineInfo.add(thisLine);
        out = new StringBuffer();
    }

}
