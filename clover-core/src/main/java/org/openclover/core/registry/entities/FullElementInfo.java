package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.ElementInfo;
import org.openclover.core.context.ContextStore;
import org.openclover.core.context.NamedContext;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.CoverageDataReceptor;
import org.openclover.core.api.registry.FileInfoRegion;
import org.openclover.core.spi.lang.LanguageConstruct;

public abstract class FullElementInfo<T extends BasicElementInfo> implements CoverageDataReceptor, FileInfoRegion, ElementInfo {
    protected final T sharedInfo;
    protected ContextSet context;

    protected FullElementInfo(ContextSet context, T sharedInfo) {
        this.context = context;
        this.sharedInfo = sharedInfo;
    }

    @Override
    public ContextSet getContext() {
        return context;
    }

    @Override
    public void setContext(ContextSet context) {
        this.context = context;
    }

    /**
     * 
     * @param filter the {@link ContextSet} whereby Contexts to be filtered out are set to 1,
     * and preserved contexts are 0.
     * @return <code>true</code> if this element info is filtered out. i.e. excluded
     * @see ContextStore
     */
    public boolean isFiltered(ContextSet filter) {
        return filter != null && filter.intersects(context);
    }

    public void addContext(NamedContext ctx) {
        context = context.set(ctx.getIndex());
    }

    @Override
    public int getComplexity() {
        return sharedInfo.getComplexity();
    }

    public void setComplexity(int complexity) {
        sharedInfo.setComplexity(complexity);
    }

    @Override
    public int getHitCount() {
        final CoverageDataProvider data = getDataProvider();
        if (data == null) {
            return 0;
        }
        return data.getHitCount(getDataIndex());
    }

    public int getRelativeDataIndex() {
        return sharedInfo.getRelativeDataIndex();
    }

    @Override
    public int getDataIndex() {
        return getContainingFile().getDataIndex() + sharedInfo.getRelativeDataIndex();
    }

    @Override
    public int getStartLine() {
        return sharedInfo.getStartLine();
    }

    @Override
    public int getStartColumn() {
        return sharedInfo.getStartColumn();
    }

    @Override
    public int getEndLine() {
        return sharedInfo.getEndLine();
    }

    @Override
    public int getEndColumn() {
        return sharedInfo.getEndColumn();
    }

    @NotNull
    public LanguageConstruct getConstruct() {
        return sharedInfo.getConstruct();
    }
}
