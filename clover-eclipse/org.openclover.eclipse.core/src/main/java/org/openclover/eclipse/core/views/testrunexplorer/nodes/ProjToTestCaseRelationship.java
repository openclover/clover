package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import com.atlassian.clover.registry.entities.FullProjectInfo;
import org.openclover.eclipse.core.views.nodes.NodeRelationship;
import org.openclover.eclipse.core.views.nodes.NodeRelationshipFilter;
import org.openclover.eclipse.core.views.nodes.Nodes;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.eclipse.core.resources.IProject;

import static org.openclover.util.Lists.newLinkedList;

public class ProjToTestCaseRelationship extends NodeRelationship {
    private TestCaseNodeFactory tcnFactory;

    public ProjToTestCaseRelationship(TestCaseNodeFactory tcnFactory) {
        this.tcnFactory = tcnFactory;
    }

    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            final CloverProject cloverProject = CloverProject.getFor((IProject) object);
            final FullProjectInfo testProjectInfo = cloverProject == null ? null : cloverProject.getModel().getTestOnlyProjectInfo();
            if (testProjectInfo != null && testProjectInfo.hasTestResults()) {
                return filter.perform(
                    Nodes.collectTestCases(
                            (IProject) object,
                            newLinkedList(),
                            new Nodes.ToTestCaseNodeCoverter(tcnFactory)));
            }
        } catch (Exception e) {
            CloverPlugin.logError("Unable to retrieve children for project " + object, e);
        }
        return new Object[]{};
    }

    @Override
    public boolean includes(Object object) {
        return object instanceof IProject;
    }
}
