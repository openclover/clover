package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.context.ContextSet;
import org.openclover.core.registry.CachingInfo;
import org.openclover.core.registry.FileInfoVisitor;
import org.openclover.core.registry.metrics.HasMetricsFilter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openclover.core.util.Lists.newArrayList;

public class BaseProjectInfo implements ProjectInfo, CachingInfo {
    protected String name;
    protected Map<String, BasePackageInfo> packages;
    protected long version;

    protected Map<String, BaseClassInfo> classLookup;
    protected Map<String, BaseFileInfo> fileLookup;
    protected BlockMetrics rawMetrics;
    protected BlockMetrics metrics;
    protected ContextSet contextFilter;

    public BaseProjectInfo(String name, long version) {
        this.name = name;
        this.version = version;
        this.packages = new LinkedHashMap<>();
    }

    public BaseProjectInfo(String name) {
        this(name, System.currentTimeMillis());
    }

    @Override
    @NotNull
    public List<? extends PackageInfo> getAllPackages() {
        return newArrayList(packages.values());
    }

    public void addPackage(BasePackageInfo pkg) {
        packages.put(pkg.getName(), pkg);
    }

    public BasePackageInfo getDefaultPackage() {
        return packages.get(PackageInfo.DEFAULT_PACKAGE_NAME);
    }

    public BasePackageInfo getNamedPackage(String name) {
        if (name == null || name.length() == 0 || PackageInfo.DEFAULT_PACKAGE_NAME.equals(name)) {
            return getDefaultPackage();
        }
        return packages.get(name);
    }

    @Override
    public PackageInfo findPackage(String name) {
        throw new UnsupportedOperationException("Use FullProjectInfo");
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isEmpty() {
        return packages.isEmpty();
    }

    /**
     * convenience method to find a class in a project using its fully qualified name. Initialized lazily, so
     * the first call may be slow
     * @param fqcn a fully qualified class name
     * @return corresponding BaseClassInfo or null if not found
     */
    @Override
    public ClassInfo findClass(String fqcn) {
        if (classLookup == null) {
            buildClassLookupMap();
        }
        return classLookup.get(fqcn);
    }

    /**
     * convenience method to find a file in a project using its package path.  Initialized lazily, so
     * the first call may be slow
     * @param pkgPath - path of the file to look for
     * @return corresponding BaseFileInfo or null if not found
     */
    @Override
    public FileInfo findFile(String pkgPath) {
        if (fileLookup == null) {
            buildFileLookupMap();
        }
        return fileLookup.get(pkgPath);
    }

    /**
     * convenience method to get all classes in a project that meet some criteria
     * @param filter filter to apply
     * @return list of classes that match filter
     */
    public List<? extends BaseClassInfo> getClasses(HasMetricsFilter filter) {
        if (classLookup == null) {
            buildClassLookupMap();
        }
        List<BaseClassInfo> result = newArrayList();
        for (BaseClassInfo classInfo : classLookup.values()) {
            if (filter.accept(classInfo)) {
                result.add(classInfo);
            }
        }
        return result;
    }

    /**
     * convenience method to get all filees in a project that meet some criteria
     * @param filter filter to apply
     * @return list of files that match filter
     */
    public List<BaseFileInfo> getFiles(HasMetricsFilter filter) {
        if (fileLookup == null) {
            buildFileLookupMap();
        }
        List<BaseFileInfo> result = newArrayList();
        for (BaseFileInfo fileInfo : fileLookup.values()) {
            if (filter.accept(fileInfo)) {
                result.add(fileInfo);
            }
        }
        return result;
    }


    public List<BasePackageInfo> getPackages(HasMetricsFilter filter) {
        List<BasePackageInfo> result = newArrayList();
        for (BasePackageInfo packageInfo : packages.values()) {
            if (filter.accept(packageInfo)) {
                result.add(packageInfo);
            }
        }
        return result;
    }

    private void buildClassLookupMap() {
        final Map<String, BaseClassInfo> tmpClassLookup = new LinkedHashMap<>();
        visitFiles(file -> {
            for (ClassInfo aClass : file.getClasses()) {
                BaseClassInfo info = (BaseClassInfo) aClass;
                tmpClassLookup.put(info.getQualifiedName(), info);
            }
        });
        classLookup = tmpClassLookup;
    }

    private void buildFileLookupMap() {
        final Map<String, BaseFileInfo> tmpFileLookup = new LinkedHashMap<>();
        visitFiles(file -> tmpFileLookup.put(file.getPackagePath(), file));
        fileLookup = tmpFileLookup;
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

    @Override
    public ContextSet getContextFilter() {
        return contextFilter;
    }

    public void setContextFilter(ContextSet filter) {
        contextFilter = filter;
        metrics = null;
    }

    /**
     * Visit yourself
     *
     * @param entityVisitor callback
     */
    @Override
    public void visit(EntityVisitor entityVisitor) {
        entityVisitor.visitProject(this);
    }

    public void visitFiles(FileInfoVisitor visitor) {
        for (BasePackageInfo pkgInfo : packages.values()) {
            pkgInfo.visitFiles(visitor);
        }
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(final long version) {
        this.version = version;
    }

    @Override
    public void invalidateCaches() {
        classLookup = null;
        fileLookup = null;
        rawMetrics = null;
        metrics = null;
    }
}
