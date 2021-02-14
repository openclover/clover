package org.openclover.eclipse.core.views.testcontributions;

import com.atlassian.clover.api.registry.ClassInfo;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotationModel;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotationFilter;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.IMemento;

import java.util.Set;

import static clover.com.google.common.collect.Sets.newHashSet;

public class AllTestContributionsFilterWidget
    extends TestContributionsWidget
    implements ICheckStateListener {

    private static final int SYNC_DELAY_MS = 1500;

    public AllTestContributionsFilterWidget(Settings settings, ViewPart viewPart, Composite parent, int style, int treeStyle, boolean showLabel) {
        super(settings, viewPart, parent, style, showLabel, treeStyle);
        getTestsViewer().addCheckStateListener(this);
    }

    @Override
    protected TestCaseInfoProvider createProvider() {
        return new AllTestCaseInfoProvider();
    }

    @Override
    protected TestCaseInfoLabelProvider createLabelProvider() {
        return new AllTestCaseInfoLabelProvider();
    }

    @Override
    protected TestContributionsComputation createContributionsComputation() {
        final AllTestContributionsComputation testContributionsComputation = new AllTestContributionsComputation();
        testContributionsComputation.addJobChangeListener(
            new JobChangeAdapter() {

                @Override
                public void done(IJobChangeEvent event) {
                    if (event.getResult().getSeverity() != Status.CANCEL) {
                        Display.getDefault().syncExec(new Runnable() {
                            @Override
                            public void run() {
                                //Content provider may be null when shutting down
                                //So do no work in that case (as it will fail anyway)
                                if (getTestsViewer().getContentProvider() != null) {
                                    if (!viewPart.getViewSite().getPage().isPartVisible(viewPart)) {
                                        testContributionsComputation.prime(null);
                                    } else {
                                        if (testContributionsComputation.isInputChanged()) {
                                            getTestsViewer().setInput(testContributionsComputation);

                                            Set checkedElements = testContributionsComputation.getCheckedTestCasesAndClasses();
                                            if (checkedElements.size() == 1
                                                && checkedElements.iterator().next() == AllTestCaseInfoProvider.ALL_TEST_CASES) {
                                                getTestsViewer().setGrayChecked(AllTestCaseInfoProvider.ALL_TEST_CASES, true);
                                            } else {
                                                getTestsViewer().setGrayChecked(AllTestCaseInfoProvider.ALL_TEST_CASES, false);
                                                getTestsViewer().setCheckedElements(checkedElements.toArray());
                                            }
                                        }
                                        testContributionsComputation.prime(editor);
                                    }
                                }
                            }
                        });

                        testContributionsComputation.schedule(SYNC_DELAY_MS);
                    }
                }
            });
        return testContributionsComputation;
    }

    @Override
    protected TreeViewer createTreeViewer() {
        return new CheckboxTreeViewer(tree);
    }

    @Override
    protected void createTree(int treeStyle) {
        super.createTree(treeStyle | SWT.CHECK);
    }

    @Override
    protected String getLabelTitle() {
        return "All contributing tests:";
    }

    public void startTrackingEditor() {
        testContributionsComputation.prime(editor);
        testContributionsComputation.schedule(SYNC_DELAY_MS);
    }

    public void stopTrackingEditor() {
        testContributionsComputation.cancel();
        try {
            testContributionsComputation.join();
        } catch (InterruptedException e) {
            CloverPlugin.logError("Failed to stop cursor listing job");
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        stopTrackingEditor();
    }

    public Tree getTree() {
        return tree;
    }

    private CheckboxTreeViewer getTestsViewer() {
        return (CheckboxTreeViewer) testsViewer;
    }

    @Override
    public void checkStateChanged(CheckStateChangedEvent event) {
        CoverageAnnotationModel model = CoverageAnnotationModel.getModel(editor);

        if (model != null) {
            setCheckedFrom(event);
            updateGrayedOut();
            model.setExcludedTests(calculateFilter());
        }
    }

    private void setCheckedFrom(CheckStateChangedEvent event) {
        Set<TestCaseInfo> allTestCases = provider.getAllTests();

        if (event.getElement() == AllTestCaseInfoProvider.ALL_TEST_CASES) {
            Set<ClassInfo> classes = newHashSet();
            boolean grayed = getTestsViewer().getGrayed(event.getElement());

            for (ClassInfo aClass : classes) {
                getTestsViewer().setChecked(aClass, !grayed);
            }

            for (TestCaseInfo testCase : allTestCases) {
                classes.add(testCase.getRuntimeType());
                if (event.getChecked()) {
                    getTestsViewer().setChecked(testCase, !grayed);
                } else {
                    getTestsViewer().setChecked(testCase, grayed);
                }
            }
        } else if (event.getElement() instanceof ClassInfo) {
            ClassInfo classInfo = (ClassInfo) event.getElement();

            boolean grayed = getTestsViewer().getGrayed(event.getElement());
            for (TestCaseInfo testCase : allTestCases) {
                if (testCase.getRuntimeType() == classInfo) {
                    if (event.getChecked()) {
                        getTestsViewer().setChecked(testCase, !grayed);
                    } else {
                        getTestsViewer().setChecked(testCase, grayed);
                    }
                }
            }

        } else if (event.getElement() instanceof TestCaseInfo) {
            TestCaseInfo testCase = (TestCaseInfo) event.getElement();
            if (event.getChecked()) {
                getTestsViewer().setChecked(testCase, true);
            } else {
                getTestsViewer().setChecked(testCase, false);
            }
        }
    }

    private void updateGrayedOut() {
        final TreeItem[] items = tree.getItems();
        for (final TreeItem item : items) {
            grayOut(item);
        }
    }

    private static class TreeItemState {
        private static final TreeItemState CHECKED = new TreeItemState();
        private static final TreeItemState UNCHECKED = new TreeItemState();
        private static final TreeItemState GRAYED = new TreeItemState();

        public TreeItemState and(TreeItemState other) {
            return
                other == null
                    ? this
                    : (this == CHECKED && other == CHECKED)
                    ? CHECKED
                    : (this == UNCHECKED && other == UNCHECKED)
                    ? UNCHECKED
                    : GRAYED;
        }

        public void applyOn(TreeItem item) {
            if (this == CHECKED) {
                item.setChecked(true);
                item.setGrayed(false);
            } else if (this == UNCHECKED) {
                item.setChecked(false);
                item.setGrayed(false);
            } else {
                item.setChecked(true);
                item.setGrayed(true);
            }
        }

        public static TreeItemState getFor(TreeItem item) {
            if (item.getGrayed()) {
                return GRAYED;
            } else {
                return
                    item.getChecked()
                        ? CHECKED
                        : UNCHECKED;
            }
        }
    }

    private TreeItemState grayOut(TreeItem item) {
        TreeItem[] items = item.getItems();
        if (items.length > 0) {
            TreeItemState state = null;

            for (TreeItem item1 : items) {
                state = grayOut(item1).and(state);
            }
            state.applyOn(item);

            return state;
        } else {
            return TreeItemState.getFor(item);
        }
    }


    private CoverageAnnotationFilter.TestFilter calculateFilter() {
        CoverageAnnotationFilter.TestFilter filter = new CoverageAnnotationFilter.TestFilter();
        for (TreeItem item : tree.getItems()) {
            calculateFilter(filter, item);
        }
        return filter;
    }

    private CoverageAnnotationFilter.TestFilter calculateFilter(CoverageAnnotationFilter.TestFilter filter, TreeItem treeItem) {
        TreeItem[] items = treeItem.getItems();
        for (TreeItem item : items) {
            if (item.getData() instanceof ClassInfo) {
                if (!item.getChecked() && !item.getGrayed()) {
                    filter.addTestClass((ClassInfo) item.getData());
                } else {
                    calculateFilter(filter, item);
                }
            } else if (item.getData() instanceof TestCaseInfo) {
                if (!item.getChecked()) {
                    filter.addTestCase(((TestCaseInfo) item.getData()));
                }
            } else {
                calculateFilter(filter, item);
            }
        }

        return filter;
    }

    public static class Settings extends TestContributionsWidgetSettings {
        public Settings(IMemento memento) {
            super(memento);
        }

        public Settings() {
            super();
        }

        @Override
        public String prefix(String base) {
            return "all.";
        }
    }
}
