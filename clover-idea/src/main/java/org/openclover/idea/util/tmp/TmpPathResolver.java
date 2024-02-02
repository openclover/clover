package org.openclover.idea.util.tmp;

import com.intellij.openapi.vfs.VirtualFile;

public interface TmpPathResolver {
    void registerMapping(VirtualFile origFile, String tmpFile);

    VirtualFile getMapping(String tmpFile);
}
