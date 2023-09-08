package org.openclover.eclipse.core.projects.builder;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.util.Collection;
import java.util.List;

import static org.openclover.util.Lists.newLinkedList;

public class InstrumentationProjectPathMap extends ProjectPathMap {
    private CloverProject project;
    private ProjectPathMap userPathMap;
    private IPath workingArea;

    public InstrumentationProjectPathMap(CloverProject project, IPath workingArea) throws CoreException {
        this(project, new ProjectPathMap(project.getJavaProject()), workingArea);
    }

    public InstrumentationProjectPathMap(CloverProject project, ProjectPathMap userPathMap, IPath workingArea) throws CoreException {
        this.userPathMap = userPathMap;
        this.project = project;
        this.workingArea = workingArea;
        this.projectLocation = project.getProject().getFullPath();

        ProjectSettings properties = project.getSettings();

        boolean outputRootSameAsProject = properties.isOutputRootSameAsProject();
        boolean recreateOutputDirs = properties.isRecreateOutputDirs();

        this.defaultOutputLocation =
            outputRootSameAsProject
                ? userPathMap.defaultOutputLocation
                : recreateOutputDirs
                    ? displace(
                        userPathMap.defaultOutputLocation,
                        userPathMap.projectLocation,
                        customRootPathFor(properties))
                    : customRootPathFor(properties);

        this.allClasspathEntries =
            resolveClasspathEntries(
                displaceAll(
                    userPathMap.allClasspathEntries,
                    userPathMap.projectLocation,
                    workingArea != null,
                    workingArea,
                    !outputRootSameAsProject,
                    recreateOutputDirs,
                    outputRootSameAsProject
                        ? customRootPathFor(properties)
                        : project.getInstrumentationOutputRootDir().getFullPath()),
                project.getJavaProject());

        this.sourcePathEntries = sourcepathEntriesIn(allClasspathEntries);
    }

    private IPath customRootPathFor(ProjectSettings properties) {
        return project.getProject().getFullPath().append(properties.getOutputRoot());
    }

    private IClasspathEntry[] sourcepathEntriesIn(IClasspathEntry[] classpathEntries) {
        List<IClasspathEntry> sourcepathEntries = newLinkedList();
        for (IClasspathEntry classpathEntry : classpathEntries) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                sourcepathEntries.add(classpathEntry);
            }
        }
        return sourcepathEntries.toArray(new IClasspathEntry[sourcepathEntries.size()]);
    }


    /**
     * @return the absolute workspace-relative path to the displaced source root the path would reside in, or null if no such path exists
     */
    public IPath getDisplacedSourceRootFor(IPath path) {
        for (int i = 0; i < userPathMap.sourcePathEntries.length; i++) {
            IClasspathEntry entry = userPathMap.sourcePathEntries[i];
            if (entry.getPath().isPrefixOf(path)) {
                return sourcePathEntries[i].getPath();
            }
        }
        return null;
    }

    /**
     * @return the absolute workspace-relative path to the displaced source file the path would reside at, or null if no such path exists
     */
    public IPath getDisplacedSourcePathFor(IPath path) {
        for (int i = 0; i < userPathMap.sourcePathEntries.length; i++) {
            IClasspathEntry entry = userPathMap.sourcePathEntries[i];
            if (entry.getPath().isPrefixOf(path)) {
                return sourcePathEntries[i].getPath().append(
                    path.removeFirstSegments(path.matchingFirstSegments(entry.getPath())));
            }
        }
        return null;
    }

    /**
     * @return the absolute workspace-relative path to original path of the displaced output path, or null if no such path exists
     */
    public IPath getOutputPathForOutputResource(IPath outputResourcePath) {
        //Try to find a source path specific output path before looking at project output path
        for (int i = 0; i < userPathMap.allClasspathEntries.length; i++) {
            IClasspathEntry entry = userPathMap.allClasspathEntries[i];
            if (entry.getOutputLocation() != null && entry.getOutputLocation().isPrefixOf(outputResourcePath)) {
                return displace(outputResourcePath, entry.getOutputLocation(), allClasspathEntries[i].getOutputLocation());
            }
        }

        if (userPathMap.defaultOutputLocation != null && userPathMap.defaultOutputLocation.isPrefixOf(outputResourcePath)) {
            return displace(outputResourcePath, userPathMap.defaultOutputLocation, defaultOutputLocation);
        }

        return outputResourcePath;
    }

    public IPath getOriginalSourceRootFor(IPath path) {
        for (int i = 0; i < userPathMap.sourcePathEntries.length; i++) {
            IClasspathEntry entry = userPathMap.sourcePathEntries[i];
            if (entry.getPath().isPrefixOf(path)) {
                return userPathMap.sourcePathEntries[i].getPath();
            }
        }
        return null;
    }

    public static IClasspathEntry[] displaceAll(
        IClasspathEntry[] entries,
        IPath root,
        boolean displaceSourcepathEntries,
        IPath srcBase,
        boolean displaceOutputLocations,
        boolean recreateOutputLocations,
        IPath classesBase) {

        IClasspathEntry[] displaced = new IClasspathEntry[entries.length];
        for (int i = 0; i < entries.length; i++) {
            IClasspathEntry entry = entries[i];
            if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                displaced[i] = JavaCore.newSourceEntry(
                    displaceSourcepathEntries
                        ? displace(entry.getPath(), root, srcBase)
                        : entry.getPath(),
                    entry.getInclusionPatterns(),
                    entry.getExclusionPatterns(),
                    displaceOutputLocations
                        ? recreateOutputLocations
                            ? displace(entry.getOutputLocation(), root, classesBase)
                            : classesBase
                        : entry.getOutputLocation(),
                    entry.getExtraAttributes());
            } else {
                displaced[i] = entries[i];
            }
        }
        return displaced;
    }

    public static IPath displace(IPath path, IPath root, IPath newRoot) {
        if (path != null) {
            if (!root.isPrefixOf(path)) {
                throw new IllegalArgumentException("Resource path " + path + " isn't under root path " + root);
            } else {
                return newRoot.append(path.removeFirstSegments(root.segmentCount()));
            }
        }
        return path;
    }

    private static IClasspathEntry[] resolveClasspathEntries(IClasspathEntry[] classpathEntries, IJavaProject javaProject) throws JavaModelException {
        List<IClasspathEntry> classpathLibs = newLinkedList();
        for (IClasspathEntry classpathEntry : classpathEntries) {
            resolveAndAddEntry(classpathLibs, javaProject, classpathEntry);
        }
        return classpathLibs.toArray(new IClasspathEntry[classpathLibs.size()]);
    }

    private static void resolveAndAddEntry(Collection<IClasspathEntry> classpathLibs, IJavaProject javaProject, IClasspathEntry entry)
        throws JavaModelException {
        if (entry == null) {
            CloverPlugin.logWarning("Unresolved classpath entry");
        } else {
            switch (entry.getEntryKind()) {
                case IClasspathEntry.CPE_PROJECT:
                case IClasspathEntry.CPE_SOURCE:
                case IClasspathEntry.CPE_LIBRARY:
                    classpathLibs.add(entry);
                    break;

                case IClasspathEntry.CPE_CONTAINER:
                    IClasspathContainer container =
                        JavaCore.getClasspathContainer(entry.getPath(), javaProject);
                    if (container != null) {
                        IClasspathEntry[] containerEntries = container.getClasspathEntries();

                        for (IClasspathEntry containerEntry : containerEntries) {
                            resolveAndAddEntry(
                                    classpathLibs,
                                    javaProject,
                                    containerEntry);
                        }
                    } else {
                        CloverPlugin.logWarning("Unable to resolve classpath container entry: " + entry.getPath());
                    }
                    break;

                case IClasspathEntry.CPE_VARIABLE:
                    IClasspathEntry variableEntry =
                        JavaCore.getResolvedClasspathEntry(entry);
                    if (variableEntry != null) {
                        resolveAndAddEntry(
                            classpathLibs,
                            javaProject,
                            variableEntry);
                    } else {
                        CloverPlugin.logWarning("Unable to resolve classpath variable entry: " + entry.getPath());
                    }
                    break;
            }
        }
    }

    public IPath guessClassFileFor(IPath sourcePath) {
        IClasspathEntry sourcePathEntry = null;

        for (int i = 0; i < userPathMap.sourcePathEntries.length; i++) {
            IClasspathEntry entry = userPathMap.sourcePathEntries[i];
            if (entry.getPath().isPrefixOf(sourcePath)) {
                sourcePathEntry = userPathMap.sourcePathEntries[i];
                break;
            }
        }

        if (sourcePathEntry != null) {
            IPath sourcePathBranch = sourcePath.removeFirstSegments(sourcePath.matchingFirstSegments(sourcePathEntry.getPath()));

            IPath outputPathRoot;
            if (sourcePathEntry.getOutputLocation() != null) {
                outputPathRoot = sourcePathEntry.getOutputLocation();
            } else {
                outputPathRoot = userPathMap.getDefaultOutputLocation();
            }
            return
                getOutputPathForOutputResource(outputPathRoot)
                    .append(sourcePathBranch)
                    .removeFileExtension()
                    .addFileExtension("class");
        } else {
            return null;
        }
    }

    /**
     * @return the absolute workspace-relative path to original path of the displaced output path, or null if no such path exists
     */
    public IPath getOutputRootForWorkignAreaSourceResource(IPath workingAreaResourcePath) {
        //Try to find a source path specific output path before looking at project output path
        for (IClasspathEntry entry : sourcePathEntries) {
            if (entry.getOutputLocation() != null && entry.getPath().isPrefixOf(workingAreaResourcePath)) {
                return entry.getOutputLocation();
            }
        }

        return defaultOutputLocation;
    }

    public IPath getWorkingArea() {
        return workingArea;
    }
}
