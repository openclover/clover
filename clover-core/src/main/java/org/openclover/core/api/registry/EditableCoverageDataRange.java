package org.openclover.core.api.registry;

public interface EditableCoverageDataRange extends CoverageDataRange {

    void setDataIndex(int index);

    void setDataLength(int length);
}
