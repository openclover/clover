package com.atlassian.clover.eclipse.core.projects.settings.source;

import com.atlassian.clover.eclipse.core.projects.settings.ProjectSettings;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static clover.com.google.common.collect.Lists.newArrayList;

public class SourceRootsWithPatternTreeContentProvider implements ITreeContentProvider {
    private final IJavaProject project;
    private final SourceRootWithPattern[] children;

    private final List<InstrumentedSourceFoldersListener> listeners = newArrayList();

    public SourceRootsWithPatternTreeContentProvider(IJavaProject project, Collection<SourceFolderPattern> existing) {
        Map<String, SourceFolderPattern> index = new HashMap<String, SourceFolderPattern>(existing.size());
        for (SourceFolderPattern sfp : existing) {
            index.put(sfp.getSrcPath(), sfp);
        }

        List<SourceRootWithPattern> list = newArrayList();
        try {
            final IPackageFragmentRoot[] pfRoots = project.getPackageFragmentRoots();
            for (IPackageFragmentRoot pfRoot : pfRoots) {
                try {
                    if (pfRoot.getKind() != IPackageFragmentRoot.K_SOURCE) {
                        continue;
                    }
                    final String path = pfRoot.getCorrespondingResource().getProjectRelativePath().toString();
                    SourceFolderPattern sfp = index.get(path);
                    if (sfp == null) {
                        sfp = new SourceFolderPattern(path,
                                                      ProjectSettings.DEFAULT_INCLUDE_PATTERN,
                                                      ProjectSettings.DEFAULT_EXCLUDE_PATTERN,
                                                      false);
                    }
                    list.add(new SourceRootWithPattern(pfRoot, sfp));
                } catch (JavaModelException e) {
                    CloverPlugin.logError("Unable to process package fragment [" + pfRoot.getElementName() + "], skipping." , e);
                }
            }
        } catch (JavaModelException e) {
            CloverPlugin.logError("Unable to retrieve package fragment roots." , e);
        }
        children = list.toArray(new SourceRootWithPattern[list.size()]);
        this.project = project;
    }

    @Override
    public SourceRootWithPattern[] getChildren(Object parentElement) {
        if (parentElement == project) {
            return children;
        } else {
            return new SourceRootWithPattern[0];
        }
    }

    @Override
    public Object getParent(Object element) {
        return element == project ? null : project;
    }


    @Override
    public boolean hasChildren(Object element) {
        return element == project;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    public SourceRootWithPattern[] getAllElements() {
        return children;
    }

    public SourceRootWithPattern[] getEnabledElements() {
        List<SourceRootWithPattern> selected = new ArrayList<SourceRootWithPattern>(children.length);
        for (SourceRootWithPattern child : children) {
            if (child.getPattern().isEnabled()) {
                selected.add(child);
            }
        }

        return selected.toArray(new SourceRootWithPattern[selected.size()]);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public IJavaProject getProject() {
        return project;
    }

    public void addChangeListener(InstrumentedSourceFoldersListener listener) {
        listeners.add(listener);
    }

    public void notifyElementChange(SourceRootWithPattern element) {
        for (InstrumentedSourceFoldersListener listener : listeners) {
            listener.selectionChanged(element);
        }
    }

    public void notifyPerFolderSettingsEnabled(boolean enabled) {
        for (InstrumentedSourceFoldersListener listener : listeners) {
            listener.perFolderSelectionEnabled(enabled);
        }
    }
}