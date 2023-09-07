package org.openclover.eclipse.core.views.testcontributions;

import com.atlassian.clover.api.registry.ClassInfo;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.model.ModelUtils;
import org.openclover.eclipse.core.views.actions.OpenJavaEditorAction;
import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistable;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

public abstract class TestContributionsWidget
    extends Composite
    implements IDoubleClickListener, IPersistable {
    
    protected Tree tree;
    protected TreeViewer testsViewer;
    protected ITextEditor editor;
    protected SelectionDispatchAction openAction;
    protected TestContributionsComputation testContributionsComputation;
    protected TestCaseInfoProvider provider;
    protected TestContributionsWidgetSettings settings;
    protected ViewPart viewPart;

    public TestContributionsWidget(TestContributionsWidgetSettings settings, ViewPart viewPart, Composite parent, int style, boolean showLabel, int treeStyle) {
        super(parent, style);
        this.settings = settings;
        this.viewPart = viewPart;

        setLayout(new GridLayout(1, false));

        if (showLabel) {
            Label label = new Label(this, SWT.NONE);
            label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            label.setText(getLabelTitle());
        }

        createTree(treeStyle);

        openAction = new OpenJavaEditorAction(viewPart.getSite());

        testsViewer = createTreeViewer();

        testContributionsComputation = createContributionsComputation();

        provider = createProvider();

        testsViewer.setContentProvider(provider);
        testsViewer.setLabelProvider(createLabelProvider());
        testsViewer.setAutoExpandLevel(3);
        testsViewer.addDoubleClickListener(this);
        testsViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object object1, Object object2) {
                int result = 0;

                if (object1 instanceof ClassInfo && object2 instanceof ClassInfo) {
                    result =
                        ((ClassInfo)object1).getQualifiedName().compareTo(
                            ((ClassInfo)object2).getQualifiedName());
                }
                else if (object1 instanceof TestCaseInfo && object2 instanceof TestCaseInfo) {
                    result =
                        FixedSourceRegion.SOURCE_ORDER_COMP.compare(
                            ((TestCaseInfo)object1).getSourceMethod(),
                            ((TestCaseInfo)object2).getSourceMethod());
                } else {
                    //This shouldn't happen (as sorting is done on a column-by-column basis
                    //so let's not bother too much, heh?
                    result = 0;
                }
                return (TestContributionsWidget.this.settings.isReverseSort() ? -1 : 1) * result;
            }
        });
    }

    protected TestCaseInfoProvider createProvider() {
        return new TestCaseInfoProvider();
    }

    protected TestCaseInfoLabelProvider createLabelProvider() {
        return new TestCaseInfoLabelProvider();
    }

    @Override
    public void saveState(IMemento memento) {
        settings.saveState(memento);
    }

    public void setEditor(ITextEditor editor) {
        this.editor = editor;
    }

    public IJavaProject getJavaProjectForEditor() {
        IJavaElement element =
            editor == null
                ? null
                : JavaUI.getEditorInputJavaElement(editor.getEditorInput());
        return element == null ? null : element.getJavaProject();

    }

    protected void jumpToSource(IStructuredSelection selection) {
        try {
            Object cloverElementSelected = selection.getFirstElement();

            //TODO: what if the test case comes from another project, not the project of the
            //current source file?
            IJavaProject javaProject = getJavaProjectForEditor();

            IJavaElement javaElement = null;
            if (cloverElementSelected instanceof ClassInfo) {
                javaElement =
                    ModelUtils.findTestCaseClass(ModelUtils.SEARCH_SELF_AND_DEPENDENTS, javaProject, (ClassInfo)cloverElementSelected);
            } else if (cloverElementSelected instanceof TestCaseInfo) {
                javaElement =
                    ModelUtils.findTestCaseMethod(ModelUtils.SEARCH_SELF_AND_DEPENDENTS, javaProject, (TestCaseInfo)cloverElementSelected);
            }

            if (javaElement != null) {
                openAction.run(new StructuredSelection(javaElement));
            }
        } catch (JavaModelException e) {
            CloverPlugin.logError("Unable to open test case class/method", e);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        tree.setEnabled(enabled);
    }

    protected abstract String getLabelTitle();

    protected void createTree(int treeStyle) {
        tree = new Tree(this, treeStyle);
        tree.setHeaderVisible(true);
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));

        final TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText("Test");
        column.setWidth(settings.getColumnSize(0));
        tree.setSortColumn(column);
        tree.setSortDirection(settings.isReverseSort() ? SWT.UP : SWT.DOWN);
        column.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                settings.sortOn(0);
                tree.setSortColumn(column);
                tree.setSortDirection(settings.isReverseSort() ? SWT.UP : SWT.DOWN);
                testsViewer.refresh();
            }
        });
    }

    protected abstract TreeViewer createTreeViewer();

    protected abstract TestContributionsComputation createContributionsComputation();

    @Override
    public void doubleClick(DoubleClickEvent event) {
        if (!event.getSelection().isEmpty() && event.getSelection() instanceof IStructuredSelection) {
            jumpToSource((IStructuredSelection)event.getSelection());
        }
    }
}
