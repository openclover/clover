package com.atlassian.clover.registry.entities;

import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.ContextSet;
import com.atlassian.clover.api.registry.EntityContainer;
import com.atlassian.clover.api.registry.EntityVisitor;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.SourceInfo;
import com.atlassian.clover.api.registry.StatementInfo;
import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedDataOutput;
import com.atlassian.clover.io.tags.TaggedPersistent;
import com.atlassian.clover.lang.Languages;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.CoverageDataReceptor;
import com.atlassian.clover.spi.lang.LanguageConstruct;
import com.atlassian.clover.registry.FixedSourceRegion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class FullStatementInfo extends FullElementInfo<BasicElementInfo> implements TaggedPersistent, StatementInfo {

    /** Parent entity - method, class or file */
    private transient ParentEntity parent;

    public FullStatementInfo(FullMethodInfo containingMethod, int relativeDataIndex, ContextSet context,
                            SourceInfo region, int complexity) {

        this(containingMethod, relativeDataIndex, context, region, complexity, LanguageConstruct.Builtin.STATEMENT);
    }

    public FullStatementInfo(FullMethodInfo containingMethod, int relativeDataIndex, ContextSet context,
                            SourceInfo region, int complexity, LanguageConstruct construct) {

        this(containingMethod, context, new BasicStatementInfo(region, relativeDataIndex, complexity, construct));
    }

    /**
     * For deserialization only. It does not set the parent. Set it using {@link #setContainingClass(FullClassInfo)} /
     * {@link #setContainingMethod(FullMethodInfo)} or {@link #setContainingFile(FullFileInfo)}
     */
    private FullStatementInfo(int relativeDataIndex, ContextSet context,
                              SourceInfo region, int complexity, LanguageConstruct construct) {
        this((ParentEntity)null, context, new BasicStatementInfo(region, relativeDataIndex, complexity, construct));
    }

    /**
     * Constructor when method is a statement's parent
     */
    public FullStatementInfo(FullMethodInfo containingMethod, ContextSet context, BasicElementInfo sharedInfo) {
        this(new ParentEntity(containingMethod), context, sharedInfo);
    }

    /**
     * Constructor when class is a statement's parent
     */
    public FullStatementInfo(FullClassInfo containingClass, ContextSet context, BasicElementInfo sharedInfo) {
        this(new ParentEntity(containingClass), context, sharedInfo);
    }

    /**
     * Constructor when file is a statement's parent
     */
    public FullStatementInfo(FullFileInfo containingFile, ContextSet context, BasicElementInfo sharedInfo) {
        this(new ParentEntity(containingFile), context, sharedInfo);
    }

    /**
     * Last constructor in the chain
     */
    private FullStatementInfo(ParentEntity parent, ContextSet context, BasicElementInfo sharedInfo) {
        super(context, sharedInfo);
        this.parent = parent;
    }

    public FullStatementInfo copy(final FullMethodInfo parentMethod) {
        return new FullStatementInfo(parentMethod, context, sharedInfo);
    }

    public FullStatementInfo copy(final FullClassInfo parentClass) {
        return new FullStatementInfo(parentClass, context, sharedInfo);
    }

    public FullStatementInfo copy(final FullFileInfo parentFile) {
        return new FullStatementInfo(parentFile, context, sharedInfo);
    }

    @Override
    public void setDataProvider(CoverageDataProvider data) {
        throw new UnsupportedOperationException("setDataProvider not supported on FullStatementInfo");
    }

    @Override
    public CoverageDataProvider getDataProvider() {
        // get data provider from the parent entity (file, class or method)
        final AtomicReference<CoverageDataProvider> dataProvider = new AtomicReference<>();
        parent.getParentEntity().visit(new EntityVisitor() {
            @Override
            public void visitFile(FileInfo parentFile) {
                dataProvider.set( ((CoverageDataReceptor) parentFile).getDataProvider());
            }

            @Override
            public void visitClass(ClassInfo parentClass) {
                dataProvider.set( ((CoverageDataReceptor) parentClass).getDataProvider());
            }

            @Override
            public void visitMethod(MethodInfo parentMethod) {
                dataProvider.set( ((CoverageDataReceptor) parentMethod).getDataProvider());
            }
        });
        return dataProvider.get();
    }

    @Override
    public int getDataLength() {
        return 1;
    }

    protected void setContainingClass(FullClassInfo containingClass) {
        parent = new ParentEntity(containingClass);
    }

    protected void setContainingMethod(FullMethodInfo methodInfo) {
        parent = new ParentEntity(methodInfo);
    }

    protected void setContainingFile(FullFileInfo containingFile) {
        if (parent != null) {
            parent.setContainingFile(containingFile);
        } else {
            parent = new ParentEntity(containingFile);
        }
    }

    @Nullable
    public ClassInfo getContainingClass() {
        return parent.getContainingClass();
    }

    @Nullable
    public MethodInfo getContainingMethod() {
        return parent.getContainingMethod();
    }

    @Override
    @Nullable
    public FileInfo getContainingFile() {
        return parent.getContainingFile();
    }

    @Override
    public EntityContainer getParent() {
        return parent.getParentEntity();
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "FullStatementInfo{" +
            "sharedInfo=" + sharedInfo +
            ", context=" + context +
            '}';
    }
    ///CLOVER:ON

    @Override
    public void write(TaggedDataOutput out) throws IOException {
        // write statement metadata
        out.write(com.atlassian.clover.context.ContextSet.class, (com.atlassian.clover.context.ContextSet)context);
        out.writeInt(sharedInfo.getRelativeDataIndex());
        out.writeInt(getComplexity());
        out.writeUTF(sharedInfo.getConstruct().getId());
        FixedSourceRegion.writeRaw(this, out);
    }

    /**
     *
     * @param in input source
     * @return FullStatementInfo you must set the parent !
     * @throws IOException
     */
    public static FullStatementInfo read(TaggedDataInput in) throws IOException {
        // read statement metadata
        final ContextSet context = in.read(com.atlassian.clover.context.ContextSet.class);
        final int relativeDataIndex = in.readInt();
        final int complexity = in.readInt();
        final LanguageConstruct construct = Languages.lookupConstruct(in.readUTF());
        final FixedSourceRegion region = FixedSourceRegion.read(in);

        // construct statement object
        return new FullStatementInfo(relativeDataIndex, context, region, complexity, construct);
    }

}
