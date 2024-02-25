package org.openclover.core.api.registry;

import org.openclover.core.registry.CachingInfo;
import org.openclover.core.registry.entities.PackageFragment;
import org.openclover.core.util.Path;

import java.util.List;

public interface ProjectInfo
        extends EntityContainer, HasPackages, HasContextFilter, HasMetrics,
        HasVersions, CoverageDataReceptor, CachingInfo {

    /**
     * Returns name of the project
     *
     * @return String project name or <code>null</code>
     */
    @Override
    String getName();

    /**
     * Returns true if project is empty.
     *
     * @return boolean - true if getAllPackages() is empty
     */
    boolean isEmpty();

    /**
     * Searches and returns a class having the specified fully qualified name
     *
     * @param fullyQualifiedName a class name with its package
     * @return ClassInfo specified class or <code>null</code> if not found
     */
    ClassInfo findClass(String fullyQualifiedName);

    /**
     * Searches and returns a file at the specified relative path. For example: <code>com/acme/Foo.java</code>
     *
     * @param pkgPath file name prefixed by a package path
     * @return FileInfo specified file or <code>null</code> if not found
     */
    FileInfo findFile(String pkgPath);

    void setName(String name);

    void addPackage(PackageInfo pkg);

    PackageInfo getNamedPackage(String name);

    List<ClassInfo> getClasses(HasMetricsFilter filter);

    List<FileInfo> getFiles(HasMetricsFilter filter);

    List<PackageInfo> getPackages(HasMetricsFilter filter);

    void setContextFilter(ContextSet filter);

    void visitFiles(FileInfoVisitor visitor);

    PackageFragment[] getPackageRoots();

    ProjectInfo copy();

    ProjectInfo copy(HasMetricsFilter filter);

    ProjectInfo copy(HasMetricsFilter filter, ContextSet contextFilter);

    void resolve(Path sourcePath);

    void setDataLength(int length);

    void buildCaches();

    boolean hasTestResults();

    void setHasTestResults(boolean hasTestResults);

    PackageFragment findPackageFragment(String packageName);
}
