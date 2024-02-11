package org.openclover.core.api.registry;

import org.openclover.core.spi.lang.LanguageConstruct;
import org.jetbrains.annotations.NotNull;

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

}
