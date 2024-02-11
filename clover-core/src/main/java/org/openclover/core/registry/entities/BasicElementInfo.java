package org.openclover.core.registry.entities;

import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.ElementInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.openclover.core.registry.FixedSourceRegion;

public class BasicElementInfo implements ElementInfo {
    private FixedSourceRegion region; //May change during instrumentation
    private final int relativeDataIndex;
    private int complexity;
    private LanguageConstruct construct;

    public BasicElementInfo(SourceInfo region, int relativeDataIndex, int complexity, LanguageConstruct construct) {
        this.region = FixedSourceRegion.of(region);
        this.relativeDataIndex = relativeDataIndex;
        this.complexity = complexity;
        this.construct = construct;
    }

    public SourceInfo getRegion() {
        return region;
    }

    public int getRelativeDataIndex() {
        return relativeDataIndex;
    }

    @Override
    public int getHitCount() {
        throw new UnsupportedOperationException("Use FullElementInfo instead");
    }

    @Override
    public ContextSet getContext() {
        throw new UnsupportedOperationException("Use FullElementInfo instead");
    }

    @Override
    public int getComplexity() {
        return complexity;
    }

    @Override
    public int getStartLine() {
        return region.getStartLine();
    }

    @Override
    public int getStartColumn() {
        return region.getStartColumn();
    }

    @Override
    public int getEndLine() {
        return region.getEndLine();
    }

    @Override
    public int getEndColumn() {
        return region.getEndColumn();
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public void setRegion(SourceInfo region) {
        this.region = FixedSourceRegion.of(region);
    }

    public LanguageConstruct getConstruct() {
        return construct;
    }

    public void setConstruct(LanguageConstruct construct) {
        this.construct = construct;
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "BasicElementInfo{" +
            "region=" + region +
            ", relativeDataIndex=" + relativeDataIndex +
            ", complexity=" + complexity +
            '}';
    }
    ///CLOVER:ON
}
