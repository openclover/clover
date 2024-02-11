package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import org.openclover.core.registry.entities.TestCaseInfo;
import org.eclipse.jdt.core.IMethod;

public interface TestCaseNodeFactory {
    public TestCaseNode newNode(TestCaseInfo tci, IMethod method);
}
