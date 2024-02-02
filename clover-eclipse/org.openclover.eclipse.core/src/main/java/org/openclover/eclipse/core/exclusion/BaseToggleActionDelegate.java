package org.openclover.eclipse.core.exclusion;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.openclover.eclipse.core.projects.settings.source.SourceFolderPattern;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public abstract class BaseToggleActionDelegate extends BaseActionDelegate {

    @Override
    public void run(IAction action) {
        if (selectedElement != null) {
            if (ExclusionUtil.togglePresence(selectedElement, isExclude(), isSourceRootBased())) {
                new ScheduleRebuildJob(selectedElement.getProject(), IncrementalProjectBuilder.FULL_BUILD).schedule();
            } else {
                touch(selectedElement);
            }
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        // TODO Auto-generated method stub
        super.selectionChanged(action, selection);
        if (selectedElement != null) {
            final boolean sourceRootBased = isSourceRootBased();

            final StringBuilder sb = new StringBuilder(getActionName());
            if (sourceRootBased) {
                sb.append(" [")
                        .append(selectedElement.getPackageFragmentRoot().getResource().getProjectRelativePath().toString())
                        .append("] ");
            } else {
                sb.append(" ");
            }
            sb.append(selectedElement.getPath());
            action.setText(sb.toString());

            action.setChecked(ExclusionUtil.isPresent(selectedElement, isExclude(), sourceRootBased));
            action.setEnabled(shouldEnable(selectedElement));
            
        }
    }

    private void touch(final ICloverExcludable selectedElement) {
        
        IWorkspaceRunnable runnable = progressMonitor -> {
            IJavaElement javaElement = selectedElement.getJavaElement();
            CloverPlugin.logVerbose("Requesting rebuild of " + javaElement.getElementName());
            if (selectedElement.isLeaf()) {
                javaElement.getResource().touch(progressMonitor);
            } else {
                final String rootName = javaElement instanceof IPackageFragmentRoot ? "" : javaElement.getElementName();
                final String rootPrefix = rootName.length() == 0 ? "" : rootName + '.';
                final Collection<IPackageFragmentRoot> roots = isSourceRootBased() ?
                        Collections.singleton(selectedElement.getPackageFragmentRoot())
                        : Arrays.asList(javaElement.getJavaProject().getAllPackageFragmentRoots());
                for (IPackageFragmentRoot srcRoot : roots) {
                    if (srcRoot.getKind() != IPackageFragmentRoot.K_SOURCE) continue;
                    final IJavaElement[] packageFragments = srcRoot.getChildren();
                    for (IJavaElement fragment: packageFragments) {
                        final String name = fragment.getElementName();
                        if (rootName.length() == 0 || name.equals(rootName) || name.startsWith(rootPrefix)) {
                            for (ICompilationUnit cu : ((IPackageFragment)fragment).getCompilationUnits()) {
                                cu.getResource().touch(progressMonitor);
                            }
                        }
                    }

                }
            }
            new ScheduleRebuildJob(selectedElement.getProject(), IncrementalProjectBuilder.AUTO_BUILD).schedule();
        };
        
        try {
            ResourcesPlugin.getWorkspace().run(runnable, null);
        } catch (CoreException e) {
            CloverPlugin.logError("Error touching resource " + selectedElement.getJavaElement().getElementName(), e);
        }

    }

    private boolean isSourceRootBased() {
        try {
            final CloverProject cloverProject = CloverProject.getFor(selectedElement.getProject());
            return cloverProject != null && cloverProject.getSettings().isInstrumentSelectedSourceFolders();
        } catch (CoreException e) {
            return false;
        }
    }

    private boolean shouldEnable(ICloverExcludable element) {
        final CloverProject cloverProject;
        try {
            cloverProject = CloverProject.getFor(element.getProject());
        } catch (CoreException e) {
            return false;
        }
        if (cloverProject == null) {
            return false;
        }
        final ProjectSettings settings = cloverProject.getSettings();
        if (!settings.isInstrumentSelectedSourceFolders()) {
            return true;
        }

        final String path = element.getPackageFragmentRoot().getResource().getProjectRelativePath().toString();
        for (SourceFolderPattern sfp : settings.getInstrumentedFolderPatterns()) {
            if (path.equals(sfp.getSrcPath())) {
                return sfp.isEnabled();
            }
        }
        return false;
    }

    protected abstract String getActionName();

    protected abstract boolean isExclude();
}