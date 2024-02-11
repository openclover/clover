package org.openclover.core.registry;

public interface CoverageDataRange {
    /**
     * @return the starting index for this element (or its first child)
     */
    int getDataIndex();

    /**
     * @return the minimum length (from the data index) of coverage data to ensure enough data for this
     * element and all children.
     */
    int getDataLength();
}
