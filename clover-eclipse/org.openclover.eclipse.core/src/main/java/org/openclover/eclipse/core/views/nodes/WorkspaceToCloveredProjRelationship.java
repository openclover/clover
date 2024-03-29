package org.openclover.eclipse.core.views.nodes;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

import java.util.Iterator;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.eclipse.core.CloverPlugin.logError;

/**
 * Collectes the children of IWorkspace nodes as IProject nodes
*/
public class WorkspaceToCloveredProjRelationship extends NodeRelationship {
    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            IWorkspace workspace = (IWorkspace)object;
            List<IProject> projects = newArrayList(workspace.getRoot().getProjects());
            Iterator<IProject> projectsIterator = projects.iterator();
            while (projectsIterator.hasNext()) {
                IProject project = projectsIterator.next();
                CloverProject cloverProject = CloverProject.getFor(project);
                if (cloverProject == null) {
                    projectsIterator.remove();
                }
            }
            return filter.perform(projects);
        } catch (Exception e) {
            logError("Unable to retrieve children for parent " + object, e);
        }
        return new Object[] {};
    }

    @Override
    public boolean includes(Object object) {
        return object instanceof IWorkspace;
    }
}
