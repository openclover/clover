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

import static org.openclover.core.spi.lang.LanguageConstruct.Builtin.BRANCH;


public class FullBranchInfo extends FullElementInfo<BasicElementInfo>
        implements TaggedPersistent, BranchInfo {

    private transient MethodInfo containingMethod;
    private final boolean isInstrumented;

    public FullBranchInfo(
            MethodInfo containingMethod, int relativeDataIndex, ContextSet context,
            SourceInfo region, int complexity, boolean isInstrumented) {
        this(containingMethod, relativeDataIndex, context, region, complexity, isInstrumented, BRANCH);
    }

    public FullBranchInfo(
            MethodInfo containingMethod, int relativeDataIndex, ContextSet context,
            SourceInfo region, int complexity, boolean isInstrumented, LanguageConstruct construct) {
        super(context, new BasicElementInfo(region, relativeDataIndex, complexity, construct));
        this.containingMethod = containingMethod;
        this.isInstrumented = isInstrumented;
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
        return isInstrumented;
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
        return new FullBranchInfo(method, sharedInfo.getRelativeDataIndex(), context,
                sharedInfo.getRegion(), sharedInfo.getComplexity(), isInstrumented);
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
        out.writeBoolean(isInstrumented);
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
