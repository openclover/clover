package org.openclover.eclipse.core.views.testrunexplorer;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.views.CloveredWorkspaceProvider;
import org.openclover.eclipse.core.views.WorkingSetNodeFilter;
import org.openclover.eclipse.core.views.nodes.NodeRelationshipFilter;
import org.openclover.eclipse.core.views.nodes.Nodes;
import org.eclipse.core.runtime.CoreException;

public class TestCaseTreeProvider
    extends CloveredWorkspaceProvider {

    public TestCaseTreeProvider(TestRunExplorerView part, TestRunExplorerViewSettings settings) {
        super(part, settings);
        CloverPlugin.getInstance().getCoverageMonitor().addCoverageChangeListener(this);
    }

    @Override
    public void dispose() {
        super.dispose();
        CloverPlugin.getInstance().getCoverageMonitor().removeCoverageChangeListener(this);
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
