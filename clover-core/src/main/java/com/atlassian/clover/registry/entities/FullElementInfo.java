package com.atlassian.clover.registry.entities;


import com.atlassian.clover.api.registry.ContextSet;
import com.atlassian.clover.api.registry.ElementInfo;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.CoverageDataReceptor;
import com.atlassian.clover.registry.FileInfoRegion;
import com.atlassian.clover.spi.lang.LanguageConstruct;
import com.atlassian.clover.context.NamedContext;
import org.jetbrains.annotations.NotNull;

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

    public void setContext(ContextSet context) {
        this.context = context;
    }

    /**
     * 
     * @param filter the {@link ContextSet} whereby Contexts to be filtered out are set to 1,
     * and preserved contexts are 0.
     * @return <code>true</code> if this element info is filtered out. i.e. excluded
     * @see com.atlassian.clover.context.ContextStore
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
        return ((FullFileInfo)getContainingFile()).dataIndex + sharedInfo.getRelativeDataIndex();
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
