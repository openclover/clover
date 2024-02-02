package org.openclover.idea.build;

/**
 *
 */
public interface InclusionDetector {
    boolean isIncluded();

    boolean isModuleExcluded();

    /**
     * The file may be included because it's path was not found - such files are included by default
     * @return path was not found so file was included by default
     */
    boolean isPathNotFound();

    boolean isPatternExcluded();

    boolean isInNoninstrumentedTestSources();

    boolean isCloverDisabled();

    boolean isNotJava();

    boolean isModuleNotFound();
}
