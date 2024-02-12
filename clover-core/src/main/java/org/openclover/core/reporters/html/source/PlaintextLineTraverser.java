package org.openclover.core.reporters.html.source;

import org.openclover.core.registry.entities.FullFileInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

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
