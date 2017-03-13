package com.atlassian.clover.api.registry;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a code entity containing statements or their equivalent (expressions, for instance)
 */
public interface HasStatements {
    /**
     * Returns list of statements
     * @return List&lt;? extends StatementInfo&gt; - list of statements or empty list if none
     */
    @NotNull
    public List<? extends StatementInfo> getStatements();

}
