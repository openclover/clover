package org.openclover.core.registry.entities;

import org.jetbrains.annotations.Nullable;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.EntityContainer;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.api.registry.StatementInfo;
import org.openclover.core.context.ContextSetImpl;
import org.openclover.core.io.tags.TaggedDataInput;
import org.openclover.core.io.tags.TaggedDataOutput;
import org.openclover.core.io.tags.TaggedPersistent;
import org.openclover.core.lang.Languages;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.CoverageDataReceptor;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.spi.lang.LanguageConstruct;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class FullStatementInfo extends FullElementInfo<BasicElementInfo> implements TaggedPersistent, StatementInfo {

    /** Parent entity - method, class or file */
    private transient ParentEntity parent;

    public FullStatementInfo(MethodInfo containingMethod, int relativeDataIndex, ContextSet context,
                             SourceInfo region, int complexity) {

        this(containingMethod, relativeDataIndex, context, region, complexity, LanguageConstruct.Builtin.STATEMENT);
    }

    public FullStatementInfo(MethodInfo containingMethod, int relativeDataIndex, ContextSet context,
                            SourceInfo region, int complexity, LanguageConstruct construct) {

        this(containingMethod, context, new BasicStatementInfo(region, relativeDataIndex, complexity, construct));
    }

    /**
     * For deserialization only. It does not set the parent. Set it using {@link #setContainingClass(ClassInfo)} /
     * {@link #setContainingMethod(MethodInfo)} or {@link #setContainingFile(FileInfo)}
     */
    private FullStatementInfo(int relativeDataIndex, ContextSet context,
                              SourceInfo region, int complexity, LanguageConstruct construct) {
        this((ParentEntity)null, context, new BasicStatementInfo(region, relativeDataIndex, complexity, construct));
    }

    /**
     * Constructor when method is a statement's parent
     */
    public FullStatementInfo(MethodInfo containingMethod, ContextSet context, BasicElementInfo sharedInfo) {
        this(new ParentEntity(containingMethod), context, sharedInfo);
    }

    /**
     * Constructor when class is a statement's parent
     */
    public FullStatementInfo(ClassInfo containingClass, ContextSet context, BasicElementInfo sharedInfo) {
        this(new ParentEntity(containingClass), context, sharedInfo);
    }

    /**
     * Constructor when file is a statement's parent
     */
    public FullStatementInfo(FileInfo containingFile, ContextSet context, BasicElementInfo sharedInfo) {
        this(new ParentEntity(containingFile), context, sharedInfo);
    }

    /**
     * Last constructor in the chain
     */
    private FullStatementInfo(ParentEntity parent, ContextSet context, BasicElementInfo sharedInfo) {
        super(context, sharedInfo);
        this.parent = parent;
    }

    @Override
    public StatementInfo copy(final MethodInfo parentMethod) {
        return new FullStatementInfo(parentMethod, context, sharedInfo);
    }

    @Override
    public StatementInfo copy(final ClassInfo parentClass) {
        return new FullStatementInfo(parentClass, context, sharedInfo);
    }

    @Override
    public StatementInfo copy(final FileInfo parentFile) {
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

    @Override
    public void setContainingClass(ClassInfo containingClass) {
        parent = new ParentEntity(containingClass);
    }

    @Override
    public void setContainingMethod(MethodInfo methodInfo) {
        parent = new ParentEntity(methodInfo);
    }

    @Override
    public void setContainingFile(FileInfo containingFile) {
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
        out.write(ContextSetImpl.class, (ContextSetImpl)context);
        out.writeInt(sharedInfo.getRelativeDataIndex());
        out.writeInt(getComplexity());
        out.writeUTF(sharedInfo.getConstruct().getId());
        FixedSourceRegion.writeRaw(this, out);
    }

    /**
     * @param in input source
     * @return FullStatementInfo you must set the parent !
     */
    public static FullStatementInfo read(TaggedDataInput in) throws IOException {
        // read statement metadata
        final ContextSet context = in.read(ContextSetImpl.class);
        final int relativeDataIndex = in.readInt();
        final int complexity = in.readInt();
        final LanguageConstruct construct = Languages.lookupConstruct(in.readUTF());
        final FixedSourceRegion region = FixedSourceRegion.read(in);

        // construct statement object
        return new FullStatementInfo(relativeDataIndex, context, region, complexity, construct);
    }

}
