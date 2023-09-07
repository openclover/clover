package org.openclover.eclipse.core.views.coverageexplorer;

import org.openclover.eclipse.core.projects.CloveredProjectLabelProvider;
import com.atlassian.clover.registry.metrics.BlockMetrics;
import com.atlassian.clover.util.MetricsFormatUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class CoverageViewMetricsPane extends Composite {
    private TreeViewer projectTreeViewer;
    private CoverageViewSettings settings;
    private WorkbenchLabelProvider labelProvider;

    private FormToolkit toolkit;
    private Form form;
    private Label passTestsValue;
    private Label failureTestsValue;
    private Label errorsValue;
    private Label testsValue;
    private Label packagesValue;
    private Label methodsValue;
    private Label filesValue;
    private Label ncLinesOfCodeValue;
    private Label classesValue;
    private Label linesOfCodeValue;
    private Section testSection;
    private Section structureSection;
    private Section sourceSection;
    private Label statementsValue;
    private Label branchesValue;
    private Label complexityValue;
    private Label avgMethodComplexityValue;
    private Label complexityDensityValue;

    public CoverageViewMetricsPane(Composite parent, int style, CoverageViewSettings settings, TreeViewer projectTreeViewer) {
        super(parent, style);
        this.settings = settings;
        this.projectTreeViewer = projectTreeViewer;

        setLayout(new GridLayout(1, false));

        labelProvider = new CloveredProjectLabelProvider();

        toolkit = new FormToolkit(getDisplay());
        form = toolkit.createForm(this);
        form.setBackground(null);
        form.setLayoutData(new GridData(GridData.FILL_BOTH));
        setMetricsFor("-");

        form.getBody().setLayout(new GridLayout(2, true));

        createStructureSection();

        createTestSection();

        createSource();

        projectTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent selectionChangedEvent) {
                if (selectionChangedEvent.getSelection() instanceof IStructuredSelection) {
                    update((IStructuredSelection)selectionChangedEvent.getSelection());
                } else {
                    clear();
                }
            }
        });
    }

    private void setMetricsFor(String selection) {
        form.setText("Metrics for: " + selection);
    }

    private void createSource() {
        sourceSection = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED);
        sourceSection.setText("Source");
        sourceSection.setToolTipText("Information about the source code in the current selection");
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        sourceSection.setLayoutData(gridData);

        Composite sourceForm = toolkit.createComposite(sourceSection);
        sourceSection.setClient(sourceForm);

        sourceForm.setLayout(new GridLayout(4, true));

        Label label = toolkit.createLabel(sourceForm, "LOC:");
        label.setToolTipText("The number of lines of code in the current selection");
        linesOfCodeValue = toolkit.createLabel(sourceForm, "-");

        label = toolkit.createLabel(sourceForm, "NC LOC:");
        label.setToolTipText("The number of non-comment lines of code in the current selection");
        ncLinesOfCodeValue = toolkit.createLabel(sourceForm, "-");

        label = toolkit.createLabel(sourceForm, "Total Cmp:");
        label.setToolTipText("The total cyclomatic complexity of the current selection");
        complexityValue = toolkit.createLabel(sourceForm, "-");

        label = toolkit.createLabel(sourceForm, "Cmp Density:");
        label.setToolTipText("The complexity density of the current selection");
        complexityDensityValue = toolkit.createLabel(sourceForm, "-");

        label = toolkit.createLabel(sourceForm, "Avg Method Cmp:");
        label.setToolTipText("The average method complexity of the current selection");
        avgMethodComplexityValue = toolkit.createLabel(sourceForm, "-");
    }

    private void createTestSection() {
        testSection = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED);
        testSection.setText("Test Executions");
        testSection.setToolTipText("Information on the test methods defined in the current selection.");
        testSection.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite testsForm = toolkit.createComposite(testSection);
        testSection.setClient(testsForm);

        testsForm.setLayout(new GridLayout(2, true));

        Label label = toolkit.createLabel(testsForm, "Executed Tests:");
        label.setToolTipText("The number of test methods defined in the current selection that have been run and generated some coverage");
        testsValue = toolkit.createLabel(testsForm, "-");

        label = toolkit.createLabel(testsForm, "Passes:");
        label.setToolTipText("The number of executed test methods in the selection that didn't fail or have an error");
        passTestsValue = toolkit.createLabel(testsForm, "-");

        label = toolkit.createLabel(testsForm, "Fails:");
        label.setToolTipText("The number of executed test methods in the selection that failed");
        failureTestsValue = toolkit.createLabel(testsForm, "-");

        label = toolkit.createLabel(testsForm, "Errors:");
        label.setToolTipText("The number of executed test methods in the selection that resulted in an error");
        errorsValue = toolkit.createLabel(testsForm, "-");
    }

    private void createStructureSection() {
        structureSection = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED);
        structureSection.setText("Structure");
        structureSection.setToolTipText("Information on the structure of the current selection");
        structureSection.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite structureForm = toolkit.createComposite(structureSection);
        structureSection.setClient(structureForm);

        structureForm.setLayout(new GridLayout(2, true));

        Label label = toolkit.createLabel(structureForm, "Packages:");
        label.setToolTipText("The number of Java packages");
        packagesValue = toolkit.createLabel(structureForm, "-");

        label = toolkit.createLabel(structureForm, "Files:");
        label.setToolTipText("The number of Java files");
        filesValue = toolkit.createLabel(structureForm, "-");

        label = toolkit.createLabel(structureForm, "Classes:");
        label.setToolTipText("The number of Java classes");
        classesValue = toolkit.createLabel(structureForm, "-");

        label = toolkit.createLabel(structureForm, "Methods:");
        label.setToolTipText("The number of Java methods");
        methodsValue = toolkit.createLabel(structureForm, "-");

        toolkit.createLabel(structureForm, "Statements:");
        statementsValue = toolkit.createLabel(structureForm, "-");

        toolkit.createLabel(structureForm, "Branches:");
        branchesValue = toolkit.createLabel(structureForm, "-");
    }

    @Override
    public void dispose() {
        toolkit.dispose();
        super.dispose();
    }

    private void clear() {
        setMetricsFor("-");
        failureTestsValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        passTestsValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        errorsValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        testsValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);

        packagesValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        methodsValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        filesValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        statementsValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        branchesValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);

        ncLinesOfCodeValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        classesValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        linesOfCodeValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        complexityValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        complexityDensityValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);
        avgMethodComplexityValue.setText(MetricsFormatUtils.NO_METRICS_LABEL);

        structureSection.layout();
        testSection.layout();
        sourceSection.layout();
    }

    private void update(IStructuredSelection selection) {
        if (selection.getFirstElement() instanceof IAdaptable) {
            BlockMetrics metrics = settings.getMetricsScope().getMetricsFor(selection.getFirstElement());
            if (metrics != null) {

                failureTestsValue.setText(MetricsFormatUtils.formatMetricsInteger(metrics.getNumTestFailures()));
                passTestsValue.setText(MetricsFormatUtils.formatMetricsInteger(metrics.getNumTestPasses()));
                errorsValue.setText(MetricsFormatUtils.formatMetricsInteger(metrics.getNumTestErrors()));
                testsValue.setText(MetricsFormatUtils.formatMetricsInteger(metrics.getNumTests()));

                packagesValue.setText(MetricsFormatUtils.formatMetricsInteger(MetricsFormatUtils.getNumPackages(metrics)));
                methodsValue.setText(MetricsFormatUtils.formatMetricsInteger(MetricsFormatUtils.getNumMethods(metrics)));
                filesValue.setText(MetricsFormatUtils.formatMetricsInteger(MetricsFormatUtils.getNumFiles(metrics)));
                statementsValue.setText(MetricsFormatUtils.formatMetricsInteger(MetricsFormatUtils.getNumStatements(metrics)));
                branchesValue.setText(MetricsFormatUtils.formatMetricsInteger(MetricsFormatUtils.getNumBranches(metrics)));
                classesValue.setText(MetricsFormatUtils.formatMetricsInteger(MetricsFormatUtils.getNumClasses(metrics)));

                ncLinesOfCodeValue.setText(MetricsFormatUtils.formatMetricsInteger(MetricsFormatUtils.getNcLineCount(metrics)));
                linesOfCodeValue.setText(MetricsFormatUtils.formatMetricsInteger(MetricsFormatUtils.getLineCount(metrics)));
                complexityValue.setText(MetricsFormatUtils.formatMetricsInteger(MetricsFormatUtils.getComplexity(metrics)));
                complexityDensityValue.setText(MetricsFormatUtils.formatMetricsDecimal(MetricsFormatUtils.getComplexityDensity(metrics)));
                avgMethodComplexityValue.setText(MetricsFormatUtils.formatMetricsDecimal(MetricsFormatUtils.getAvgMethodComplexity(metrics)));

                structureSection.layout();
                testSection.layout();
                sourceSection.layout();

                setFormName(selection);

                return;
            }
        }
        clear();
        setFormName(selection);

    }

    private void setFormName(IStructuredSelection selection) {
        IAdaptable asAdaptable = selection == null ? null : (IAdaptable) selection.getFirstElement();
        IJavaElement asJavaElement = asAdaptable == null ? null : (IJavaElement) asAdaptable.getAdapter(IJavaElement.class);

        String label = "-";
        if (asJavaElement != null) {
            label = labelProvider.getText(asJavaElement);
        }
        setMetricsFor(label);
    }
}
