package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.EntityContainer;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.FileInfoVisitor;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.metrics.HasMetricsNode;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DummyPackageInfo implements PackageInfo {
    @Override
    public int getDataIndex() {
        return 0;
    }

    @Override
    public int getDataLength() {
        return 0;
    }

    @Override
    public void setDataIndex(int index) {

    }

    @Override
    public void setDataLength(int length) {

    }

    @Override
    public void setDataProvider(CoverageDataProvider data) {

    }

    @Override
    public CoverageDataProvider getDataProvider() {
        return null;
    }

    @Override
    public void visit(EntityVisitor entityVisitor) {

    }

    @Override
    public ContextSet getContextFilter() {
        return null;
    }

    @Override
    public void visitFiles(FileInfoVisitor visitor) {

    }

    @Override
    public BlockMetrics getMetrics() {
        return null;
    }

    @Override
    public BlockMetrics getRawMetrics() {
        return null;
    }

    @Override
    public void setMetrics(BlockMetrics metrics) {

    }

    @Override
    public void setComparator(Comparator<HasMetrics> cmp) {

    }

    @Override
    public EntityContainer getParent() {
        return null;
    }

    @Override
    public ProjectInfo getContainingProject() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public String getChildType() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int getNumChildren() {
        return 0;
    }

    @Override
    public HasMetricsNode getChild(int i) {
        return null;
    }

    @Override
    public int getIndexOfChild(HasMetricsNode child) {
        return 0;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @NotNull
    @Override
    public List<FileInfo> getFiles() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<ClassInfo> getClasses() {
        return Collections.emptyList();
    }

    @Override
    public List<ClassInfo> getClasses(HasMetricsFilter filter) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<ClassInfo> getClassesIncludingSubPackages() {
        return null;
    }

    @NotNull
    @Override
    public List<ClassInfo> getAllClasses() {
        return null;
    }

    @NotNull
    @Override
    public List<ClassInfo> getAllClassesIncludingSubPackages() {
        return null;
    }

    @Override
    public boolean isDescendantOf(PackageInfo other) {
        return false;
    }

    @Override
    public boolean isChildOrDescendantOf(PackageInfo other) {
        return false;
    }

    @Override
    public void addFile(FileInfo file) {

    }

    @Override
    public FileInfo getFileInPackage(String name) {
        return null;
    }

    @Override
    public FileInfo getFile(String packagePath) {
        return null;
    }

    @Override
    public PackageInfo copy(ProjectInfo proj, HasMetricsFilter filter) {
        return null;
    }

    @Override
    public boolean isNamed(String name) {
        return false;
    }

    @Override
    public void invalidateCaches() {

    }
}
