package org.openclover.eclipse.core.views.nodes;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.swt.graphics.Image;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.metrics.HasMetricsNode;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.model.JavaElementAdapter;
import org.openclover.eclipse.core.projects.model.MetricsScope;
import org.openclover.eclipse.core.ui.CloverPluginIcons;
import org.openclover.eclipse.core.views.testrunexplorer.nodes.TestCaseNode;
import org.openclover.eclipse.core.views.testrunexplorer.nodes.TestCaseNodeFactory;

import java.util.List;
import java.util.Set;

import static org.openclover.core.util.Lists.newLinkedList;

public class Nodes {
    public static final TestCaseConverter TO_TESTCASEINFO = (tci, method) -> tci;

    public static final TestCaseConverter TO_IMETHOD = (tci, method) -> method;

    public static class ToTestCaseNodeCoverter implements TestCaseConverter {
        private TestCaseNodeFactory tcnFactory;

        public ToTestCaseNodeCoverter(TestCaseNodeFactory tcnFactory) {
            this.tcnFactory = tcnFactory;
        }

        @Override
        public Object convert(TestCaseInfo tci, IMethod method) {
            return tcnFactory.newNode(tci, method);
        }
    };

    public static final TypeCondition CONTAINS_TEST_CASE = new TypeCondition() {
        @Override
        public boolean evaluate(IType type) throws CoreException {
            FullClassInfo classInfo = (FullClassInfo) MetricsScope.TEST_ONLY.getHasMetricsFor(type, FullClassInfo.class);
            if (classInfo != null && classInfo.isTestClass()) {
                if (classInfo.getTestCases().size() > 0) {
                    return true;
                } else {
                    IType[] types = type.getTypes();
                    for (IType t : types) {
                        if (evaluate(t)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public Inference inferFor(IProject element) throws CoreException {
            final CloverProject cloverProject = CloverProject.getFor(element);
            final ProjectInfo project = cloverProject == null ? null : MetricsScope.TEST_ONLY.getProjectInfoFor(cloverProject);
            return (project == null || !project.hasTestResults()) ? Inference.FALSE : Inference.TRUE;
        }
    };

    public static final TypeCondition IS_TEST_CLASS_CONTAINER = new TypeCondition() {
        @Override
        public boolean evaluate(IType type) throws CoreException {
            return MetricsScope.TEST_ONLY.getHasMetricsFor(type, FullClassInfo.class) != null;
        }

        @Override
        public Inference inferFor(IProject element) throws CoreException {
            final CloverProject cloverProject = CloverProject.getFor(element);
            final ProjectInfo project = cloverProject == null ? null : MetricsScope.TEST_ONLY.getProjectInfoFor(cloverProject);
            return (project == null || project.isEmpty()) ? Inference.FALSE : Inference.TRUE;
        }

        @Override
        public Inference inferFor(IPackageFragment element) throws CoreException {
            final CloverProject cloverProject = CloverProject.getFor(element.getJavaProject());
            final HasMetrics hasMetrics = cloverProject == null ? null : MetricsScope.TEST_ONLY.getHasMetricsFor(element);
            final HasMetricsNode hasMetricsNode = hasMetrics instanceof HasMetricsNode ? (HasMetricsNode)hasMetrics : null;
            return (hasMetricsNode == null || hasMetricsNode.isEmpty()) ? Inference.FALSE : Inference.TRUE;
        }
    };

    private static final TypeCondition IS_APP_CLASS_CONTAINER = new TypeCondition() {
        @Override
        public boolean evaluate(IType type) throws CoreException {
            return MetricsScope.APP_ONLY.getHasMetricsFor(type, FullClassInfo.class) != null;
        }

        @Override
        public Inference inferFor(IProject element) throws CoreException {
            final CloverProject cloverProject = CloverProject.getFor(element);
            final ProjectInfo project = cloverProject == null ? null : MetricsScope.APP_ONLY.getProjectInfoFor(cloverProject);
            return (project == null || project.isEmpty()) ? Inference.FALSE : Inference.TRUE;
        }

        @Override
        public Inference inferFor(IPackageFragment element) throws CoreException {
            final CloverProject cloverProject = CloverProject.getFor(element.getJavaProject());
            final HasMetrics hasMetrics = cloverProject == null ? null : MetricsScope.APP_ONLY.getHasMetricsFor(element);
            final HasMetricsNode hasMetricsNode = hasMetrics instanceof HasMetricsNode ? (HasMetricsNode)hasMetrics : null;
            return (hasMetricsNode == null || hasMetricsNode.isEmpty()) ? Inference.FALSE : Inference.TRUE;
        }
    };

    public interface TestCaseConverter {
        public Object convert(TestCaseInfo tci, IMethod method);
    }

    public static abstract class TypeCondition {
        public abstract boolean evaluate(IType type) throws CoreException;
        /** An optimization to avoid walking the type tree if we have a high-level way of evaluating. */
        public Inference inferFor(IProject element) throws CoreException { return Inference.UNKNOWN; };
        public Inference inferFor(IPackageFragmentRoot element) throws CoreException { return Inference.UNKNOWN; };
        public Inference inferFor(IPackageFragment element) throws CoreException { return Inference.UNKNOWN; };
        public Inference inferFor(TreePackageFragmentNode element) throws CoreException { return Inference.UNKNOWN; };
        public Inference inferFor(LeafPackageFragmentNode element) throws CoreException { return Inference.UNKNOWN; };
    }

    private enum Inference {
        UNKNOWN {
            @Override
            public boolean result() {
                return false;
            }},
        TRUE {
            @Override
            public boolean result() {
                return true;
            }},
        FALSE {
            @Override
            public boolean result() {
                return false;
            }};

        public abstract boolean result();
    }

    public static List collectTestCases(Object mysteryMeat, TestCaseConverter converter) throws CoreException {
        List testCases = newLinkedList();
        if (mysteryMeat instanceof IProject) {
            CloverProject cloverProject = CloverProject.getFor((IProject) mysteryMeat);
            if (cloverProject != null) {
                return collectTestCases(cloverProject.getJavaProject(), testCases, converter);
            }
        } else if (mysteryMeat instanceof IJavaProject) {
            return collectTestCases((IJavaProject)mysteryMeat, testCases, converter);
        } else if (mysteryMeat instanceof IPackageFragmentRoot) {
            return collectTestCases((IPackageFragmentRoot)mysteryMeat, testCases, converter);
        } else if (mysteryMeat instanceof IPackageFragment) {
            return collectTestCases((IPackageFragment)mysteryMeat, testCases, converter);
        } else if (mysteryMeat instanceof LeafPackageFragmentNode) {
            return collectTestCases((LeafPackageFragmentNode)mysteryMeat, testCases, converter);
        } else if (mysteryMeat instanceof TreePackageFragmentNode) {
            return collectTestCases((TreePackageFragmentNode)mysteryMeat, testCases, converter);
        } else if (mysteryMeat instanceof ICompilationUnit) {
            return collectTestCases((ICompilationUnit)mysteryMeat, testCases, converter);
        } else if (mysteryMeat instanceof IType) {
            return collectTestCases((IType)mysteryMeat, testCases, converter);
        } else if (mysteryMeat instanceof IMethod) {
            return collectTestCases((IMethod)mysteryMeat, testCases, converter);
        } else if (mysteryMeat instanceof TestCaseNode) {
            return collectTestCases((TestCaseNode)mysteryMeat, testCases, converter);
        }
        return testCases;
    }

    private static List collectTestCases(LeafPackageFragmentNode leafPackageFragmentNode, List testCases, TestCaseConverter converter) throws CoreException {
        Set<? extends IPackageFragment> packageFragments = leafPackageFragmentNode.getPackageFragments();
        for (IPackageFragment packageFragment : packageFragments) {
            collectTestCases(packageFragment, testCases, converter);
        }
        return testCases;
    }

    private static List collectTestCases(TreePackageFragmentNode treePackageFragmentNode, List testCases, TestCaseConverter converter) throws CoreException {
        IPackageFragment packageFragment = (IPackageFragment)treePackageFragmentNode.toJavaElement();
        packageFragment =
            packageFragment instanceof JavaElementAdapter
                ? (IPackageFragment)((JavaElementAdapter)packageFragment).getAdaptee()
                : packageFragment;
        collectTestCases(packageFragment, testCases, converter);

        if (!packageFragment.isDefaultPackage()) {
            final IJavaElement[] packages = ((IPackageFragmentRoot)packageFragment.getParent()).getChildren();
            for (IJavaElement aPackage : packages) {
                if (aPackage instanceof IPackageFragment
                        && aPackage.getElementName().indexOf(packageFragment.getElementName() + ".") == 0) {
                    collectTestCases((IPackageFragment) aPackage, testCases, converter);
                }
            }
        }
        return testCases;
    }

    public static List collectTestCases(IJavaProject project, List testCases, TestCaseConverter converter) throws CoreException {
        final CloverProject cloverProject = CloverProject.getFor(project);
        final ProjectInfo projectInfo = cloverProject.getModel().getTestOnlyProjectInfo();
        if (projectInfo != null && projectInfo.hasTestResults()) {
            IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();

            for (IPackageFragmentRoot root : roots) {
                if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    collectTestCases(root, testCases, converter);
                }
            }
        }
        return testCases;
    }

    public static List collectTestCases(IProject project, List testCases, TestCaseConverter converter) throws CoreException {
        CloverProject cloverProject = CloverProject.getFor(project);
        if (cloverProject != null) {
            return collectTestCases(cloverProject.getJavaProject(), testCases, converter);
        } else {
            return testCases;
        }
    }

    public static List collectTestCases(IPackageFragmentRoot root, List testCases, TestCaseConverter converter) throws CoreException {
        IJavaElement[] children = root.getChildren();
        for (IJavaElement child : children) {
            if (child instanceof IPackageFragment) {
                collectTestCases((IPackageFragment) child, testCases, converter);
            }
        }
        return testCases;
    }

    public static List collectTestCases(IPackageFragment packageFragment, List testCases, TestCaseConverter converter) throws CoreException {
        ICompilationUnit[] cus = packageFragment.getCompilationUnits();
        for (ICompilationUnit cu : cus) {
            collectTestCases(cu, testCases, converter);
        }
        return testCases;
    }

    public static List collectTestCases(ICompilationUnit cu, List testCases, TestCaseConverter converter) throws CoreException {
        IType[] types = cu.getTypes();
        for (IType type : types) {
            collectTestCases(type, testCases, converter);
        }
        return testCases;
    }

    public static List collectTestCases(IType type, List testCases, TestCaseConverter converter) throws CoreException {
        FullClassInfo classInfo = (FullClassInfo) MetricsScope.TEST_ONLY.getHasMetricsFor(type, FullClassInfo.class);
        if (classInfo != null && classInfo.isTestClass()) {
            IMethod[] methods = type.getMethods();
            for (IMethod method : methods) {
                collectTestCases(method, testCases, converter);
            }
            IType[] innerTypes = type.getTypes();
            for (IType innerType : innerTypes) {
                collectTestCases(innerType, testCases, converter);
            }
        }
        return testCases;
    }

    public static List collectTestCases(IMethod method, List testCases, TestCaseConverter converter) {
        TestCaseInfo[] tcis = MetricsScope.TEST_ONLY.getTestCaseInfosFor(method);
        if (tcis != null) {
            for (TestCaseInfo tci : tcis) {
                testCases.add(converter.convert(tci, method));
            }
        }
        return testCases;
    }

    private static List collectTestCases(TestCaseNode testCaseNode, List testCases, TestCaseConverter converter) {
        IMethod method = testCaseNode.getTestMethod();
        TestCaseInfo[] tcis = MetricsScope.TEST_ONLY.getTestCaseInfosFor(method);
        if (tcis != null) {
            for (TestCaseInfo tci : tcis) {
                if (tci.getId() == testCaseNode.getTciId()) {
                    testCases.add(converter.convert(tci, method));
                    //There can only be one test that maps to a test ID
                    break;
                }
            }
        }
        return testCases;
    }

    public static boolean containsTestCases(Object mysteryMeat) throws CoreException {
        return (mysteryMeat instanceof TestCaseNode || evalOnTypes(CONTAINS_TEST_CASE, mysteryMeat));
    }

    public static boolean isTestClassContainer(Object mysteryMeat) throws CoreException {
        return evalOnTypes(IS_TEST_CLASS_CONTAINER, mysteryMeat);
    }

    public static boolean isAppClassContainer(Object mysterMeat) throws CoreException {
        return evalOnTypes(IS_APP_CLASS_CONTAINER, mysterMeat);
    }

    public static boolean evalOnTypes(TypeCondition condition, Object mysteryMeat) throws CoreException {
        if (mysteryMeat instanceof IProject) {
            return evalOnTypes(condition, (IProject)mysteryMeat);
        } else if (mysteryMeat instanceof IJavaProject) {
            return evalOnTypes(condition, ((IJavaProject)mysteryMeat).getProject());
        } else if (mysteryMeat instanceof IPackageFragmentRoot) {
            return evalOnTypes(condition, (IPackageFragmentRoot)mysteryMeat);
        } else if (mysteryMeat instanceof TreePackageFragmentNode) {
            return evalOnTypes(condition, (TreePackageFragmentNode)mysteryMeat);
        } else if (mysteryMeat instanceof LeafPackageFragmentNode) {
            return evalOnTypes(condition, (LeafPackageFragmentNode)mysteryMeat);
        } else if (mysteryMeat instanceof IPackageFragment) {
            return evalOnTypes(condition, (IPackageFragment)mysteryMeat);
        } else if (mysteryMeat instanceof ICompilationUnit) {
            return evalOnTypes(condition, (ICompilationUnit)mysteryMeat);
        } else if (mysteryMeat instanceof IType) {
            return condition.evaluate((IType)mysteryMeat);
        }
        return false;
    }

    public static boolean containsTestCases(IProject project) throws CoreException {
        return evalOnTypes(CONTAINS_TEST_CASE, project);
    }

    public static boolean containsTestMethods(IProject project) throws CoreException {
        return evalOnTypes(IS_TEST_CLASS_CONTAINER, project);
    }

    public static boolean evalOnTypes(TypeCondition condition, IProject project) throws CoreException {
        final Inference inference = condition.inferFor(project);
        if (inference == Inference.UNKNOWN) {
            final CloverProject cloverProject = CloverProject.getFor(project);
            if (cloverProject != null) {
                final IPackageFragmentRoot[] roots =
                    cloverProject.getJavaProject().getPackageFragmentRoots();

                for (IPackageFragmentRoot root : roots) {
                    if (root.getKind() == IPackageFragmentRoot.K_SOURCE && evalOnTypes(condition, root)) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return inference.result();
        }
    }

    public static boolean evalOnTypes(TypeCondition condition, LeafPackageFragmentNode packageFragmentNode) throws CoreException {
        boolean eval = false;
        for (IPackageFragment packageFragment : packageFragmentNode.getPackageFragments()) {
            eval = eval || evalOnTypes(condition, packageFragment);
            if (eval) {
                break;
            }
        }
        return eval;
    }

    public static boolean evalOnTypes(TypeCondition condition, TreePackageFragmentNode packageFragmentNode) throws CoreException {
        IPackageFragment packageFragment = (IPackageFragment)packageFragmentNode.toJavaElement();
        packageFragment =
            packageFragment instanceof JavaElementAdapter
                ? (IPackageFragment)((JavaElementAdapter)packageFragment).getAdaptee()
                : packageFragment;
        if (evalOnTypes(condition, packageFragment)) {
            return true;
        } else if (!packageFragment.isDefaultPackage()) {
            final IJavaElement[] packages = ((IPackageFragmentRoot)packageFragment.getParent()).getChildren();
            for (IJavaElement aPackage : packages) {
                if (aPackage instanceof IPackageFragment
                        && aPackage.getElementName().indexOf(packageFragment.getElementName() + ".") == 0) {
                    if (evalOnTypes(condition, (IPackageFragment) aPackage)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean containsTestCases(IPackageFragmentRoot root) throws CoreException {
        return evalOnTypes(CONTAINS_TEST_CASE, root);
    }

    public static boolean containsTestMethods(IPackageFragmentRoot root) throws CoreException {
        return evalOnTypes(IS_TEST_CLASS_CONTAINER, root);
    }

    public static boolean evalOnTypes(TypeCondition condition, IPackageFragmentRoot root) throws CoreException {
        IJavaElement[] children = root.getChildren();
        for (IJavaElement child : children) {
            if (child instanceof IPackageFragment && evalOnTypes(condition, (IPackageFragment) child)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsTestCases(IPackageFragment packageFragment) throws CoreException {
        return evalOnTypes(CONTAINS_TEST_CASE, packageFragment);
    }

    public static boolean containsTestMethods(IPackageFragment packageFragment) throws CoreException {
        return evalOnTypes(IS_TEST_CLASS_CONTAINER, packageFragment);
    }

    public static boolean evalOnTypes(TypeCondition condition, IPackageFragment packageFragment) throws CoreException {
        final Inference inference = condition.inferFor(packageFragment);
        if (inference == Inference.UNKNOWN) {
            ICompilationUnit[] cus = packageFragment.getCompilationUnits();
            for (ICompilationUnit cu : cus) {
                if (evalOnTypes(condition, cu)) {
                    return true;
                }
            }
            return false;
        } else {
            return inference.result();
        }
    }

    public static boolean containsTestCases(ICompilationUnit cu) throws CoreException {
        return evalOnTypes(CONTAINS_TEST_CASE, cu);
    }

    public static boolean containsTestMethods(ICompilationUnit cu) throws CoreException {
        return evalOnTypes(IS_TEST_CLASS_CONTAINER, cu);
    }

    public static boolean evalOnTypes(TypeCondition condition, ICompilationUnit cu) throws CoreException {
        IType[] types = cu.getTypes();
        for (IType type : types) {
            if (condition.evaluate(type)) {
                return true;
            }
        }
        return false;
    }

    public static Image iconFor(TestCaseInfo testCaseInfo) {
        if (!testCaseInfo.isHasResult()) {
            return CloverPlugin.getImage(CloverPluginIcons.TEST_ICON);
        } else if (testCaseInfo.isSuccess()) {
            return CloverPlugin.getImage(CloverPluginIcons.TEST_PASS_ICON);
        } else if (testCaseInfo.isFailure()) {
            return CloverPlugin.getImage(CloverPluginIcons.TEST_FAILURE_ICON);
        } else {
            return CloverPlugin.getImage(CloverPluginIcons.TEST_ERROR_ICON);
        }
    }
}