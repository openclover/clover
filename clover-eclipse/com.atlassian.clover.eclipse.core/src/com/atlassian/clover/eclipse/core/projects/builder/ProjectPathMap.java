package com.atlassian.clover.eclipse.core.projects.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Maps.newHashMap;
import static clover.com.google.common.collect.Sets.newLinkedHashSet;

public class ProjectPathMap {
    protected IPath projectLocation;
    protected IPath defaultOutputLocation;
    protected IClasspathEntry[] allClasspathEntries;
    protected IClasspathEntry[] sourcePathEntries;
    protected Map<IClasspathEntry, IPath> sourcePathsToOutputPaths;

    protected ProjectPathMap() {}

    public ProjectPathMap(IJavaProject project) throws CoreException {
        this.projectLocation = project.getProject().getFullPath();
        this.defaultOutputLocation = project.getJavaProject().getOutputLocation();
        this.allClasspathEntries = project.getJavaProject().getRawClasspath();
        this.sourcePathsToOutputPaths = newHashMap();
        List<IClasspathEntry> sourcepathEntries = newArrayList();
        for (IClasspathEntry entry : allClasspathEntries) {
            if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                sourcepathEntries.add(entry);
                sourcePathsToOutputPaths.put(
                        entry,
                        entry.getOutputLocation() == null ? this.defaultOutputLocation : entry.getOutputLocation());
            }
        }
        this.sourcePathEntries = sourcepathEntries.toArray(new IClasspathEntry[sourcepathEntries.size()]);
    }

    public IPath getDefaultOutputLocation() {
        return defaultOutputLocation;
    }

    public IPath[] getAllOutputLocations() {
        List<IPath> paths = newArrayList();
        if (defaultOutputLocation != null) {
            paths.add(defaultOutputLocation);
        }
        for (IClasspathEntry sourcePathEntry : sourcePathEntries) {
            if (sourcePathEntry.getOutputLocation() != null) {
                paths.add(sourcePathEntry.getOutputLocation());
            }
        }
        return paths.toArray(new IPath[paths.size()]);
    }

    public IPath[] getSourcePaths() {
        List<IPath> paths = newArrayList();
        for (IClasspathEntry sourcePathEntry : sourcePathEntries) {
            if (sourcePathEntry.getPath() != null) {
                paths.add(sourcePathEntry.getPath());
            }
        }
        return paths.toArray(new IPath[paths.size()]);
    }

    public Set getOutputLocations() {
        Set<String> libraries = new LinkedHashSet<>();
        for (IClasspathEntry sourcePathEntry : sourcePathEntries) {
            IPath outputLocation = sourcePathEntry.getOutputLocation();
            if (outputLocation != null) {
                libraries.add(PathUtils.containerFor(outputLocation).getLocation().toOSString());
            }
        }
        if (defaultOutputLocation != null) {
            libraries.add(PathUtils.containerFor(defaultOutputLocation).getLocation().toOSString());
        }
        return libraries;
    }

    public Set<IPath> getOutputPaths() {
        Set<IPath> libraries = newLinkedHashSet();
        for (IClasspathEntry sourcePathEntry : sourcePathEntries) {
            IPath outputLocation = sourcePathEntry.getOutputLocation();
            if (outputLocation != null) {
                libraries.add(PathUtils.containerFor(outputLocation).getLocation());
            }
        }
        if (defaultOutputLocation != null) {
            libraries.add(PathUtils.containerFor(defaultOutputLocation).getLocation());
        }
        return libraries;
    }

    public String toClasspath() {
        StringBuilder buffer = new StringBuilder();
        for (IClasspathEntry sourcePathEntry : sourcePathEntries) {
            IPath outputLocation = sourcePathEntry.getOutputLocation();
            if (outputLocation != null) {
                if (buffer.length() > 0) {
                    buffer.append(File.pathSeparatorChar);
                }
                buffer.append(PathUtils.containerFor(outputLocation).getLocation().toOSString());
            }
        }
        if (defaultOutputLocation != null) {
            if (buffer.length() > 0) {
                buffer.append(File.pathSeparatorChar);
            }
            buffer.append(PathUtils.containerFor(defaultOutputLocation).getLocation().toOSString());
        }
        return buffer.toString();
    }
}
