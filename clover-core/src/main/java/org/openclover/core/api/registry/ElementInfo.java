package org.openclover.core.api.registry;

import org.openclover.core.spi.lang.LanguageConstruct;

/**
 *
 */
public interface ElementInfo extends SourceInfo {
    int getHitCount();

    ContextSet getContext();

    /**
     * Returns cyclomatic complexity of this code element
     * @return int complexity
     */
    int getComplexity();

    LanguageConstruct getConstruct();

    boolean isFiltered(ContextSet filter);
}

