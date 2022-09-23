package com.atlassian.clover.reporters.html.source;

public interface SourceListener {
    void onStartDocument();
    void onEndDocument();
    void onNewLine();
    void onChunk(String s);
}
