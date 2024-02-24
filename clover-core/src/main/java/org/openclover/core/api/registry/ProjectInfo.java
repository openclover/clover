package org.openclover.core.api.registry;

public interface ProjectInfo extends EntityContainer, HasPackages, HasContextFilter, HasMetrics {

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

    PackageInfo getNamedPackage(String name);

    List<ClassInfo> getClasses(HasMetricsFilter filter);

    List<PackageInfo> getPackages(HasMetricsFilter filter);
}
