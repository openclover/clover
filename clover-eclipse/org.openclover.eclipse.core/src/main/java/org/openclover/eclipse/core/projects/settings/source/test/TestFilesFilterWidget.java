package org.openclover.eclipse.core.projects.settings.source.test;

import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public class TestFilesFilterWidget extends Composite {
    private Tree sourceDirsTree;
    private TreeViewer sourceDirsTreeViewer;
    private Composite filterGroup;
    private Button addButton;
    private Button editButton;
    private Button removeButton;
    private TestSourcesElement testPackageRoots;

    public TestFilesFilterWidget(ProjectSettings properties, Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(2, false));

        testPackageRoots = getTestPackageRoots(properties);

        sourceDirsTree = new Tree(this, SWT.BORDER);
        sourceDirsTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        sourceDirsTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {

            }
        });
        sourceDirsTreeViewer = new TreeViewer(sourceDirsTree);
        sourceDirsTreeViewer.setAutoExpandLevel(4);
        sourceDirsTreeViewer.setContentProvider(new TestPackageRootContentProvider());
        sourceDirsTreeViewer.setLabelProvider(new TestPackageRootLabelProvider());
        sourceDirsTreeViewer.setInput(testPackageRoots);

        filterGroup = new Composite(this, SWT.NONE);
        filterGroup.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        filterGroup.setLayout(new GridLayout(1, true));

        addButton = new Button(filterGroup, SWT.NONE);
        addButton.setText("Add Folder");
        addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                addTestSourceFolder();
            }
        });

        editButton = new Button(filterGroup, SWT.NONE);
        editButton.setText("Edit Folder");
        editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                editSelectedTestSourceFolder();
            }
        });

        removeButton = new Button(filterGroup, SWT.NONE);
        removeButton.setText("Remove Folder");
        removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                removeSelectedTestSourceFolder();
            }
        });
    }

    private void addTestSourceFolder() {
        TestPackageRootDialog dialog = new TestPackageRootDialog(getShell());
        dialog.open();
    }

    private void removeSelectedTestSourceFolder() {

    }

    private void editSelectedTestSourceFolder() {

    }

    private TestSourcesElement getTestPackageRoots(ProjectSettings properties) {
        TestSourcesElement testPackageRoots = new TestSourcesElement();

        TestPackageRootElement testPackageRoot = testPackageRoots.newTestPackageRoot("foo/bar");
        TestClassElement testClass = testPackageRoot.getExpression().newTestClassElement();
        testClass.setName("Foo");
        testClass.setPackage("com\\.foo\\.bar");
        testClass.setAnnotation("Test");
        testClass.setSuper("TestCase");

        testClass.getTestMethods().newTestMethod(null, "FooAnno", null);
        testClass.getTestMethods().newTestMethod("Bar", null, null);

        testClass = testPackageRoot.getExpression().newTestClassElement();
        testClass.getTestMethods().newTestMethod(null, null, null);

        return testPackageRoots;
    }

    private void setTestPackageRoots(ProjectSettings properties) {

    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        sourceDirsTree.setEnabled(enabled);
        filterGroup.setEnabled(enabled);
    }

    public final class TestPackageRootContentProvider implements ITreeContentProvider {
        private TestSourcesElement input;

        @Override
        public Object[] getElements(Object object) {
            if (hasChildren(object)) {
                return getChildren(object);
            } else {
                return new Object[]{};
            }
        }

        @Override
        public Object getParent(Object object) {
            if (object instanceof TreeElement) {
                return ((TreeElement) object).getParent();
            } else {
                return null;
            }
        }

        @Override
        public Object[] getChildren(Object object) {
            if (object == input) {
                return input.getTestPackageRoots().toArray();
            } else if (object instanceof TestPackageRootElement) {
                List children = newArrayList();
                children.add(((TestPackageRootElement) object).getIncludes());
                children.add(((TestPackageRootElement) object).getExcludes());
                children.add(((TestPackageRootElement) object).getExpression());
                return children.toArray();
            } else if (object instanceof ExpressionElement) {
                return ((ExpressionElement) object).getTestClasses().toArray();
            } else if (object instanceof TestClassElement) {
                List children = newArrayList();
                children.addAll(((TestClassElement) object).getConditions());
                children.add(((TestClassElement) object).getTestMethods());
                return children.toArray();
            } else if (object instanceof TestMethodsElement) {
                return ((TestMethodsElement) object).getMethods().toArray();
            } else if (object instanceof TestMethodElement) {
                return ((TestMethodElement) object).getConditions().toArray();
            } else {
                return new Object[]{};
            }
        }

        @Override
        public boolean hasChildren(Object object) {
            return
                (object == input
                    || object instanceof TestPackageRootElement
                    || object instanceof ExpressionElement
                    || object instanceof TestClassElement
                    || object instanceof TestMethodsElement
                    || object instanceof TestMethodElement);
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            input = (TestSourcesElement) newInput;
        }

        @Override
        public void dispose() {

        }
    }

    public class TestPackageRootLabelProvider implements ITableLabelProvider {

        @Override
        public Image getColumnImage(Object object, int col) {
            return null;
        }

        @Override
        public String getColumnText(Object object, int col) {
            if (object instanceof TestPackageRootElement) {
                return "Examine source files in \"" + ((TestPackageRootElement) object).getProjectRelativePath() + "\"";
            } else if (object instanceof SourceExcludePatternElement) {
                SourceExcludePatternElement sourceExcludePattern = (SourceExcludePatternElement) object;
                return
                    "Excluded files: (" +
                        (sourceExcludePattern.isEmpty()
                            ? "None"
                            : sourceExcludePattern.isAll()
                            ? "All"
                            : sourceExcludePattern.getPattern()) + ")";
            } else if (object instanceof SourceIncludePatternElement) {
                SourceIncludePatternElement sourceIncludePattern = (SourceIncludePatternElement) object;
                return
                    "Included files: (" +
                        (sourceIncludePattern.isEmpty()
                            ? "None"
                            : sourceIncludePattern.isAll()
                            ? "All"
                            : sourceIncludePattern.getPattern()) + ")";
            } else if (object instanceof OrExpressionElement) {
                return "Examine classes:";
            } else if (object instanceof TestClassElement) {
                return "For classes:";
            } else if (object instanceof NameConditionElement) {
                return "Where the name matches \"" + ((NameConditionElement) object).getValue() + "\"";
            } else if (object instanceof TestClassElement.PackageNameConditionElement) {
                return "Where the package matches \"" + ((TestClassElement.PackageNameConditionElement) object).getValue() + "\"";
            } else if (object instanceof AnnotationNameConditionElement) {
                return "Where one of its annotation's name matches \"" + ((AnnotationNameConditionElement) object).getValue() + "\"";
            } else if (object instanceof TestClassElement.SuperTypeNameConditionElement) {
                return "Where the superclass name matches \"" + ((TestClassElement.SuperTypeNameConditionElement) object).getValue() + "\"";
            } else if (object instanceof TestClassElement.AnyConditionElement) {
                return "Of any kind";
            } else if (object instanceof TestMethodElement.AnyConditionElement) {
                return "Any";
            } else if (object instanceof TestMethodsElement) {
                return "Test methods are defined as any of:";
            } else if (object instanceof TestMethodElement) {
                return "Methods matching all the following conditions:";
            } else if (object instanceof TestMethodElement.ReturnTypeNameConditionElement) {
                return "Where the return type name matches \"" + ((TestMethodElement.ReturnTypeNameConditionElement) object).getValue() + "\"";
            } else {
                return "??";
            }
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        @Override
        public void addListener(ILabelProviderListener listener) {

        }

        @Override
        public void removeListener(ILabelProviderListener iLabelProviderListener) {

        }

        @Override
        public void dispose() {

        }
    }
}
