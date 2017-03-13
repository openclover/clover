package com.atlassian.clover.registry;

import com.atlassian.clover.registry.entities.BaseFileInfo;

/**
 * used to visit all FileInfos in a model
 */
public interface FileInfoVisitor {

    void visitFileInfo(BaseFileInfo file);

}
