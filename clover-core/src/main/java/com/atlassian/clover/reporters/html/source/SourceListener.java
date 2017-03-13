package com.atlassian.clover.reporters.html.source;

public interface SourceListener {
    public void onStartDocument();
    public void onEndDocument();
    public void onNewLine();
    public void onChunk(String s);
}
