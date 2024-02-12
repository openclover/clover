package org.openclover.eclipse.core.exclusion;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.openclover.eclipse.core.projects.CloverProject;

public class CloverExcludableAdapterFactory implements IAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == ICloverExcludable.class && adaptableObject instanceof IJavaElement) {
            final IJavaElement javaElement = (IJavaElement) adaptableObject;
            final IProject project = javaElement.getJavaProject().getProject();
            try {
                if (!CloverProject.isAppliedTo(project)) {
                    return null;
                }
            } catch (CoreException e) {
                return null;
            }
            if (adaptableObject instanceof IPackageFragment) {
                try {
                    final IPackageFragment pf = (IPackageFragment) adaptableObject;
                    if (pf.getKind() == IPackageFragmentRoot.K_SOURCE) {
                        final String path = pf.getElementName().replace('.', '/') + "/";
                        return new CloverExcludable(project, pf, path);
                    }
                } catch (JavaModelException e) {
                    // ignore
                }
            }
            if (adaptableObject instanceof IPackageFragmentRoot) {
                try {
                    final IPackageFragmentRoot pfr = (IPackageFragmentRoot) adaptableObject;
                    if (pfr.getKind() == IPackageFragmentRoot.K_SOURCE) {
                        return new CloverExcludable(project, pfr, "/");
                    }
                } catch (JavaModelException e) {
                    // ignore
                }
            }
            if (adaptableObject instanceof ICompilationUnit) {
                final ICompilationUnit cu = (ICompilationUnit) adaptableObject;
                final String path = cu.getParent().getElementName().replace('.', '/') + '/' + cu.getElementName();
                return new CloverExcludable(project, cu, path);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static Class[] ADAPTER_LIST = { ICloverExcludable.class };

    @Override
    @SuppressWarnings("unchecked")
    public Class[] getAdapterList() {
        return ADAPTER_LIST;
    }

}

class CloverExcludable implements ICloverExcludable {
    private final IProject project;
    private final IJavaElement javaElement;
    private final IPackageFragmentRoot packageFragmentRoot;
    private final String path;

    public CloverExcludable(IProject project, IJavaElement element, String path) {
        this.project = project;
        this.javaElement = element;
        this.path = path;

        packageFragmentRoot = (IPackageFragmentRoot) javaElement.getAncestor(IPackageFragment.PACKAGE_FRAGMENT_ROOT);
    }

    @Override
    public IProject getProject() {
        return project;
    }

    @Override
    public IPackageFragmentRoot getPackageFragmentRoot() {
        return packageFragmentRoot;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isLeaf() {
        return javaElement instanceof ICompilationUnit;
    }

    @Override
    public IJavaElement getJavaElement() {
        return javaElement;
    }

}