package org.openclover.eclipse.core.reports;

import com.atlassian.clover.reporters.ShowLambdaFunctions;
import org.openclover.eclipse.core.ui.GLH;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;

import java.io.File;

public abstract class ConfigureReportPage extends WizardPage {
    protected Text reportTitle;
    protected boolean reportTitleCustomised;
    protected Text outputPath;
    protected boolean outputPathCustomised;
    protected Button fileButton;
    protected Button useCurrentFilterProjectButton;
    protected Button includeFailedTestCoverageButton;
    protected Combo threadCount;

    // for XML and HTML reports only
    protected Label showLambdaLabel;
    protected Combo showLambdaCombo;

    protected ConfigureReportPage(String title) {
        super(title);
    }

    protected boolean isOutputPathEmpty() {
        return ((outputPath.getText() == null) || (outputPath.getText().trim().length() == 0));
    }

    protected Listener newValidationListener() {
        return new Listener() {
            @Override
            public void handleEvent(Event event) {
                validate();
                getWizard().getContainer().updateButtons();
            }
        };
    }

    private void validate() {
        // validation:
        if (isOutputPathEmpty()) {
            setErrorMessage("Please specify an output directory.");
        } else {
            if (getNumThreads() != 0) {
                setMessage(
                    "Increasing the number of report threads will increase the amount of memory required.",
                    IStatus.WARNING);
            } else {
                setMessage(null, IStatus.WARNING);
            }
            setErrorMessage(null);
        }
    }

    protected abstract Listener newOutputPathListener();

    protected abstract String getOutputPathLabelTitle();

    protected abstract void addListeners();

    protected abstract void createCustomSettings(Composite composite);

    protected void setOutputPath(String path) {
        outputPath.setText(path);
        outputPathCustomised = true;
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        final int columnCount = 3;

        composite.setLayout(new GridLayout(columnCount, false));

        Label reportLabel = new Label(composite, SWT.NONE);
        reportLabel.setText("Report Title:");
        GridData gd = SwtUtils.gridDataFor(reportLabel);
        gd.horizontalSpan = columnCount;
        gd.horizontalAlignment = GridData.BEGINNING;

        reportTitle = new Text(composite, SWT.BORDER | SWT.SINGLE);
        reportTitle.setText("Coverage Report");
        reportTitle.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                reportTitleCustomised = true;
            }
        });

        gd = SwtUtils.gridDataFor(reportTitle);
        gd.horizontalSpan = columnCount;
        gd.horizontalAlignment = GridData.FILL;

        Label outputLabel = new Label(composite, SWT.NONE);
        outputLabel.setText(getOutputPathLabelTitle());
        gd = SwtUtils.gridDataFor(outputLabel);
        gd.horizontalSpan = columnCount;
        gd.horizontalAlignment = GridData.BEGINNING;

        outputPath = new Text(composite, SWT.BORDER | SWT.SINGLE);
        outputPath.setText("");
        outputPath.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                outputPathCustomised = true;
            }
        });
        gd = SwtUtils.gridDataFor(outputPath);
        gd.horizontalSpan = columnCount - 1;
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;

        // support file dialog here.
        fileButton = new Button(composite, SWT.PUSH);
        fileButton.setText("...");
        fileButton.addListener(SWT.Selection, newOutputPathListener());

        gd = SwtUtils.gridDataFor(fileButton);
        gd.horizontalSpan = columnCount - 2;
        gd.horizontalAlignment = GridData.END;

        Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SwtUtils.gridDataFor(separator).horizontalSpan = columnCount;

        useCurrentFilterProjectButton = new Button(composite, SWT.CHECK);
        useCurrentFilterProjectButton.setText("Use the current filter settings from this project");
        useCurrentFilterProjectButton.setSelection(true);
        useCurrentFilterProjectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                getWizard().getContainer().updateButtons();
            }
        });

        gd = SwtUtils.gridDataFor(useCurrentFilterProjectButton);
        gd.horizontalSpan = columnCount;
        gd.horizontalAlignment = GridData.BEGINNING;
        useCurrentFilterProjectButton.setLayoutData(gd);

        includeFailedTestCoverageButton = new Button(composite, SWT.CHECK);
        includeFailedTestCoverageButton.setText("Include failed test coverage");
        includeFailedTestCoverageButton.setSelection(false);
        gd = SwtUtils.gridDataFor(includeFailedTestCoverageButton);
        gd.horizontalSpan = columnCount;
        gd.horizontalAlignment = GridData.BEGINNING;

        Composite threadCountComposite = new Composite(composite, SWT.NONE);
        SwtUtils.gridDataFor(threadCountComposite).horizontalSpan = 2;
        GridLayout threadCountCompositeLayout = new GLH(2, false).marginHeight(0).marginWidth(0).getGridLayout();
        threadCountComposite.setLayout(threadCountCompositeLayout);

        threadCount = new Combo(threadCountComposite, SWT.READ_ONLY);
        threadCount.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        threadCount.setItems(new String[] {"1", "2", "3", "4", "8", "12", "16"});
        threadCount.select(0);

        String threadCountTooltip =
            "A report with more threads may finish faster but will require more memory. " +
            "For very large projects, only one report thread is recommended. Smaller " +
            "projects may benefit from an increased number of threads.";

        Label threadCountPreable = new Label(threadCountComposite, SWT.NONE);
        threadCountPreable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        threadCountPreable.setText("thread(s) allocated to report generation");

        threadCount.setToolTipText(threadCountTooltip);
        threadCountPreable.setToolTipText(threadCountTooltip);
        
        createCustomSettings(composite);

        addListeners();
        setControl(composite);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            updateForSelectedProjects(((GenerateReportWizard)getWizard()).selectProjectsPage.getSelectedProjects());
            validate();
        }
        super.setVisible(visible);
    }

    protected void updateForSelectedProjects(CloverProject[] selectedProjects) {
        updateProjectFilterCombo(selectedProjects);
        updateTitleAndPath(selectedProjects);
    }

    private void updateTitleAndPath(CloverProject[] selectedProjects) {
        if (selectedProjects.length == 1) {
            if (!reportTitleCustomised) {
                reportTitle.setText(selectedProjects[0].getName() + " Coverage Report");
            }
            if (!outputPathCustomised) {
                outputPath.setText(getDefaultOutputPathFor(selectedProjects[0].getProject()));
            }
        } else {
            if (!reportTitleCustomised) {
                reportTitle.setText("Coverage Report");
            }
            if (!outputPathCustomised) {
                outputPath.setText("");
            }
        }
    }

    private void updateProjectFilterCombo(CloverProject[] selectedProjects) {
        useCurrentFilterProjectButton.setEnabled(selectedProjects.length == 1);
        if (!useCurrentFilterProjectButton.isEnabled()) {
            useCurrentFilterProjectButton.setSelection(false);
        }
    }

    @Override
    public boolean canFlipToNextPage() {
        return getErrorMessage() == null;
    }

    @Override
    public boolean isPageComplete() {
        return getErrorMessage() == null;
    }

    @Override
    public IWizardPage getNextPage() {
        if (!useCurrentFilterProjectButton.getSelection()) {
            return ((GenerateReportWizard) getWizard()).configureFilterPage;
        } else {
            return ((GenerateReportWizard) getWizard()).configureJvmPage;
        }
    }

    protected String getReportTitle() {
        return reportTitle.getText();
    }

    protected boolean isUsingDefaultContextFilter() {
        return useCurrentFilterProjectButton.getSelection();
    }

    protected File getOutput() {
        return new File(outputPath.getText());
    }

    public boolean shouldIncludeFailedTestCoverage() {
        return includeFailedTestCoverageButton.getSelection();
    }

    public ShowLambdaFunctions getShowLambdaFunctions() {
        if (showLambdaCombo != null && showLambdaCombo.getSelectionIndex() == 2) {
            return ShowLambdaFunctions.FIELDS_AND_INLINE;
        } else if (showLambdaCombo != null && showLambdaCombo.getSelectionIndex() == 1) {
            return ShowLambdaFunctions.FIELDS_ONLY;
        } else {
            return ShowLambdaFunctions.NONE;
        }
    }

    public int getNumThreads() {
        try {
            return Integer.parseInt(threadCount.getText()) - 1;
        } catch (NumberFormatException e) {
            CloverPlugin.logError("Unable to parse thread count: \"" + threadCount.getText() + "\", defaulting to 0", e);
            return 0;
        }
    }

    protected abstract String getDefaultOutputPathFor(IProject project);

    /**
     * Extra toggle for XML and HTML reports.
     * @param parent parent component
     */
    protected void createShowLambdaComposite(Composite parent) {
        final Composite showLambdaComposite = new Composite(parent, SWT.NONE);
        SwtUtils.gridDataFor(showLambdaComposite).horizontalSpan = 2;
        final GridLayout compositeLayout = new GLH(2, false).marginHeight(0).marginWidth(0).getGridLayout();
        showLambdaComposite.setLayout(compositeLayout);

        // label
        showLambdaLabel = new Label(showLambdaComposite, SWT.NONE);
        showLambdaLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        showLambdaLabel.setText("Show lambda functions: ");

        // drop-down
        showLambdaCombo = new Combo(showLambdaComposite, SWT.READ_ONLY);
        showLambdaCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        showLambdaCombo.setItems(new String[]{
                ShowLambdaFunctions.NONE.getDescription(),
                ShowLambdaFunctions.FIELDS_ONLY.getDescription(),
                ShowLambdaFunctions.FIELDS_AND_INLINE.getDescription()
        });
        showLambdaCombo.select(0);

    }
}
