package org.openclover.core.registry.entities;

import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.EntityContainer;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.context.ContextSetImpl;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.core.lang.Languages;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.spi.lang.LanguageConstruct;

import java.io.IOException;


public class FullBranchInfo extends FullElementInfo<BasicBranchInfo>
        implements TaggedPersistent, BranchInfo {

    private transient MethodInfo containingMethod;

    public FullBranchInfo(
            MethodInfo containingMethod, int relativeDataIndex, ContextSet context,
            SourceInfo region, int complexity, boolean instrumented) {
        this(containingMethod, relativeDataIndex, context, region, complexity, instrumented, LanguageConstruct.Builtin.BRANCH);
    }

    public FullBranchInfo(
            MethodInfo containingMethod, int relativeDataIndex, ContextSet context,
            SourceInfo region, int complexity, boolean instrumented, LanguageConstruct construct) {
        this(containingMethod, context, new BasicBranchInfo(region, relativeDataIndex, complexity, instrumented, construct));
    }

    private FullBranchInfo(
            MethodInfo containingMethod, ContextSet context, BasicBranchInfo sharedInfo) {
        super(context, sharedInfo);
        this.containingMethod = containingMethod;
    }

    // BranchInfo

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
    public boolean isInstrumented() {
        return sharedInfo.isInstrumented();
    }

    // HasParent

    @Override
    public EntityContainer getParent() {
        return containingMethod;
    }

    // CoverageDataReceptor

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

    // FileInfoRegion

    @Override
    @Nullable
    public FileInfo getContainingFile() {
        return containingMethod.getContainingFile();
    }

    // OTHER

    @Override
    public BranchInfo copy(MethodInfo method) {
        return new FullBranchInfo(method, context, sharedInfo);
    }

    @Override
    public void setContainingMethod(MethodInfo methodInfo) {
        this.containingMethod = methodInfo;
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
        out.write(ContextSetImpl.class, (ContextSetImpl)context);
        out.writeInt(sharedInfo.getRelativeDataIndex());
        out.writeInt(getComplexity());
        out.writeUTF(sharedInfo.getConstruct().getId());
        out.writeBoolean(sharedInfo.isInstrumented());
        FixedSourceRegion.writeRaw(this, out);
    }

    public static FullBranchInfo read(TaggedDataInput in) throws IOException {
        final ContextSet context = in.read(ContextSetImpl.class);
        final int relativeDataIndex = in.readInt();
        final int complexity = in.readInt();
        final LanguageConstruct construct = Languages.lookupConstruct(in.readUTF());
        final boolean isInstrumented = in.readBoolean();
        final FixedSourceRegion region = FixedSourceRegion.read(in);

        return new FullBranchInfo(null, relativeDataIndex, context, region, complexity, isInstrumented, construct);
    }
}
