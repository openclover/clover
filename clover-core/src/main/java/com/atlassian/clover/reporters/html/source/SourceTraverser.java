package com.atlassian.clover.reporters.html.source;

import com.atlassian.clover.registry.entities.FullFileInfo;

import java.io.Reader;

public interface SourceTraverser<L extends SourceListener> {
    void traverse(Reader sourceReader, FullFileInfo fileInfo, L listener) throws Exception;
}
