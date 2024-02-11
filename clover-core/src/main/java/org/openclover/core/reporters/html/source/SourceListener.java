package org.openclover.core.reporters.html.source;

public interface SourceListener {
    void onStartDocument();
    void onEndDocument();
    void onNewLine();
    void onChunk(String s);
}
