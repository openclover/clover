package org.openclover.core.api.registry;

import org.openclover.core.api.registry.FileInfo;

/**
 * used to visit all FileInfos in a model
 */
public interface FileInfoVisitor {

    void visitFileInfo(FileInfo file);

}
