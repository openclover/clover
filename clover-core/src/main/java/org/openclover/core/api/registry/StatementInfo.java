package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.spi.lang.LanguageConstruct;

/**
 * Represents a single statement.
 */
public interface StatementInfo extends ElementInfo, InstrumentationInfo, HasParent {

    /**
     * Return kind of the code construct (statement, branch, elvis expression etc)
     *
     * @return LanguageConstruct
     */
    @NotNull
    LanguageConstruct getConstruct();

    boolean isFiltered(ContextSet filter);

}
