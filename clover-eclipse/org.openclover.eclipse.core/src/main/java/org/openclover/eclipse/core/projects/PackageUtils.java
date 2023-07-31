package org.openclover.eclipse.core.projects;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import java.util.Collection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.openclover.util.Lists.newArrayList;

public class PackageUtils {
    public static IProjectDescription duplicateProjectDescription(IProject sourceProject, IProject targetProject) throws CoreException {
        IProjectDescription userProjectDescription = sourceProject.getProject().getDescription();
        IProjectDescription thisProjectDescription =
            sourceProject.getProject().getWorkspace().newProjectDescription(
                "Clover internal instrumentation project for: \"" + targetProject.getName() + "\" (please don't modify this project)");

        IProject[] userProjectDynamicReferences = userProjectDescription.getReferencedProjects();
        IProject[] thisProjectDynamicReferences = new IProject[userProjectDynamicReferences.length];
        System.arraycopy(
            userProjectDynamicReferences, 0,
            thisProjectDynamicReferences, 0,
            userProjectDynamicReferences.length);

        thisProjectDescription.setDynamicReferences(thisProjectDynamicReferences);

        IProject[] userProjectReferences = userProjectDescription.getReferencedProjects();
        IProject[] thisProjectReferences = new IProject[userProjectReferences.length];
        System.arraycopy(
            userProjectReferences, 0,
            thisProjectReferences, 0,
            userProjectReferences.length);

        thisProjectDescription.setReferencedProjects(thisProjectReferences);
        return thisProjectDescription;
    }

    public static void addToDynamicReference(
        IProjectDescription sourceProjectDescription,
        IProject targetProject) throws CoreException {

        List<IProject> dynamicReferences = newArrayList(sourceProjectDescription.getReferencedProjects()); // copy
        dynamicReferences.add(targetProject);

        sourceProjectDescription.setDynamicReferences(
                dynamicReferences.toArray(new IProject[dynamicReferences.size()]));
    }

    public static void removeFromDynamicReference(
        IProjectDescription sourceProjectDescription,
        IProject targetProject) throws CoreException {

        Collection<IProject> dynamicReferences = newArrayList(sourceProjectDescription.getReferencedProjects()); // copy
        for (Iterator<IProject> iterator = dynamicReferences.iterator(); iterator.hasNext();) {
            IProject project = iterator.next();
            if (targetProject.equals(project)) {
                iterator.remove();
            }
        }
        sourceProjectDescription.setDynamicReferences(
            dynamicReferences.toArray(new IProject[dynamicReferences.size()]));
    }

}
