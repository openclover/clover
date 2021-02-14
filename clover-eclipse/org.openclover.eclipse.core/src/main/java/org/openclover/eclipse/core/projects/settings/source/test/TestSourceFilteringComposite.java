package org.openclover.eclipse.core.projects.settings.source.test;

import clover.com.google.common.collect.Sets;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.projects.settings.source.LayoutUtils;
import org.openclover.eclipse.core.ui.GLH;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.projects.settings.ProjectPropertyPage;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.openclover.eclipse.core.projects.settings.source.SourceRootWithPattern;
import org.openclover.eclipse.core.projects.settings.source.SourceRootsWithPatternTreeContentProvider;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TestSourceFilteringComposite extends Composite {
    private final Button testFoldersAllButton;
    private final Button testFoldersSelectedButton;
    private final Button testFoldersNoneButton;
    private final Text testIncludeFilterText;
    private final Text testExcludeFilterText;
    private final CheckboxTableViewer testRootFolders;

    int lastTestSourceFolders;
    String lastInclude;
    String lastExclude;
    List<String> lastSelectedFolders;

    public TestSourceFilteringComposite(Composite parent, IJavaProject project, ProjectSettings properties, SourceRootsWithPatternTreeContentProvider contentProvider) {
        super(parent, SWT.NONE);

        lastTestSourceFolders = properties.getTestSourceFolders();
        lastInclude = properties.getTestIncludeFilter() == null
                ? ProjectSettings.DEFAULT_TEST_INCLUDE_PATTERN : properties.getTestIncludeFilter();
        lastExclude = properties.getTestExcludeFilter() == null
                ? ProjectSettings.DEFAULT_EXCLUDE_PATTERN : properties.getTestExcludeFilter();
        lastSelectedFolders = properties.getSelectedTestFolders();

        Group testSourcesGroup = LayoutUtils.createGroup(this, CloverEclipsePluginMessages.TEST_FOLDERS());
        testSourcesGroup.setLayout(new GLH().standardiseMargin().getGridLayout());
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        testSourcesGroup.setLayoutData(gridData);

        testFoldersAllButton = new Button(testSourcesGroup, SWT.RADIO);
        testFoldersAllButton.setText(CloverEclipsePluginMessages.TEST_FOLDERS_ALL());
        testFoldersAllButton.setSelection(lastTestSourceFolders == ProjectSettings.Values.ALL_FOLDERS);

        final Composite c = new Composite(testSourcesGroup, SWT.NONE);
        c.setLayout(new GridLayout(2, false));
        final GridData gd1 = new GridData(GridData.FILL_HORIZONTAL);
        gd1.horizontalIndent = ProjectPropertyPage.Panel.HORIZONTAL_INDENT;
        c.setLayoutData(gd1);

        final Label includeLabel = new Label(c, SWT.NONE);
        includeLabel.setText(CloverEclipsePluginMessages.FILE_FILTERING_INCLUDE());

        testIncludeFilterText = new Text(c, SWT.BORDER);

        testIncludeFilterText.setText(lastInclude);
        testIncludeFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Label excludeLabel = new Label(c, SWT.NONE);
        excludeLabel.setText(CloverEclipsePluginMessages.FILE_FILTERING_EXCLUDE());

        testExcludeFilterText = new Text(c, SWT.BORDER);
        testExcludeFilterText.setText(lastExclude);
        testExcludeFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));


        testFoldersSelectedButton = new Button(testSourcesGroup, SWT.RADIO);
        testFoldersSelectedButton.setText(CloverEclipsePluginMessages.TEST_FOLDERS_SELECTED());
        testFoldersSelectedButton.setSelection(lastTestSourceFolders == ProjectSettings.Values.SELECTED_FOLDERS);

        testRootFolders = CheckboxTableViewer.newCheckList(testSourcesGroup, SWT.BORDER | SWT.V_SCROLL);
        GridData gd = SwtUtils.gridDataFor(testRootFolders.getControl());
        gd.minimumHeight = 100;
        gd.minimumWidth = 250;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalIndent = ProjectPropertyPage.Panel.HORIZONTAL_INDENT;

        final TestFolderLabelProvider labelProvider =
                new TestFolderLabelProvider(properties.isInstrumentSelectedSourceFolders());

        testRootFolders.setLabelProvider(labelProvider);
        testRootFolders.setContentProvider(contentProvider); // use as source root folders info - not suitable as data model
        testRootFolders.setInput(contentProvider.getProject());

        final Set<String> selectedFolders = Sets.newHashSet(lastSelectedFolders);
        for (SourceRootWithPattern srwp : contentProvider.getChildren(project)) {
            if (selectedFolders.contains(srwp.getPattern().getSrcPath())) {
                testRootFolders.setChecked(srwp, true);
            }
        }
        contentProvider.addChangeListener(labelProvider);

        testFoldersNoneButton = new Button(testSourcesGroup, SWT.RADIO);
        testFoldersNoneButton.setText(CloverEclipsePluginMessages.TEST_FOLDERS_NONE());
        testFoldersNoneButton.setSelection(lastTestSourceFolders == ProjectSettings.Values.NO_TEST_FOLDERS);


        final SelectionAdapter adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final boolean enableAllPart = testFoldersAllButton.getSelection();
                includeLabel.setEnabled(enableAllPart);
                excludeLabel.setEnabled(enableAllPart);
                testIncludeFilterText.setEnabled(enableAllPart);
                testExcludeFilterText.setEnabled(enableAllPart);

                testRootFolders.getControl().setEnabled(testFoldersSelectedButton.getSelection());

            }
        };
        testFoldersSelectedButton.addSelectionListener(adapter);
        testFoldersAllButton.addSelectionListener(adapter);
        adapter.widgetSelected(null);
    }

    public void storeTo(ProjectSettings properties) throws JavaModelException {

        lastTestSourceFolders = getCurrentTestSourceFolders();
        lastInclude = testIncludeFilterText.getText().trim();
        lastExclude = testExcludeFilterText.getText().trim();
        lastSelectedFolders = getCurrentSelectedTestFolders();

        properties.setTestIncludeFilter(lastInclude.length() > 0 ? lastInclude : null);
        properties.setTestExcludeFilter(lastExclude.length() > 0 ? lastExclude : null);
        properties.setTestSourceFolders(lastTestSourceFolders);
        properties.setSelectedTestFolders(lastSelectedFolders);
    }

    private List<String> getCurrentSelectedTestFolders() throws JavaModelException {
        final Object[] selection = testRootFolders.getCheckedElements();
        final List<String> selectedFolders = new ArrayList<String>(selection.length);
        for (Object aSelection : selection) {
            selectedFolders.add(((SourceRootWithPattern) aSelection).getPattern().getSrcPath());
        }
        return selectedFolders;
    }

    private int getCurrentTestSourceFolders() {
        return testFoldersNoneButton.getSelection() ? ProjectSettings.Values.NO_TEST_FOLDERS
                : testFoldersSelectedButton.getSelection() ? ProjectSettings.Values.SELECTED_FOLDERS
                : /*default all*/ ProjectSettings.Values.ALL_FOLDERS;
    }

    public boolean isModified() throws JavaModelException {
        if (lastTestSourceFolders != getCurrentTestSourceFolders()) {
            return true;
        }

        switch (lastTestSourceFolders) {
            case ProjectSettings.Values.ALL_FOLDERS:
                return !(lastInclude.equals(testIncludeFilterText.getText().trim())
                        && lastExclude.equals(testExcludeFilterText.getText().trim()));
            case ProjectSettings.Values.SELECTED_FOLDERS:
                return !lastSelectedFolders.equals(getCurrentSelectedTestFolders());
            default:
                return false;
        }
    }
}
