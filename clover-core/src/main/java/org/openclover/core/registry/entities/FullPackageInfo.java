package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.EditableInstrumentationInfo;
import org.openclover.core.api.registry.EntityContainer;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.CachingInfo;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.CoverageDataReceptor;
import org.openclover.core.api.registry.FileInfoVisitor;
import org.openclover.core.registry.metrics.FileMetrics;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.registry.metrics.HasMetricsNode;
import org.openclover.core.registry.metrics.PackageMetrics;
import org.openclover.core.util.CloverUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


public class FullPackageInfo extends BasePackageInfo implements HasMetricsNode, CoverageDataReceptor, PackageInfo {
    private int dataIndex;
    private int dataLength;

    private Comparator<HasMetrics> orderby;
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
    public void setComparator(final Comparator<HasMetrics> cmp) {
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
            classes.sort(orderby);
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
    public boolean isDescendantOf(PackageInfo other) {
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
