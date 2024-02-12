package org.openclover.eclipse.core.views.nodes;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.openclover.eclipse.core.projects.model.JavaElementAdapter;

public class PackageFragmentAdapter
    implements IPackageFragment, JavaElementAdapter {

    private final IPackageFragment target;

    public PackageFragmentAdapter(IPackageFragment target) {
        this.target = target;
    }

    @Override
    public IJavaElement getAdaptee() {
        return target;
    }

    @Override
    public boolean containsJavaResources() throws JavaModelException {
        return target.containsJavaResources();
    }

    @Override
    public ICompilationUnit createCompilationUnit(String s, String s1, boolean b, IProgressMonitor iProgressMonitor) throws JavaModelException {
        return target.createCompilationUnit(s, s1, b, iProgressMonitor);
    }

    @Override
    public IClassFile getClassFile(String s) {
        return target.getClassFile(s);
    }

    @Override
    public IClassFile[] getClassFiles() throws JavaModelException {
        return target.getClassFiles();
    }

    @Override
    public ICompilationUnit getCompilationUnit(String s) {
        return target.getCompilationUnit(s);
    }

    @Override
    public ICompilationUnit[] getCompilationUnits() throws JavaModelException {
        return target.getCompilationUnits();
    }

    @Override
    public ICompilationUnit[] getCompilationUnits(WorkingCopyOwner workingCopyOwner) throws JavaModelException {
        return target.getCompilationUnits(workingCopyOwner);
    }

    @Override
    public int getKind() throws JavaModelException {
        return target.getKind();
    }

    @Override
    public Object[] getNonJavaResources() throws JavaModelException {
        return target.getNonJavaResources();
    }

    @Override
    public boolean hasSubpackages() throws JavaModelException {
        return target.hasSubpackages();
    }

    @Override
    public boolean isDefaultPackage() {
        return target.isDefaultPackage();
    }

    @Override
    public IJavaElement[] getChildren() throws JavaModelException {
        return target.getChildren();
    }

    @Override
    public boolean hasChildren() throws JavaModelException {
        return target.hasChildren();
    }

    @Override
    public boolean exists() {
        return target.exists();
    }

    @Override
    public IJavaElement getAncestor(int i) {
        return target.getAncestor(i);
    }

    @Override
    public String getAttachedJavadoc(IProgressMonitor iProgressMonitor) throws JavaModelException {
        return target.getAttachedJavadoc(iProgressMonitor);
    }

    @Override
    public IResource getCorrespondingResource() throws JavaModelException {
        return target.getCorrespondingResource();
    }

    @Override
    public String getElementName() {
        return target.getElementName();
    }

    @Override
    public int getElementType() {
        return target.getElementType();
    }

    @Override
    public String getHandleIdentifier() {
        return target.getHandleIdentifier();
    }

    @Override
    public IJavaModel getJavaModel() {
        return target.getJavaModel();
    }

    @Override
    public IJavaProject getJavaProject() {
        return target.getJavaProject();
    }

    @Override
    public IOpenable getOpenable() {
        return target.getOpenable();
    }

    @Override
    public IJavaElement getParent() {
        return target.getParent();
    }

    @Override
    public IPath getPath() {
        return target.getPath();
    }

    @Override
    public IJavaElement getPrimaryElement() {
        return target.getPrimaryElement();
    }

    @Override
    public IResource getResource() {
        return target.getResource();
    }

    @Override
    public ISchedulingRule getSchedulingRule() {
        return target.getSchedulingRule();
    }

    @Override
    public IResource getUnderlyingResource() throws JavaModelException {
        return target.getUnderlyingResource();
    }

    @Override
    public boolean isReadOnly() {
        return target.isReadOnly();
    }

    @Override
    public boolean isStructureKnown() throws JavaModelException {
        return target.isStructureKnown();
    }

    @Override
    public Object getAdapter(Class aClass) {
        return target.getAdapter(aClass);
    }

    @Override
    public void close() throws JavaModelException {
        target.close();
    }

    @Override
    public String findRecommendedLineSeparator() throws JavaModelException {
        return target.findRecommendedLineSeparator();
    }

    @Override
    public IBuffer getBuffer() throws JavaModelException {
        return target.getBuffer();
    }

    @Override
    public boolean hasUnsavedChanges() throws JavaModelException {
        return target.hasUnsavedChanges();
    }

    @Override
    public boolean isConsistent() throws JavaModelException {
        return target.isConsistent();
    }

    @Override
    public boolean isOpen() {
        return target.isOpen();
    }

    @Override
    public void makeConsistent(IProgressMonitor iProgressMonitor) throws JavaModelException {
        target.makeConsistent(iProgressMonitor);
    }

    @Override
    public void open(IProgressMonitor iProgressMonitor) throws JavaModelException {
        target.open(iProgressMonitor);
    }

    @Override
    public void save(IProgressMonitor iProgressMonitor, boolean b) throws JavaModelException {
        target.save(iProgressMonitor, b);
    }

    @Override
    public void copy(IJavaElement iJavaElement, IJavaElement iJavaElement1, String s, boolean b, IProgressMonitor iProgressMonitor) throws JavaModelException {
        target.copy(iJavaElement, iJavaElement1, s, b, iProgressMonitor);
    }

    @Override
    public void delete(boolean b, IProgressMonitor iProgressMonitor) throws JavaModelException {
        target.delete(b, iProgressMonitor);
    }

    @Override
    public void move(IJavaElement iJavaElement, IJavaElement iJavaElement1, String s, boolean b, IProgressMonitor iProgressMonitor) throws JavaModelException {
        target.move(iJavaElement, iJavaElement1, s, b, iProgressMonitor);
    }

    @Override
    public void rename(String s, boolean b, IProgressMonitor iProgressMonitor) throws JavaModelException {
        target.rename(s, b, iProgressMonitor);
    }
}
