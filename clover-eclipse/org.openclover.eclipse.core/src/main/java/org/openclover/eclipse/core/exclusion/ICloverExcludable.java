package org.openclover.eclipse.core.exclusion;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public interface ICloverExcludable {
    IProject getProject();

    IPackageFragmentRoot getPackageFragmentRoot();

    IJavaElement getJavaElement();

    String getPath();

    boolean isLeaf();
}
