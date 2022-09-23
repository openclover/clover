package com.atlassian.clover.api.registry;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Implemented interfaces:
 *
 * <ul>
 *     <li>EntityContainer - package can contain files</li>
 *     <li>HasClasses - classes declared in source files inside this package</li>
 *     <li>HasFiles - list of source files belonging to a package</li>
 *     <li>HasContextFilter - context filter applied to a package</li>
 *     <li>HasMetrics - code metrics for a package</li>
 *     <li>HasParent - parent project for this package</li>
 * </ul>
 */
public interface PackageInfo extends EntityContainer, HasClasses, HasFiles, HasContextFilter, HasMetrics, HasParent {
    String DEFAULT_PACKAGE_NAME = "default-pkg";

    /**
     * Returns a project containing this package {@link EntityVisitor#visitProject(ProjectInfo)}.
     * @return parent
     */
    @Override
    EntityContainer getParent();

    /**
     * Returns a project containing this package.
     * @return parent {@link ProjectInfo}
     */
    ProjectInfo getContainingProject();

    @Override
    String getName();

    String getPath();

    boolean isDefault();

    boolean isEmpty();

    /**
     * Returns list of source files belonging to this package namespace.
     *
     * @return List&lt;? extends FileInfo&gt; list of files or empty list if none
     */
    @Override
    @NotNull
    List<? extends FileInfo> getFiles();

    /**
     * Returns a list of top-level classes (i.e. declared on a file's top-level, not as inner classes or inline ones)
     * declared in this package. It does not return classes from sub-packages.
     * <p/>
     * Note that in many programming languages you can have more than one class in a source file. It can also happen
     * that some source file has no classes.
     *
     * @return List&lt;? extends ClassInfo&gt; list of classes or empty list if none
     */
    @Override
    @NotNull
    List<? extends ClassInfo> getClasses();

    /**
     * Returns a list of top-level classes (i.e. not inner or inline ones) declared in this package AND all
     * sub-packages.
     * <p/>
     * For example, if this package is named "com.acme" then it will return all top-level classes
     * from "com.acme" as well as from "com.acme.foo", "com.acme.foo.bar" but not "com.other".
     *
     * @return List&lt;? extends ClassInfo&gt; list of classes or empty list if none
     */
    @NotNull
    List<? extends ClassInfo> getClassesIncludingSubPackages();

    /**
     * Returns list of all classes (including inner or inline classes) declared in this package.
     *
     * @return List&lt;? extends ClassInfo&gt; - list of classes or empty list if none
     */
    @Override
    @NotNull
    List<? extends ClassInfo> getAllClasses();

    /**
     * Returns list of all classes (including inner or inline classes) declared in this package AND all sub-packages.
     * <p/>
     * For example if this package is named "com.acme" then it will return all top-level and inner classes
     * from "com.acme" as well as from "com.acme.foo", "com.acme.foo.bar" but not from "com.other".
     *
     * @return List&lt;? extends ClassInfo&gt; - list of classes or empty list if none
     */
    @NotNull
    List<? extends ClassInfo> getAllClassesIncludingSubPackages();

    boolean isDescendantOf(PackageInfo other);
}
