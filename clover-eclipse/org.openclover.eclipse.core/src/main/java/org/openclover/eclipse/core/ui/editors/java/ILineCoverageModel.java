package org.openclover.eclipse.core.ui.editors.java;

import java.util.Set;

import org.openclover.core.registry.entities.FullElementInfo;
import org.openclover.core.registry.entities.TestCaseInfo;

/**
 * Please note that values returned by hasMissesInLine() vs hasHitsInLine() and
 * hasPassedHitsInLine() vs hasFailedHitsInLine() are not mutually exclusive.
 * Sample combinations and their interpretation:
 *   <li>hasHitsInLine(x)==false            => has no coverage at all</li>
 *   <li>hasHitsInLine(x)==true             => has any coverage (from passed or failed tests, but also outside tests)</li>
 *   <li>hasMissesInLine(x)==true && hasHitsInLine(x)==true       => we have partial line coverage</li>
 *
 *   <li>hasPassedHitsInLine(x)==true       => has any coverage from at least one passed test</li>
 *   <li>hasFailedHitsInLine(x)==true       => has any coverage from at least one failed test</li>
 *   <li>hasPassedHitsInLine(x)==true && hasFailedHitsInLine(x)==true  => both passed and failed tests</li>
 *
 *   <li>hasHitsInLine(x)==true && hasPassedHitsInLine(x)==false && hasFailedHitsInLine(x)==false => code executed outside tests</li>
 */
public interface ILineCoverageModel {
    /**
     * Returns Entry object containing extra information about given line number
     * @param lineNumber
     * @return Entry
     */
    Entry getForLine(int lineNumber);

    /**
     * Returns true if given line number has any lacks of coverage.
     * @param lineNumber
     * @return true if at least part of line is not covered, false otherwise
     */
    boolean hasMissesInLine(int lineNumber);

    /**
     * Returns true if given line number has any coverage.
     * @param lineNumber
     * @return true if at least part of line is covered, false otherwise
     */
    boolean hasHitsInLine(int lineNumber);

    /**
     * Returns true if given line number has coverage resulting from at least one passed test case.
     * @param lineNumber
     * @return true if at least part of line is covered due to passing test, false otherwise
     */
    boolean hasPassedHitsInLine(int lineNumber);

    /**
     * Returns true if given line number has coverage resulting from at least one failed test case.
     * @param lineNumber
     * @return true if at least part of line is covered due to passing test, false otherwise
     */
    boolean hasFailedHitsInLine(int lineNumber);

    /**
     * Returns true if given line of given number has been filtered out by context filters.
     * @param lineNumber
     * @return true if line is filtered-out
     */
    public boolean isFilteredInLine(int lineNumber);

    interface Entry {
        FullElementInfo getElementInfo();
        Set<TestCaseInfo> getTestCaseInfos();
    }
}
