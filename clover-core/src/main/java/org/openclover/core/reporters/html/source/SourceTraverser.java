package org.openclover.core.reporters.html.source;

import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.registry.entities.FullFileInfo;

import java.io.Reader;

public interface SourceTraverser<L extends SourceListener> {
    void traverse(Reader sourceReader, FileInfo fileInfo, L listener) throws Exception;
}
