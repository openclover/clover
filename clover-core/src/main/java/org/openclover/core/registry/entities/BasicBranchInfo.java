package org.openclover.core.registry.entities;

import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.spi.lang.LanguageConstruct;

public class BasicBranchInfo extends BasicElementInfo {
    private final boolean instrumented;

    public BasicBranchInfo(SourceInfo region, int relativeDataIndex, int complexity, boolean instrumented) {
        this(region, relativeDataIndex, complexity, instrumented, LanguageConstruct.Builtin.BRANCH);
    }

    public BasicBranchInfo(SourceInfo region, int relativeDataIndex, int complexity, boolean instrumented, LanguageConstruct construct) {
        super(region, relativeDataIndex, complexity, construct);
        this.instrumented = instrumented;
    }

    public boolean isInstrumented() {
        return instrumented;
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "BasicBranchInfo{" +
            "instrumented=" + instrumented +
            "} " + super.toString();
    }
    ///CLOVER:ON
}
