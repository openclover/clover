package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a code entity containing source files. It can be a project, module, package etc.
 */
public interface HasFiles {
    /**
     * Returns list of files
     *
     * @return List&lt;FileInfo&gt; - list of files or empty list if none
     */
    @NotNull
    List<FileInfo> getFiles();

    void visitFiles(final FileInfoVisitor visitor);
}
