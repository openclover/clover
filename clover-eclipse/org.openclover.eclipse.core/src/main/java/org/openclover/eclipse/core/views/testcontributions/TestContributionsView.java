package org.openclover.eclipse.core.views.testcontributions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.views.CloverViewPart;
import org.openclover.eclipse.core.views.widgets.ViewAlertContainer;

public class TestContributionsView extends CloverViewPart {
    public static final String ID = CloverPlugin.ID + ".views.testcontributions";
    
    private ActiveEditorListener editorListener;

    private AllTestContributionsFilterWidget allTestsWidget;
    private AllTestContributionsFilterWidget.Settings allTestWidgetSettings;

    private LineTestContributionsWidget lineTestsWidget;
    private LineTestContributionsWidget.Settings lineTestsWidgetSettings;

    private static final int ACTION_OPEN = 0;
    private static final int ACTION_MAX = 1;

    @Override
    public void init(IViewSite iViewSite) throws PartInitException {
        super.init(iViewSite);
        allTestWidgetSettings = new AllTestContributionsFilterWidget.Settings();
        lineTestsWidgetSettings = new LineTestContributionsWidget.Settings();
    }

    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
        super.init(site, memento);
        allTestWidgetSettings = new AllTestContributionsFilterWidget.Settings(memento);
        lineTestsWidgetSettings = new LineTestContributionsWidget.Settings(memento);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        alertContainer = new ViewAlertContainer(parent);
        mainContent = new SashForm(alertContainer, calcViewOrientation());
        mainContent.setLayoutData(new GridData(GridData.FILL_BOTH));
        alertContainer.setContent(mainContent);

        createAllTestsWidget(mainContent);
        createLineTestsWidget(mainContent);

        mainContent.setWeights(new int[] {5, 3});

        alertContainer.updateLinks();

        createActiveEditorListener();

        allTestsWidget.startTrackingEditor();
        lineTestsWidget.startTrackingEditor();

        CloverPlugin.getInstance().getCoverageMonitor().addCoverageChangeListener(alertContainer);
    }

    private void createLineTestsWidget(Composite parent) {
        lineTestsWidget =
            new LineTestContributionsWidget(
                lineTestsWidgetSettings,
                this,
                parent,
                SWT.NONE,
                SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BOTTOM, true);
    }

    private void createAllTestsWidget(Composite parent) {
        allTestsWidget =
            new AllTestContributionsFilterWidget(
                allTestWidgetSettings,
                this,
                parent,
                SWT.NONE,
                SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CENTER, true);
    }

    private void createActiveEditorListener() {
        editorListener = new ActiveEditorListener(this);
    }

    @Override
    public void setFocus() {
        allTestsWidget.getTree().setFocus();
    }

    @Override
    public void dispose() {
        if (editorListener != null) {editorListener.dispose();}
        if (allTestsWidget != null) {allTestsWidget.dispose();}
        if (lineTestsWidget != null) {lineTestsWidget.dispose();}

        if (alertContainer != null) {
            CloverPlugin.getInstance().getCoverageMonitor().removeCoverageChangeListener(alertContainer);
        }
        super.dispose();
    }

    public void setEnabled(boolean enabled) {
        allTestsWidget.setEnabled(enabled);
        lineTestsWidget.setEnabled(enabled);
    }

    public void setEditor(ITextEditor editor) {
        allTestsWidget.setEditor(editor);
        lineTestsWidget.setEditor(editor);
    }

    @Override
    protected int getAssumedViewOrientationIfUnknowable() {
        return SWT.VERTICAL;
    }
}
