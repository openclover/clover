package com.atlassian.clover.registry.entities;

import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.EntityVisitor;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.ProjectInfo;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.CoverageDataReceptor;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.registry.metrics.HasMetricsNode;
import com.atlassian.clover.registry.metrics.FileMetrics;
import com.atlassian.clover.registry.metrics.PackageMetrics;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;


public class FullPackageInfo extends BasePackageInfo implements HasMetricsNode, CoverageDataReceptor, com.atlassian.clover.api.registry.PackageInfo {
    private int dataIndex;
    private int dataLength;

    private Comparator orderby;
    private CoverageDataProvider data;

    public FullPackageInfo(FullProjectInfo containingProject, String pkg, int dataIndex) {
        super(containingProject, pkg);
        this.dataIndex = dataIndex;
    }

    public static FullPackageInfo createEmptyFromTemplate(FullPackageInfo info) {
        return new FullPackageInfo(null, info.getName(), 0);
    }

    @Override
    public void setDataProvider(final CoverageDataProvider data) {
        this.data = data;
        for (final FileInfo file : files.values()) {
            FullFileInfo fullFileInfo = (FullFileInfo) file;
            fullFileInfo.setDataProvider(data);
        }
        rawMetrics = null;
        metrics = null;
    }

    @Override
    public CoverageDataProvider getDataProvider() {
        return data;
    }

    @Override
    public int getDataIndex() {
        return dataIndex;
    }

    @Override
    public int getDataLength() {
        return dataLength;
    }

    public void setDataIndex(int index) {
        dataIndex = index;
    }

    public void setDataLength(int length) {
        dataLength = length;
    }

    // note - these methods skip the "file" level of the hierarchy

    @Override
    public String getChildType() {
        return "class";
    }

    @Override
    public int getNumChildren() {
        if (classes == null) {
            gatherClassesFromPackage();
        }
        return classes.size();
    }

    @Override
    public HasMetricsNode getChild(int i) {
        if (classes == null) {
            gatherClassesFromPackage();
        }
        return (HasMetricsNode)classes.get(i);
    }

    @Override
    public int getIndexOfChild(HasMetricsNode child) {
        if (classes == null) {
            gatherClassesFromPackage();
        }
        return classes.indexOf(child);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void setComparator(final Comparator cmp) {
        orderby = cmp;
        classes = null;
        for (final FileInfo fileInfo : files.values()) {
            final FullFileInfo fullFileInfo = (FullFileInfo) fileInfo;
            fullFileInfo.setComparator(cmp); // note - comparator is passed to classes via containing files
        }
    }

    @Override
    public BlockMetrics getMetrics() {
        if (metrics == null || getContainingProject().getContextFilter() != contextFilter) {
            contextFilter = getContainingProject().getContextFilter();
            metrics = calcMetrics(true);
        }
        return metrics;
    }

    @Override
    public BlockMetrics getRawMetrics() {
        if (rawMetrics == null) {
            rawMetrics = calcMetrics(false);
        }
        return rawMetrics;
    }

    @Override
    protected void gatherClassesFromPackage() {
        super.gatherClassesFromPackage();
        if (orderby != null) {
            Collections.sort(classes, orderby);
        }
    }

    private PackageMetrics calcMetrics(boolean filter) {
        PackageMetrics packageMetrics = new PackageMetrics(this);
        int numFiles = 0;
        for (FileInfo fileInfo : files.values()) {
            FullFileInfo fullFileInfo = (FullFileInfo) fileInfo;
            if (!filter) {
                packageMetrics.add((FileMetrics) fullFileInfo.getRawMetrics());
            } else {
                packageMetrics.add((FileMetrics) fullFileInfo.getMetrics());
            }
            numFiles++;
        }
        packageMetrics.setNumFiles(numFiles);
        return packageMetrics;
    }

    public FullPackageInfo copy(FullProjectInfo proj, HasMetricsFilter filter) {
        FullPackageInfo pkg = new FullPackageInfo(proj, name, dataIndex);
        pkg.setDataProvider(getDataProvider());
        for (FileInfo fileInfo : files.values()) {
            FullFileInfo fullFileInfo = (FullFileInfo) fileInfo;
            if (filter.accept(fullFileInfo)) {
                FullFileInfo info = fullFileInfo.copy(pkg, filter);
                if (!info.isEmpty()) {
                    pkg.addFile(info);
                }
            }
        }
        pkg.setDataLength(getDataLength());
        return pkg;
    }

    @Override
    public String toString() {
        return "FullPackageInfo{" +
            "name='" + name + '\'' +
            ", defaultPkg=" + defaultPkg +
            ", path='" + path + '\'' +
            '}';
    }

    @Override
    public boolean isDescendantOf(com.atlassian.clover.api.registry.PackageInfo other) {
        // fetch parent project from the other package
        final AtomicReference<ProjectInfo> otherContainingProject = new AtomicReference<>();
        other.getParent().visit(new EntityVisitor() {
            @Override
            public void visitProject(ProjectInfo parentProject) {
                otherContainingProject.set(parentProject);
            }
        });

        // compare it with parent project in this package and next check common prefix
        return containingProject == otherContainingProject.get()
                && (this != other)        //This is never a child of itself&&
                && (other.isDefault()     //If other is default then all packages
                                          //"com.foo.bar" contains "com.foo." implies com.foo.bar is a child of com.foo
                        || (getName().indexOf(other.getName() + ".") == 0));
    }
}
