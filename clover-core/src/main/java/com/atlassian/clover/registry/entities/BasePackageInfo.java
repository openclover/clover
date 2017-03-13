package com.atlassian.clover.registry.entities;

import clover.com.google.common.collect.Lists;
import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.ContextSet;
import com.atlassian.clover.api.registry.EntityContainer;
import com.atlassian.clover.api.registry.EntityVisitor;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.api.registry.ProjectInfo;
import com.atlassian.clover.registry.CachingInfo;
import com.atlassian.clover.registry.FileInfoVisitor;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.util.CloverUtils;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static clover.com.google.common.collect.Lists.newArrayList;

public class BasePackageInfo implements PackageInfo, CachingInfo {

    protected final String name;
    protected final boolean defaultPkg;
    protected final Map<String, FileInfo> files = new LinkedHashMap<String, FileInfo>();
    protected final String path;

    protected BaseProjectInfo containingProject;
    protected BlockMetrics rawMetrics;
    protected BlockMetrics metrics;
    protected ContextSet contextFilter;

    /** Top-level classes in this package */
    protected List<? extends ClassInfo> classes;

    /** All classes in this package */
    protected List<? extends ClassInfo> allClasses;

    public BasePackageInfo(BaseProjectInfo containingProject, String name) {
        this.containingProject = containingProject;
        this.defaultPkg = isDefaultName(name);
        if (this.defaultPkg) {
            this.name = com.atlassian.clover.api.registry.PackageInfo.DEFAULT_PACKAGE_NAME;
        }
        else {
            this.name = name;
        }
        this.path = CloverUtils.packageNameToPath(name, defaultPkg);
    }

    public static boolean isDefaultName(String name) {
        return (name.length() == 0 || name.equals(PackageInfo.DEFAULT_PACKAGE_NAME));
    }

    /**
     * Return parent project
     *
     * @return container with {@link ProjectInfo}
     */
    @Override
    public EntityContainer getParent() {
        return new EntityContainer() {
            @Override
            public void visit(EntityVisitor entityVisitor) {
                entityVisitor.visitProject(containingProject);
            }
        };
    }

    /**
     * Visit yourself
     *
     * @param entityVisitor callback
     */
    @Override
    public void visit(EntityVisitor entityVisitor) {
        entityVisitor.visitPackage(this);
    }

    @Override
    public ProjectInfo getContainingProject() {
        return containingProject;
    }

    @Override
    public ContextSet getContextFilter() {
        return getContainingProject().getContextFilter();
    }

    @Override
    public String getName() {
        return name;
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
    public List<? extends FileInfo> getFiles() {
       return newArrayList(files.values());
    }

    public void addFile(BaseFileInfo file) {
        file.setContainingPackage(this);
        files.put(file.getPackagePath(), file);
    }

    public BaseFileInfo getFile(String packagePath) {
        return (BaseFileInfo)files.get(packagePath);
    }

    public BaseFileInfo getFileInPackage(String name) {
        return getFile(getPath() + name);
    }

    /**
     * Return list of top-level classes declared in this package namespace.
     *
     * @return List&lt;? extends ClassInfo&gt; list of classes found or empty list
     */
    @Override
    @NotNull
    public List<? extends ClassInfo> getClasses() {
        if (classes == null) {
            gatherClassesFromPackage();
        }
        return newArrayList(classes);
    }

    /**
     * Return list of top-level classes declared in this package namespace as well as from sub-packages.
     *
     * @return List&lt;? extends ClassInfo&gt; list of classes found or empty list
     */
    @Override
    @NotNull
    public List<? extends ClassInfo> getClassesIncludingSubPackages() {
        final List<? extends PackageInfo> packages = getContainingProject().getAllPackages();
        final List<ClassInfo> classes = Lists.newLinkedList(getClasses()); // gather classes from this package

        for (final PackageInfo aPackage : packages) {
            final BasePackageInfo otherPkg = (BasePackageInfo) aPackage;
            if (otherPkg.isChildOrDescendantOf(this)) {
                classes.addAll(otherPkg.getClasses());  // gather classes from other subpackages too
            }
        }

        return classes;
    }

    /**
     * Return list of top-level classes declared in this package matching the filter
     *
     * @param filter metrics filter to be applied
     * @return List&lt;? extends ClassInfo&gt; list of classes found or empty list
     */
    public List<? extends ClassInfo> getClasses(final HasMetricsFilter filter) {
        final List<ClassInfo> filteredClasses = newArrayList();
        final List<? extends ClassInfo> allClasses = getClasses();
        for (final ClassInfo aClass : allClasses) {
            if (filter.accept(aClass)) {
                filteredClasses.add(aClass);
            }
        }
        return filteredClasses;
    }

    @Override
    @NotNull
    public List<? extends ClassInfo> getAllClasses() {
        if (allClasses == null) {
            gatherAllClassesFromPackage();
        }
        return newArrayList(allClasses);
    }

    @Override
    @NotNull
    public List<? extends ClassInfo> getAllClassesIncludingSubPackages() {
        final List<? extends PackageInfo> packages = getContainingProject().getAllPackages();
        final List<ClassInfo> classes = Lists.newLinkedList(getAllClasses()); // gather all classes from this package

        for (final PackageInfo aPackage : packages) {
            final BasePackageInfo otherPkg = (BasePackageInfo) aPackage;
            if (otherPkg.isChildOrDescendantOf(this)) {
                classes.addAll(otherPkg.getAllClasses());  // gather all classes from other subpackages
            }
        }

        return classes;
    }

    @Override
    public boolean isDescendantOf(PackageInfo other) {
        throw new UnsupportedOperationException("Use FullPackageInfo");
    }

    @Override
    public BlockMetrics getRawMetrics() {
        return rawMetrics;
    }

    @Override
    public BlockMetrics getMetrics() {
        return metrics;
    }

    @Override
    public void setMetrics(BlockMetrics metrics) {
        this.metrics = metrics;
    }

    public void visitFiles(final FileInfoVisitor visitor) {
        for (final FileInfo fileInfo : files.values()) {
            visitor.visitFileInfo((BaseFileInfo) fileInfo);
        }
    }

    /**
     * @return true is this package is a child or descendant of the other package. Default packages
     * are considered the parent or ancestor of all packages.
     */
    public boolean isChildOrDescendantOf(final BasePackageInfo other) {
        return containingProject == other.containingProject
                && (this != other)    // This is never a child of itself
                && (other.isDefault() // If other is default then all packages
                                      // "com.foo.bar" contains "com.foo." implies com.foo.bar is a child of com.foo
                        || (getName().indexOf(other.getName() + ".") == 0));
    }

    public boolean isNamed(String name) {
        return
            (isDefault() && isDefaultName(name))
            || this.name.equals(name);
    }

    @Override
    public void invalidateCaches() {
        classes = null;
        rawMetrics = null;
        metrics = null;
    }

    protected void gatherClassesFromPackage() {
        final List<ClassInfo> topLevelClasses = newArrayList();
        for (final FileInfo file : files.values()) {
            topLevelClasses.addAll(file.getClasses());
        }
        classes = topLevelClasses;
    }

    protected void gatherAllClassesFromPackage() {
        final List<ClassInfo> tmpClasses = newArrayList();
        for (final FileInfo fileInfo : files.values()) {
            tmpClasses.addAll(fileInfo.getAllClasses());
        }
        allClasses = tmpClasses;
    }

}
