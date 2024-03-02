package org.openclover.core.api.registry;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.spi.lang.LanguageConstruct;

/**
 * Represents a single statement.
 */
public interface StatementInfo extends CoverageDataRange, ElementInfo, EntityEnclosure, HasParent {

    /**
     * Return a copy of the statement, setting a method as a parent.
     */
    StatementInfo copy(MethodInfo parentMethod);

    /**
     * Return a copy of the statement, setting a class as a parent.
     */
    StatementInfo copy(ClassInfo parentClass);

    /**
     * Return a copy of the statement, setting a file as a parent.
     */
    StatementInfo copy(FileInfo parentFile);

    /**
     * Return kind of the code construct (statement, branch, elvis expression etc)
     *
     * @return LanguageConstruct
     */
    @NotNull
    LanguageConstruct getConstruct();

    boolean isFiltered(ContextSet filter);

}
