package org.openclover.eclipse.core.views.testrunexplorer.nodes;

import com.atlassian.clover.registry.entities.FullClassInfo;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.views.nodes.NodeRelationship;
import org.openclover.eclipse.core.views.nodes.NodeRelationshipFilter;
import org.openclover.eclipse.core.views.nodes.Nodes;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import java.util.List;

import static clover.com.google.common.collect.Lists.newLinkedList;

public class TypeToTestInnerTypeAndTestMethodRelationship extends NodeRelationship {

    private TestCaseNodeFactory tcnFactory;

    public TypeToTestInnerTypeAndTestMethodRelationship(TestCaseNodeFactory tcnFactory) {
        this.tcnFactory = tcnFactory;
    }

    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            //Ignore all but inner-type test class definitions and test methods (e.g. no imports, package statements)
            final IType[] types = ((IType) object).getTypes();
            final List<IType> children = newLinkedList();
            for (IType type : types) {
                if (Nodes.containsTestCases(type)) {
                    children.add(type);
                }
            }

            return
                filter.perform(
                    Nodes.collectTestCases(
                        (IType)object,
                        children,
                        new Nodes.ToTestCaseNodeCoverter(tcnFactory)));
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to collect inner-types in type " + object, e);
            return new Object[] {};
        }
    }


    @Override
    public Boolean hasChildren(Object object, NodeRelationshipFilter filter) {
        try {
            //Ignore all but inner-type test class definitions and methods

            IType[] types = ((IType) object).getTypes();
            IMethod[] methods = ((IType) object).getMethods();

            for (IType type : types) {
                FullClassInfo hasMetricsType = (FullClassInfo) MetricsScope.TEST_ONLY.getHasMetricsFor(type, FullClassInfo.class);
                if (hasMetricsType != null && hasMetricsType.isTestClass()) {
                    return Boolean.TRUE;
                }
            }

            for (IMethod method : methods) {
                TestCaseInfo testCaseInfo = MetricsScope.TEST_ONLY.getTestCaseInfoFor(method);
                if (testCaseInfo != null) {
                    return Boolean.TRUE;
                }
            }

        } catch (JavaModelException e) {
            CloverPlugin.logError("Unable to count inner-types in type " + object, e);
        }
        return Boolean.FALSE;
    }

    @Override
    public boolean includes(Object object) {
        return object instanceof IType;
    }
}
