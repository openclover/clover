package org.openclover.core.registry;

import org.openclover.core.registry.entities.BaseFileInfo;

/**
 * used to visit all FileInfos in a model
 */
public interface FileInfoVisitor {

    void visitFileInfo(BaseFileInfo file);

}
