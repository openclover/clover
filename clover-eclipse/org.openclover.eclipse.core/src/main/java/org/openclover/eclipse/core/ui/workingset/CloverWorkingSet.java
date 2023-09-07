package org.openclover.eclipse.core.ui.workingset;

import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

/**
 * A neat wrapper/manager around the Clover "large project" working set.
 */
public class CloverWorkingSet {

    private String CLOVER_WORKING_SET_NAME = CloverEclipsePluginMessages.CLOVER_WORKING_SET();

    private final IWorkingSetManager workingSetManager;
    private IWorkingSet mCachedWorkingSet;
    private IAdaptable[] mCachedElements;
    private List<IJavaElement> mCachedJavaElements;
    private final List<File> mMatchingFiles = newArrayList();
    private final List<File> mMatchingPackageDirs = newArrayList();
    private final List<File> mMatchingRootDirs = newArrayList();

    public CloverWorkingSet(IWorkingSetManager workingSetManager) {
        this.workingSetManager = workingSetManager;
        addChangeListener();
    }

    public boolean isWorkingSetMode() {
        return CloverPlugin.getInstance().isInWorkingSetMode();
    }

    private void checkCache() {
        if (mCachedWorkingSet != null) {
            return;
        }

        IWorkingSet ws = workingSetManager.getWorkingSet(CLOVER_WORKING_SET_NAME);
        if (ws == null) {
            ws = workingSetManager.createWorkingSet(CLOVER_WORKING_SET_NAME, new IAdaptable[0]);
            ws.setId("org.eclipse.jdt.ui.JavaWorkingSetPage");
            workingSetManager.addWorkingSet(ws);
        }
        mCachedWorkingSet = ws;
        mCachedElements = ws.getElements();
        extractCurrentJavaElements(mCachedElements);
    }

    public IWorkingSet getWorkingSet() {
        checkCache();
        return mCachedWorkingSet;
    }

    public void setElements(IAdaptable[] elements) {
        getWorkingSet().setElements(elements);
        clearCache();
    }

    public List getMatchingFiles() {
        checkCache();
        return Collections.unmodifiableList(mMatchingFiles);
    }

    public List getMatchingPackageDirs() {
        checkCache();
        return Collections.unmodifiableList(mMatchingPackageDirs);
    }

    public List getMatchingRootDirs() {
        checkCache();
        return Collections.unmodifiableList(mMatchingRootDirs);
    }

    public void toggleInclusion(List toggleList, IProgressMonitor monitor) throws CoreException {
        checkCache();

        for (Object o : toggleList) {
            IJavaElement je = asJavaElement(o);
            if (je != null) {
                if (mCachedJavaElements.contains(je)) {
                    mCachedJavaElements.remove(je);
                } else {
                    mCachedJavaElements.add(je);
                }

                // make sure it is recompiled
                IResource res = je.getResource();
                if (res != null) {
                    je.getResource().touch(monitor);
                }
            }
        }

        setElements(mCachedJavaElements.toArray(new IAdaptable[mCachedJavaElements.size()]));
    }

    /**
     * List of the IJavaElements described by the current working set
     */
    private void extractCurrentJavaElements(IAdaptable[] current) {
        mCachedJavaElements = new ArrayList<>(current.length);
        mMatchingFiles.clear();
        mMatchingPackageDirs.clear();
        mMatchingRootDirs.clear();

        for (IAdaptable a : current) {
            IJavaElement je = asJavaElement(a);
            if (je != null) {
                IResource r = je.getResource();
                mCachedJavaElements.add(je);
                if ((r != null) && (r.getLocation() != null)) {
                    try {
                        File file = r.getLocation().toFile();
                        file = file == null ? null : file.getCanonicalFile();
                        if (file == null) {
                            //TODO log "could not match to filesystem"
                        } else if (je instanceof IPackageFragment) {
                            mMatchingPackageDirs.add(file);
                        } else if (je instanceof ICompilationUnit) {
                            mMatchingFiles.add(file);
                        } else if ((je instanceof IJavaProject) || (je instanceof IPackageFragmentRoot)) {
                            mMatchingRootDirs.add(file);
                        }
                    } catch (IOException e) {
                        // just ignore if we can't canonical-ize
                    }
                }
            }
        }
    }

    public boolean includes(IAdaptable adaptable) {
        IJavaElement je = asJavaElement(adaptable);
        if (je != null) {
            if (je instanceof IMethod) {
                return includesCU(((IMethod)je).getCompilationUnit());
            } else if (je instanceof ICompilationUnit) {
                return includesCU(adaptable);
            } else if (je instanceof IPackageFragment) {
                return includesExamplePackageFragment(adaptable);
            } else if (je instanceof IPackageFragmentRoot) {
                return includesPackageFragmentRoot(adaptable);
            } else if (je instanceof IJavaProject) {
                return includesJavaProject(adaptable);
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean includesFile(File path) {
        String pathName = path.toString();

        // check if it matches our files
        for (File expected : mMatchingFiles) {
            if (expected.equals(path)) {
                return true;
            }
        }

        // see if it is a direct child of a package dir
        for (File dir : mMatchingPackageDirs) {
            if (dir.equals(path.getParentFile())) {
                return true;
            }
        }

        // see if it is a descendant of any of the roots
        for (File root : mMatchingRootDirs) {
            if (pathName.startsWith(root.getPath())) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return true if path (which is considered a file) is considered the working set
     */
    public boolean includesCU(IAdaptable adaptable) {
        checkCache();

        final IJavaElement je = asJavaElement(adaptable);
        final IResource resource = je == null ? null : je.getResource();
        final IPath ipath = resource == null ? null : resource.getLocation();
        final File path = ipath == null ? null : ipath.toFile();
        if (path != null) {
            return includesFile(path);
        } else {
            return false;
        }
    }

    /**
     * Package fragments are anchored off one of potentially many package fragment
     * roots but Clover has a one-root view of packages. This will determine if a
     * package root is in the working set, not just based on its own location,
     * but by any location of any similar package fragments in any of the roots.
     * This is a slight hack, and should be dispensed with once we have multiple
     * package roots in the model.
     */
    public boolean includesExamplePackageFragment(IAdaptable adaptable) {
        checkCache();

        try {
            IPackageFragment fragment = (IPackageFragment) asJavaElement(adaptable);
            if (fragment != null) {
                IPackageFragmentRoot[] allFragmentRoots = fragment.getJavaProject().getPackageFragmentRoots();
                ArrayList<IPackageFragment> allSimilarPackages = new ArrayList<>(allFragmentRoots.length);
                for (IPackageFragmentRoot allFragmentRoot : allFragmentRoots) {
                    if (allFragmentRoot.getKind() == IPackageFragmentRoot.K_SOURCE) {
                        IPackageFragment packageFragment = allFragmentRoot.getPackageFragment(fragment.getElementName());
                        if (packageFragment != null) {
                            allSimilarPackages.add(packageFragment);
                        }
                    }
                }

                return includesAnyPackageFragments(allSimilarPackages);
            }
        } catch (JavaModelException e) {
            CloverPlugin.logError("Unable to query all package fragments", e);
        }
        return false;
    }

    private boolean includesAnyPackageFragments(List<IPackageFragment> fragments) {
        boolean includes = false;
        for (IPackageFragment fragment : fragments) {
            includes = includes || includesPackageFragment(fragment);
            if (includes) {
                break;
            }
        }
        return includes;
    }

    private boolean includesPackageFragment(IPackageFragment fragment) {
        checkCache();

        IPath projectLocation = fragment.getResource().getLocation();
        return includes(
                new List[]{ mMatchingFiles },
                new List[]{ mMatchingPackageDirs, mMatchingRootDirs },
            projectLocation);
    }

    public boolean includesJavaProject(IAdaptable adaptable) {
        checkCache();

        IJavaElement je = asJavaElement(adaptable);
        if (je != null) {
            IPath projectLocation = je.getResource().getLocation();

            return includes(
                new List[] { mMatchingFiles, mMatchingPackageDirs, mMatchingRootDirs },
                new List[] { },
                projectLocation);
        }
        return false;
    }

    private boolean includes(List<File>[] children, List<File>[] peers, IPath resourcePath) {
        for (List<File> match : children) {
            // Check if there are any children of this resource,
            // if so then this resource is included.
            // This check is necessary because once a child file is removed from a working
            // set, the set changes from holding just a reference to the parent to
            // a set with no parent but all children except the one removed.
            // Logically this still means the parent is included, however hence why we perform
            // this test.
            for (File expected : match) {
                if (new Path(expected.toString()).matchingFirstSegments(resourcePath) == resourcePath.segmentCount()) {
                    return true;
                }
            }
        }

        for (List<File> match : peers) {
            // Check if this resource matches any of its peers.
            // If so then it has been explicitly included.
            for (File expected : match) {
                Path expectedPath = new Path(expected.toString());
                if (resourcePath.matchingFirstSegments(expectedPath) == expectedPath.segmentCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean includesPackageFragmentRoot(IAdaptable adaptable) {
        checkCache();

        IJavaElement je = asJavaElement(adaptable);
        if (je != null) {
            IPath projectLocation = je.getResource().getLocation();

            return includes(
                new List[] { mMatchingFiles, mMatchingPackageDirs },
                new List[] { mMatchingRootDirs },
                projectLocation);
        }
        return false;
    }

    private static IJavaElement asJavaElement(Object o) {
        if (o == null) {
            return null;
        }
        if (!(o instanceof IAdaptable)) {
            return null;
        }
        IAdaptable a = (IAdaptable) o;
        return (IJavaElement) a.getAdapter(IJavaElement.class);
    }

    private void addChangeListener() {
        workingSetManager.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                boolean refresh = false;
                String property= event.getProperty();
                if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(property)) {
                    IWorkingSet ws = (IWorkingSet) event.getNewValue();
                    if (ws.getName().equals(CLOVER_WORKING_SET_NAME)) {
                        refresh = true;
                    }
                } else if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(property)) {
                    refresh = true;
                } else if (IWorkingSetManager.CHANGE_WORKING_SET_REMOVE.equals(property)) {
                    IWorkingSet ws = (IWorkingSet) event.getOldValue();
                    if (ws.getName().equals(CLOVER_WORKING_SET_NAME)) {
                        refresh = true;
                    }
                }
                if (refresh) {
                    clearCache();

                    if (CloverPlugin.getInstance().isInWorkingSetMode()) {
                        //Fire coverage change so all views change their filters
                        //accordingly
                        CloverProject.refreshAllModels(true, true);
                    }
                }
            }
        });
    }

    private void clearCache() {
        mCachedWorkingSet = null;
    }

}
