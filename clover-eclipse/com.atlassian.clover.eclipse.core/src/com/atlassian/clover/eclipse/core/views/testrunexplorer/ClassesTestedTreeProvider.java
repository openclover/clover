package com.atlassian.clover.eclipse.core.views.testrunexplorer;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.BitSetCoverageProvider;
import com.atlassian.clover.CoverageData;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.eclipse.core.projects.model.MetricsScope;
import com.atlassian.clover.eclipse.core.views.nodes.Nodes;
import com.atlassian.clover.eclipse.core.views.testrunexplorer.nodes.TestCaseNode;
import com.atlassian.clover.eclipse.core.views.testrunexplorer.nodes.ClassCoverageContributionNode;
import com.atlassian.clover.eclipse.core.views.testrunexplorer.nodes.CoverageContributionNode;
import com.atlassian.clover.eclipse.core.views.testrunexplorer.nodes.MethodCoverageContributionNode;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import com.atlassian.clover.registry.CoverageDataReceptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.model.WorkbenchContentProvider;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Lists.newLinkedList;
import static clover.com.google.common.collect.Maps.newHashMap;
import static clover.com.google.common.collect.Sets.newHashSet;

public class ClassesTestedTreeProvider
    extends WorkbenchContentProvider
    implements ISelectionChangedListener {

    private TreeViewer treeViewer;
    private Object input;
    private List classes;
    private final Map methods = newHashMap();
    private boolean includeCoverageFromFailedTests = true;

    public ClassesTestedTreeProvider(TreeViewer treeViewer) {
        this.treeViewer = treeViewer;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        super.inputChanged(viewer, oldInput, newInput);
        input = newInput;
        classes = null;
        methods.clear();
    }

    @Override
    public Object[] getElements(Object parent) {
        try {
            if (classes == null) {
                final List testCases =
                    removeFailed(
                        !includeCoverageFromFailedTests,
                        Nodes.collectTestCases(parent, Nodes.TO_TESTCASEINFO));

                final CloverProject selectedTestCloverProject = CloverProject.getFor(asJavaElement(parent).getJavaProject());
                if (selectedTestCloverProject != null && testCases.size() > 0) {
                    classes = collectTestedClassesFor(newLinkedList(), testCases, selectedTestCloverProject);
                } else {
                    classes = Collections.emptyList();
                }
            }
            return classes.toArray();
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to find tested methods for " + parent, e);
        }
        return new Object[] {};
    }

    private List collectTestedClassesFor(final List testedClassInfos, final List testCases, final CloverProject project) throws CoreException {
        final CloverProject[] dependencies = project.getDependencies();
        for (CloverProject dependency : dependencies) {
            collectTestedClassesFor(testedClassInfos, testCases, dependency);
        }

        final CloverDatabase database = project.getModel().getDatabase();
        final FullProjectInfo appOnlyProject = database == null ? null : database.getAppOnlyModel();
        final FullProjectInfo fullProject = database == null ? null : database.getFullModel();

        if (database != null && appOnlyProject != null) {
            final HashSet testCasesSet = newHashSet(testCases);
            final CoverageData data = database.getCoverageData();
            final CoverageDataProvider testHits = new BitSetCoverageProvider(data.getHitsFor(testCasesSet), data);
            final CoverageDataProvider uniqueTestHits = new BitSetCoverageProvider(data.getUniqueHitsFor(testCasesSet), data);
            appOnlyProject.getClasses(new HasMetricsFilter() {
                @Override
                public boolean accept(HasMetrics hm) {
                     final FullClassInfo classInfo = (FullClassInfo) hm;
                    try {
                        final IType clazz = project.getJavaProject().findType(classInfo.getQualifiedName(), (IProgressMonitor)null);
                        if (clazz != null) {
                            //TODO: filter should be in accordance with global context filter
                            maybeAddCoverageContributionNode(
                                NodeBuilder.FOR_CLASSES,
                                classInfo,
                                clazz,
                                classInfo.copy((FullFileInfo)classInfo.getContainingFile(), HasMetricsFilter.ACCEPT_ALL),
                                testHits,
                                uniqueTestHits,
                                testedClassInfos);
                        }
                    } catch (Exception e) {
                        CloverPlugin.logError("Unable to calculate classes tested", e);
                    }

                    //We don't really want to collect, just visit
                    return false;
                }
            });
        }
        return testedClassInfos;
    }

    private List collectTestedMethodsFor(IType javaType, final CoverageDataProvider testHits, final CoverageDataProvider uniqueTestHits) throws CoreException {
        FullClassInfo classInfo = (FullClassInfo)MetricsScope.APP_ONLY.getHasMetricsFor(javaType, FullClassInfo.class);

        List testedMethodInfos = Collections.emptyList();
        if (classInfo != null) {
            testedMethodInfos = (List)methods.get(classInfo);
            if (testedMethodInfos == null) {
                testedMethodInfos = newArrayList();
                methods.put(classInfo, testedMethodInfos);
                final IMethod[] methods = javaType.getMethods();
                if (javaType != null && methods.length > 0) {
                    for (IMethod method : methods) {
                        FullMethodInfo methodInfo = (FullMethodInfo) MetricsScope.APP_ONLY.getHasMetricsFor(method, FullMethodInfo.class);
                        if (methodInfo != null) {
                            maybeAddCoverageContributionNode(
                                    NodeBuilder.FOR_METHODS,
                                    methodInfo,
                                    method,
                                    methodInfo.copy(classInfo),
                                    testHits,
                                    uniqueTestHits,
                                    testedMethodInfos);
                        }
                    }
                }
            }
        }
        return testedMethodInfos;
    }

    private void maybeAddCoverageContributionNode(NodeBuilder builder, HasMetrics hasMetrics, IJavaElement element, HasMetrics hasMetricsCopy, CoverageDataProvider testHits, CoverageDataProvider uniqueTestHits, List testedClassInfos) {
        float elementCoverage = calculateCoverage(hasMetricsCopy, testHits);

        if (elementCoverage > 0f) {
            testedClassInfos.add(
                builder.build(
                    element,
                    elementCoverage,
                    calculateCoverage(hasMetricsCopy, uniqueTestHits),
                    testHits,
                    uniqueTestHits));
        }
    }

    private float calculateCoverage(HasMetrics hasMetricsCopy, CoverageDataProvider testHits) {
        int totalElements = hasMetricsCopy.getMetrics().getNumElements();
        ((CoverageDataReceptor)hasMetricsCopy).setDataProvider(testHits);
        if (totalElements == 0) {
            if (hasMetricsCopy instanceof MethodInfo) {
                FullMethodInfo method = (FullMethodInfo)hasMetricsCopy;
                return method.getHitCount() == 0 ? 0f : 1.0f;
            } else {
                return 0f;
            }
        } else {
            return ((float)hasMetricsCopy.getMetrics().getNumCoveredElements() / (float)totalElements);
        }
    }

    private List<MethodInfo> getTestMethods(List<TestCaseInfo> testCases, CloverDatabase database) {
        final List<MethodInfo> testCaseMethods = newLinkedList();
        for (TestCaseInfo testInTestProject : testCases) {
            TestCaseInfo testInDependentProject = database.getTestCase(testInTestProject.getId().intValue());
            if (testInDependentProject != null && testInDependentProject.getSourceMethod() != null) {
                testCaseMethods.add(testInDependentProject.getSourceMethod());
            }
        }
        return testCaseMethods;
    }

    private List removeFailed(boolean doIt, List testCaseInfos) {
        if (doIt) {
            for(Iterator iter = testCaseInfos.iterator(); iter.hasNext();) {
                if (!((TestCaseInfo)iter.next()).isSuccess()) {
                    iter.remove();
                }
            }
        }
        return testCaseInfos;
    }

    private IJavaElement asJavaElement(Object element) {
        if (element instanceof IJavaElement) {
            return (IJavaElement)element;
        } else if (element instanceof TestCaseNode) {
            return ((TestCaseNode)element).toJavaElement();
        } else if (element instanceof IAdaptable) {
            return (IJavaElement)((IAdaptable)element).getAdapter(IJavaElement.class);
        } else {
            return null;
        }
    }

    @Override
    public Object[] getChildren(Object o) {
        try {
            if (o instanceof ClassCoverageContributionNode) {
                ClassCoverageContributionNode classContributionNode = (ClassCoverageContributionNode)o;
                return collectTestedMethodsFor((IType)classContributionNode.getElement(), classContributionNode.getTestHits(), classContributionNode.getUniqueTestHits()).toArray();
            }
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to calculate methods tested", e);
        }
        return new Object[] {};
    }

    @Override
    public boolean hasChildren(Object o) {
        return getChildren(o).length > 0;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        if (event.getSelection() instanceof IStructuredSelection) {
            final Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    //We support any Java element or any TestCaseNodes (which wraps
                    //IMethod, itself being a Java element)
                    treeViewer.setInput(
                        (asJavaElement(selection) == null)
                            ? null
                            : selection);
                }
            });
        }
    }

    private interface NodeBuilder {
        CoverageContributionNode build(IJavaElement element, float testContribution, float uniqueTestContribution, CoverageDataProvider testHits, CoverageDataProvider uniqueTestHits);

        public static NodeBuilder FOR_CLASSES = new NodeBuilder() {
            @Override
            public CoverageContributionNode build(IJavaElement element, float testContribution, float uniqueTestContribution, CoverageDataProvider testHits, CoverageDataProvider uniqueTestHits) {
                return new ClassCoverageContributionNode((IType)element, testContribution, uniqueTestContribution, testHits, uniqueTestHits);
            }
        };

        public static NodeBuilder FOR_METHODS = new NodeBuilder() {
            @Override
            public CoverageContributionNode build(IJavaElement element, float testContribution, float uniqueTestContribution, CoverageDataProvider testHits, CoverageDataProvider uniqueTestHits) {
                return new MethodCoverageContributionNode((IMethod)element, testContribution, uniqueTestContribution, testHits, uniqueTestHits);
            }
        };
    }
}
