package org.openclover.core.reporters.html.source;

import org.openclover.core.registry.entities.FullFileInfo;

import java.io.Reader;

public interface SourceTraverser<L extends SourceListener> {
    void traverse(Reader sourceReader, FullFileInfo fileInfo, L listener) throws Exception;
}
