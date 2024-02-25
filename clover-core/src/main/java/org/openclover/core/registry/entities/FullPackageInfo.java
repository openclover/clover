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

import static org.openclover.core.api.registry.PackageInfo.isDefaultName;
import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Lists.newLinkedList;


public class FullPackageInfo
        implements PackageInfo, HasMetricsNode, CoverageDataReceptor, CachingInfo,
                EditableInstrumentationInfo {

    private final String name;
    private final boolean defaultPkg;
    private final Map<String, FileInfo> files = new LinkedHashMap<>();
    private final String path;

    private ProjectInfo containingProject;
    private BlockMetrics rawMetrics;
    private BlockMetrics metrics;
    private ContextSet contextFilter;

    private int dataIndex;
    private int dataLength;

    private Comparator<HasMetrics> orderby;
    private CoverageDataProvider data;

    /**
     * Top-level classes in this package
     */
    private List<ClassInfo> classes;

    /**
     * All classes in this package
     */
    private List<ClassInfo> allClasses;

    public static PackageInfo createEmptyFromTemplate(PackageInfo info) {
        return new FullPackageInfo(null, info.getName(), 0);
    }

    public FullPackageInfo(ProjectInfo containingProject, String pkg, int dataIndex) {
        this(containingProject, pkg);
        this.dataIndex = dataIndex;
    }

    public FullPackageInfo(ProjectInfo containingProject, String name) {
        this.containingProject = containingProject;
        this.defaultPkg = isDefaultName(name);
        if (this.defaultPkg) {
            this.name = PackageInfo.DEFAULT_PACKAGE_NAME;
        } else {
            this.name = name;
        }
        this.path = CloverUtils.packageNameToPath(name, defaultPkg);
    }

    // PackageInfo

    @Override
    public void addFile(FileInfo file) {
        file.setContainingPackage(this);
        files.put(file.getPackagePath(), file);
    }

    @Override
    public ProjectInfo getContainingProject() {
        return containingProject;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isDefault() {
        return defaultPkg;
    }

    @Override
    public boolean isEmpty() {
        return files.isEmpty();
    }

    @Override
    @NotNull
    public List<ClassInfo> getClasses() {
        if (classes == null) {
            gatherClassesFromPackage();
        }
        return newArrayList(classes);
    }

    @Override
    public List<ClassInfo> getClasses(final HasMetricsFilter filter) {
        final List<ClassInfo> filteredClasses = newArrayList();
        final List<ClassInfo> allClasses = getClasses();
        for (final ClassInfo aClass : allClasses) {
            if (filter.accept(aClass)) {
                filteredClasses.add(aClass);
            }
        }
        return filteredClasses;
    }

    /**
     * Return list of top-level classes declared in this package namespace as well as from sub-packages.
     *
     * @return List&lt;ClassInfo&gt; list of classes found or empty list
     */
    @Override
    @NotNull
    public List<ClassInfo> getClassesIncludingSubPackages() {
        final List<PackageInfo> packages = getContainingProject().getAllPackages();
        final List<ClassInfo> classes = newLinkedList(getClasses()); // gather classes from this package

        for (final PackageInfo otherPkg : packages) {
            if (otherPkg.isChildOrDescendantOf(this)) {
                classes.addAll(otherPkg.getClasses());  // gather classes from other subpackages too
            }
        }

        return classes;
    }

    @Override
    @NotNull
    public List<ClassInfo> getAllClasses() {
        if (allClasses == null) {
            gatherAllClassesFromPackage();
        }
        return newArrayList(allClasses);
    }

    @Override
    @NotNull
    public List<ClassInfo> getAllClassesIncludingSubPackages() {
        final List<PackageInfo> packages = getContainingProject().getAllPackages();
        final List<ClassInfo> classes = newLinkedList(getAllClasses()); // gather all classes from this package

        for (final PackageInfo otherPkg : packages) {
            if (otherPkg.isChildOrDescendantOf(this)) {
                classes.addAll(otherPkg.getAllClasses());  // gather all classes from other subpackages
            }
        }

        return classes;
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

    /**
     * @return true is this package is a child or descendant of the other package. Default packages
     * are considered the parent or ancestor of all packages.
     */
    public boolean isChildOrDescendantOf(final PackageInfo other) {
        return containingProject == other.getContainingProject()
                && (this != other)    // This is never a child of itself
                && (other.isDefault() // If other is default then all packages
                // "com.foo.bar" contains "com.foo." implies com.foo.bar is a child of com.foo
                || (getName().indexOf(other.getName() + ".") == 0));
    }

    @Override
    public FileInfo getFileInPackage(String name) {
        return getFile(getPath() + name);
    }

    // CachingInfo

    @Override
    public void invalidateCaches() {
        classes = null;
        rawMetrics = null;
        metrics = null;
    }

    // CoverageDataRange

    @Override
    public int getDataIndex() {
        return dataIndex;
    }

    @Override
    public int getDataLength() {
        return dataLength;
    }

    // CoverageDataReceptor

    @Override
    public void setDataProvider(final CoverageDataProvider data) {
        this.data = data;
        for (final FileInfo file : files.values()) {
            file.setDataProvider(data);
        }
        rawMetrics = null;
        metrics = null;
    }

    @Override
    public CoverageDataProvider getDataProvider() {
        return data;
    }

    // EditableInstrumentationInfo
    @Override
    public void setDataIndex(int index) {
        dataIndex = index;
    }

    @Override
    public void setDataLength(int length) {
        dataLength = length;
    }

    // EntityContainer

    /**
     * Visit yourself
     *
     * @param entityVisitor callback
     */
    @Override
    public void visit(EntityVisitor entityVisitor) {
        entityVisitor.visitPackage(this);
    }

    // HasClasses

    // HasContextFilter

    @Override
    public ContextSet getContextFilter() {
        return getContainingProject().getContextFilter();
    }

    // HasFiles

    @Override
    @NotNull
    public List<FileInfo> getFiles() {
        return newArrayList(files.values());
    }

    @Override
    public void visitFiles(final FileInfoVisitor visitor) {
        for (final FileInfo fileInfo : files.values()) {
            visitor.visitFileInfo(fileInfo);
        }
    }

    // HasMetrics

    @Override
    public String getName() {
        return name;
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
    public void setMetrics(BlockMetrics metrics) {
        this.metrics = metrics;
    }

    // HasMetricsNode

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
        return (HasMetricsNode) classes.get(i);
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

    // HasParent

    /**
     * Return parent project
     *
     * @return container with {@link ProjectInfo}
     */
    @Override
    public EntityContainer getParent() {
        return entityVisitor -> entityVisitor.visitProject(containingProject);
    }

    // IsMetricsComparable

    @Override
    public void setComparator(final Comparator<HasMetrics> cmp) {
        orderby = cmp;
        classes = null;
        for (final FileInfo fileInfo : files.values()) {
            fileInfo.setComparator(cmp); // note - comparator is passed to classes via containing files
        }
    }

    // OTHER

    @Override
    public FileInfo getFile(String packagePath) {
        return files.get(packagePath);
    }


    // note - these methods skip the "file" level of the hierarchy

    protected void gatherClassesFromPackage() {
        final List<ClassInfo> topLevelClasses = newArrayList();
        for (final FileInfo file : files.values()) {
            topLevelClasses.addAll(file.getClasses());
        }
        classes = topLevelClasses;

        if (orderby != null) {
            classes.sort(orderby);
        }
    }

    private PackageMetrics calcMetrics(boolean filter) {
        PackageMetrics packageMetrics = new PackageMetrics(this);
        int numFiles = 0;
        for (FileInfo fileInfo : files.values()) {
            if (!filter) {
                packageMetrics.add((FileMetrics) fileInfo.getRawMetrics());
            } else {
                packageMetrics.add((FileMetrics) fileInfo.getMetrics());
            }
            numFiles++;
        }
        packageMetrics.setNumFiles(numFiles);
        return packageMetrics;
    }

    @Override
    public PackageInfo copy(ProjectInfo proj, HasMetricsFilter filter) {
        PackageInfo pkg = new FullPackageInfo(proj, name, dataIndex);
        pkg.setDataProvider(getDataProvider());
        for (FileInfo fileInfo : files.values()) {
            if (filter.accept(fileInfo)) {
                FileInfo info = fileInfo.copy(pkg, filter);
                if (!info.isEmpty()) {
                    pkg.addFile(info);
                }
            }
        }
        pkg.setDataLength(getDataLength());
        return pkg;
    }

    @Override
    public boolean isNamed(String name) {
        return (isDefault() && isDefaultName(name))
                || this.name.equals(name);
    }

    protected void gatherAllClassesFromPackage() {
        final List<ClassInfo> tmpClasses = newArrayList();
        for (final FileInfo fileInfo : files.values()) {
            tmpClasses.addAll(fileInfo.getAllClasses());
        }
        allClasses = tmpClasses;
    }

    @Override
    public String toString() {
        return "FullPackageInfo{" +
                "name='" + name + '\'' +
                ", defaultPkg=" + defaultPkg +
                ", path='" + path + '\'' +
                '}';
    }
}
