package org.openclover.core.api.registry;

import org.openclover.core.registry.entities.PackageFragment;
import org.openclover.core.util.Path;

import java.util.List;

public interface ProjectInfo
        extends EntityContainer, HasPackages, HasContextFilter, HasMetrics,
        HasVersions, CoverageDataReceptor, IsCacheable {

    void addPackage(PackageInfo pkg);

    ProjectInfo copy();

    ProjectInfo copy(HasMetricsFilter filter);

    ProjectInfo copy(HasMetricsFilter filter, ContextSet contextFilter);

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

    PackageFragment findPackageFragment(String packageName);

    List<ClassInfo> getClasses(HasMetricsFilter filter);

    List<FileInfo> getFiles(HasMetricsFilter filter);

    /**
     * Returns name of the project
     *
     * @return String project name or <code>null</code>
     */
    @Override
    String getName();

    PackageInfo getNamedPackage(String name);

    List<PackageInfo> getPackages(HasMetricsFilter filter);

    PackageFragment[] getPackageRoots();

    boolean hasTestResults();

    /**
     * Returns true if project is empty.
     *
     * @return boolean - true if getAllPackages() is empty
     */
    boolean isEmpty();

    void resolve(Path sourcePath);

    void setContextFilter(ContextSet filter);

    void setDataLength(int length);

    void setHasTestResults(boolean hasTestResults);

    void setName(String name);

    void visitFiles(FileInfoVisitor visitor);
}
