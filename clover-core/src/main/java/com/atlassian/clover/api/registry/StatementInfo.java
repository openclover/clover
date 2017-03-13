package com.atlassian.clover.api.registry;

import com.atlassian.clover.spi.lang.LanguageConstruct;
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
    public LanguageConstruct getConstruct();

}
