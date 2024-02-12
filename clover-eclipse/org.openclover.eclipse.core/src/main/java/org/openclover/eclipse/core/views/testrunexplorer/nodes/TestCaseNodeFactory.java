package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.eclipse.jdt.core.IMethod;
import org.openclover.core.registry.entities.TestCaseInfo;

public interface TestCaseNodeFactory {
    public TestCaseNode newNode(TestCaseInfo tci, IMethod method);
}
