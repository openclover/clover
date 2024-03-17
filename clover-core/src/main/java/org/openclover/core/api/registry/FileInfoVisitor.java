package org.openclover.core.api.registry;

/**
 * used to visit all FileInfos in a model
 */
public interface FileInfoVisitor {

    void visitFileInfo(FileInfo file);

}
