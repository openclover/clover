package com.atlassian.clover.registry.entities;

import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.ContextSet;
import com.atlassian.clover.api.registry.EntityContainer;
import com.atlassian.clover.api.registry.EntityVisitor;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.api.registry.StatementInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BaseFileInfo implements FileInfo {

    protected String name;
    protected String encoding;
    protected int lineCount;
    protected int ncLineCount;

    protected long timestamp;
    protected long filesize;
    protected long checksum;

    protected transient BasePackageInfo containingPackage;
    protected transient BlockMetrics rawMetrics;
    protected transient BlockMetrics metrics;
    protected transient ContextSet contextFilter;

    public BaseFileInfo(BasePackageInfo containingPackage, String name, String encoding, int lineCount, int ncLineCount,
                        long timestamp, long filesize, long checksum) {
        this.name = name;
        this.encoding = encoding;
        this.containingPackage = containingPackage;
        this.lineCount = lineCount;
        this.ncLineCount = ncLineCount;
        this.timestamp = timestamp;
        this.filesize = filesize;
        this.checksum = checksum;
    }

    @Override
    public boolean isTestFile() {
        for (ClassInfo classInfo : getClasses()) {
            if (classInfo.isTestClass()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @NotNull
    public List<? extends ClassInfo> getClasses() {
        throw new UnsupportedOperationException("Use FullFileInfo instead");
    }

    @Override
    @NotNull
    public List<? extends ClassInfo> getAllClasses() {
        throw new UnsupportedOperationException("Use FullFileInfo instead");
    }

    @Override
    @NotNull
    public List<? extends MethodInfo> getMethods() {
        throw new UnsupportedOperationException("Use FullFileInfo instead");
    }

    @Override
    @NotNull
    public List<? extends MethodInfo> getAllMethods() {
        throw new UnsupportedOperationException("Use FullFileInfo instead");
    }

    @Override
    @NotNull
    public List<? extends StatementInfo> getStatements() {
        throw new UnsupportedOperationException("Use FullFileInfo instead");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BlockMetrics getMetrics() {
        return metrics;
    }

    @Override
    public void setMetrics(BlockMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public BlockMetrics getRawMetrics() {
        return rawMetrics;
    }    

    @Override
    public String getEncoding() {
        return encoding;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public long getFilesize() {
        return filesize;
    }

    @Override
    public long getChecksum() {
        return checksum;
    }

    @Override
    public String getPackagePath() {
        if (containingPackage == null) {
            throw new IllegalStateException("This FileInfo has no PackageInfo set on it yet");
        }
        return containingPackage.getPath() + getName();
    }

    @Override
    public PackageInfo getContainingPackage() {
        return containingPackage;
    }

    public void setContainingPackage(BasePackageInfo containingPackage) {
        this.containingPackage = containingPackage;
        for (ClassInfo classInfo : getClasses()) {
            ((BaseClassInfo) classInfo).setPackage(containingPackage);
        }
    }

    @Override
    public EntityContainer getParent() {
        return entityVisitor -> entityVisitor.visitPackage(containingPackage);
    }

    @Override
    public ContextSet getContextFilter() {
        return getContainingPackage().getContextFilter();
    }

    @Override
    public int getLineCount() {
        return lineCount;
    }

    @Override
    public int getNcLineCount() {
        return ncLineCount;
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Use FullFileInfo instead");
    }

    public int hashCode() {
        return getPackagePath().hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BaseFileInfo that = (BaseFileInfo) o;
        return this.getPackagePath().equals(that.getPackagePath());
    }

    @Override
    public int getDataIndex() {
        throw new UnsupportedOperationException("Use FullFileInfo instead");
    }

    @Override
    public int getDataLength() {
        throw new UnsupportedOperationException("Use FullFileInfo instead");
    }

    @Override
    public int getStartLine() {
        return 1;
    }

    @Override
    public int getStartColumn() {
        return 1;
    }

    @Override
    public int getEndLine() {
        return lineCount;
    }

    @Override
    public int getEndColumn() {
        return 1;  // TODO hack, not a true end
    }

    /**
     * Visit yourself
     *
     * @param entityVisitor callback
     */
    @Override
    public void visit(EntityVisitor entityVisitor) {
        entityVisitor.visitFile(this);
    }

}