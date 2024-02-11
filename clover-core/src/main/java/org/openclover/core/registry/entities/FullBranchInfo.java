package org.openclover.core.registry.entities;

import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.EntityContainer;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.core.lang.Languages;
import org.openclover.core.registry.CoverageDataProvider;
import org.openclover.core.spi.lang.LanguageConstruct;
import org.openclover.core.registry.FixedSourceRegion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;


public class FullBranchInfo extends FullElementInfo<BasicBranchInfo> implements TaggedPersistent, BranchInfo {
    private transient FullMethodInfo containingMethod;

    public FullBranchInfo(
            FullMethodInfo containingMethod, int relativeDataIndex, ContextSet context,
            SourceInfo region, int complexity, boolean instrumented) {
        this(containingMethod, relativeDataIndex, context, region, complexity, instrumented, LanguageConstruct.Builtin.BRANCH);
    }

    public FullBranchInfo(
            FullMethodInfo containingMethod, int relativeDataIndex, ContextSet context,
            SourceInfo region, int complexity, boolean instrumented, LanguageConstruct construct) {
        this(containingMethod, context, new BasicBranchInfo(region, relativeDataIndex, complexity, instrumented, construct));
    }

    private FullBranchInfo(
            FullMethodInfo containingMethod, ContextSet context, BasicBranchInfo sharedInfo) {
        super(context, sharedInfo);
        this.containingMethod = containingMethod;
    }

    @Override
    public int getTrueHitCount() {
        return super.getHitCount();
    }

    @Override
    public int getFalseHitCount() {
        final CoverageDataProvider data = getDataProvider();
        if (data == null) {
            return 0;
        }
        return data.getHitCount(getDataIndex() + 1);
    }

    @Override
    public EntityContainer getParent() {
        return containingMethod;
    }

    @Override
    public boolean isInstrumented() {
        return sharedInfo.isInstrumented();
    }

    public FullBranchInfo copy(FullMethodInfo method) {
        return new FullBranchInfo(method, context, sharedInfo);
    }

    @Override
    public void setDataProvider(CoverageDataProvider data) {
        throw new UnsupportedOperationException("setDataProvider not supported on FullBranchInfo");
    }

    @Override
    public CoverageDataProvider getDataProvider() {
        return containingMethod.getDataProvider();
    }

    @Override
    public int getDataLength() {
        return 2;
    }

    void setContainingMethod(FullMethodInfo methodInfo) {
        this.containingMethod = methodInfo;
    }

    @Override
    @Nullable
    public FileInfo getContainingFile() {
        return containingMethod.getContainingFile();
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "FullBranchInfo{" +
            "sharedInfo=" + sharedInfo +
            ", context=" + context +
            '}';
    }
    ///CLOVER:ON

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        out.write(org.openclover.core.context.ContextSet.class, (org.openclover.core.context.ContextSet)context);
        out.writeInt(sharedInfo.getRelativeDataIndex());
        out.writeInt(getComplexity());
        out.writeUTF(sharedInfo.getConstruct().getId());
        out.writeBoolean(sharedInfo.isInstrumented());
        FixedSourceRegion.writeRaw(this, out);
    }

    public static FullBranchInfo read(TaggedDataInput in) throws IOException {
        final ContextSet context = in.read(org.openclover.core.context.ContextSet.class);
        final int relativeDataIndex = in.readInt();
        final int complexity = in.readInt();
        final LanguageConstruct construct = Languages.lookupConstruct(in.readUTF());
        final boolean isInstrumented = in.readBoolean();
        final FixedSourceRegion region = FixedSourceRegion.read(in);

        return new FullBranchInfo(null, relativeDataIndex, context, region, complexity, isInstrumented, construct);
    }
}
