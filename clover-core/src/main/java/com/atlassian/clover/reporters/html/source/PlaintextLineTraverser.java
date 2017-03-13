package com.atlassian.clover.reporters.html.source;

import com.atlassian.clover.registry.entities.FullFileInfo;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;

public class PlaintextLineTraverser<L extends SourceListener> implements SourceTraverser<L> {
    @Override
    public void traverse(Reader sourceReader, FullFileInfo fileInfo, L listener) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(sourceReader);
        String line = bufferedReader.readLine();

        listener.onStartDocument();
        while (line != null) {
            listener.onChunk(line);
            line = bufferedReader.readLine();
            if (line != null) {
                listener.onNewLine();
            }
        }
        listener.onEndDocument();
    }
}
