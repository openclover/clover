package org.openclover.eclipse.core.views.testrunexplorer;

import org.eclipse.core.runtime.CoreException;
import org.openclover.eclipse.core.ui.projects.DatabaseChangeEvent;
import org.openclover.eclipse.core.views.CloveredWorkspaceProvider;
import org.openclover.eclipse.core.views.ExplorerView;
import org.openclover.eclipse.core.views.WorkingSetNodeFilter;
import org.openclover.eclipse.core.views.nodes.NodeRelationshipFilter;
import org.openclover.eclipse.core.views.nodes.Nodes;

public class TestCaseTreeProvider
    extends CloveredWorkspaceProvider {

    public TestCaseTreeProvider(TestRunExplorerView part, TestRunExplorerViewSettings settings) {
        super(part, settings);
        // Note: parent CloveredWorkspaceProvider already registers this as a coverage change listener;
        // do not add again to avoid double-firing databaseChanged().
    }

    @Override
    public void databaseChanged(DatabaseChangeEvent event) {
        // Use preserveExpandedPaths=false so that expandToLevel() is called after each
        // substantive change. This is required because the Clover DB loads asynchronously
        // after the tree is first shown: at creation time the tree has no data, projects
        // appear as leaves, and a subsequent refresh(true,...) would restore the "nothing
        // expanded" state — test cases would never appear. Expanding to the configured
        // depth on every DB change is the correct behaviour for the Test Run Explorer.
        if (event.isSubstantiveProjectChange()) {
            part.refresh(false, ExplorerView.ENTIRE_WORKSPACE);
        }
    }

    @Override
    protected NodeRelationshipFilter getFilter() {
        return new WorkingSetNodeFilter() {
            @Override
            public boolean accept(Object element) {
                return
                    super.accept(element)
                    && containsTestCases(element);
            }

            private boolean containsTestCases(Object element) {
                try {
                    return Nodes.containsTestCases(element);
                } catch (CoreException e) {
                    return false;
                }
            }

            @Override
            public boolean requiresFiltering() {
                return true;
            }
        };
    }
    
    private TestRunExplorerViewSettings getSettings() {
        return (TestRunExplorerViewSettings)settings;
    }
}
