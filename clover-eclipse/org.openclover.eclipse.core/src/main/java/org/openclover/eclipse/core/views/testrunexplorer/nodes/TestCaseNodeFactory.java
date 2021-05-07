package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.eclipse.jdt.core.IMethod;

public interface TestCaseNodeFactory {
    public TestCaseNode newNode(TestCaseInfo tci, IMethod method);
}
