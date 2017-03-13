package com.atlassian.clover.api.registry;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a code entity containing source files. It can be a project, module, package etc.
 */
public interface HasFiles {
    /**
     * Returns list of files
     *
     * @return List&lt;? extends FileInfo&gt; - list of files or empty list if none
     */
    @NotNull
    List<? extends FileInfo> getFiles();

}
