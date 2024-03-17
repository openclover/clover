package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.eclipse.jdt.core.IMethod;
import org.openclover.core.api.registry.TestCaseInfo;


public interface TestCaseNodeFactory {
    public TestCaseNode newNode(TestCaseInfo tci, IMethod method);
}
