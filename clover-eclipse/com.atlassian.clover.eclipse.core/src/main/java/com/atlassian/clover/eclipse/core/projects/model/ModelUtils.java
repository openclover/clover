package com.atlassian.clover.eclipse.core.projects.model;

import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IProject;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.eclipse.core.CloverPlugin;

public class ModelUtils {
    public static final int SEARCH_SELF_AND_DEPENDENTS = -1;
    public static final int SEARCH_SELF = 0;
    public static final int SEARCH_SELF_AND_DEPENDENCIES = 1;
    
    public static IType findTestCaseClass(int searchType, IJavaProject project, ClassInfo testClass) throws JavaModelException {
        IType type = null;
        if (project != null) {
            try {
                type = project.findType(testClass.getQualifiedName(), (IProgressMonitor) null);

                if (type == null) {
                    IProject[] projects = new IProject[] {};
                    if (searchType == SEARCH_SELF_AND_DEPENDENTS) {
                        projects = project.getProject().getReferencingProjects();
                    } else if (searchType == SEARCH_SELF_AND_DEPENDENCIES) {
                        projects = project.getProject().getReferencedProjects();
                    }
                    for (IProject other : projects) {
                        if (CloverProject.isAppliedTo(other)) {
                            type = findTestCaseClass(searchType, JavaCore.create(other), testClass);
                            if (type != null) {
                                break;
                            }
                        }
                    }
                }
            } catch (CoreException e) {
                CloverPlugin.logError("Unable to look up test case class", e);
            }
        }
        return type;
    }

    public static IMethod findTestCaseMethod(int searchType, IJavaProject javaProject, TestCaseInfo testCase) throws JavaModelException {
        IType type = findTestCaseClass(searchType, javaProject, testCase.getRuntimeType());

        MethodInfo cloverTestCaseMethod = testCase.getSourceMethod();

        IMethod[] methods =
            type == null
                ? new IMethod[0]
                : type.getMethods();

        IMethod testCaseMethod = null;

        for (IMethod method : methods) {
            if (method.getElementName().equals(cloverTestCaseMethod.getSimpleName())) {
                testCaseMethod = method;
                break;
            }
        }
        return testCaseMethod;
    }

}
