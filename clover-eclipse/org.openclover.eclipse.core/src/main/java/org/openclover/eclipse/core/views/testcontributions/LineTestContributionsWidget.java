package org.openclover.eclipse.core.views.testcontributions;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.IMemento;
import org.openclover.eclipse.core.CloverPlugin;

public class LineTestContributionsWidget extends TestContributionsWidget {
    private static final int SYNC_DELAY_MS = 1500;

    public LineTestContributionsWidget(Settings settings, ViewPart viewPart, Composite parent, int style, int treeStyle, boolean showLabel) {
        super(settings, viewPart, parent, style, showLabel, treeStyle);
    }

    @Override
    protected TreeViewer createTreeViewer() {
        return new TreeViewer(tree);
    }

    @Override
    protected TestContributionsComputation createContributionsComputation() {
        final LineTestContributionsComputation testContributionsComputation = new LineTestContributionsComputation();
        testContributionsComputation.addJobChangeListener(
            new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    if (event.getResult().getSeverity() != Status.CANCEL) {
                        Display.getDefault().syncExec(() -> {
                            try {
                                if (testsViewer.getContentProvider() != null
                                    && testContributionsComputation != null
                                    && viewPart.getViewSite().getPage().isPartVisible(viewPart)) {
                                    if (testContributionsComputation.isInputChanged()) {
                                        testsViewer.setInput(testContributionsComputation);
                                    }
                                    testContributionsComputation.prime(editor);
                                }
                            } catch (Exception e) {
                                CloverPlugin.logError("Unable to update line-based test contributions", e);
                            }
                        });

                        testContributionsComputation.schedule(SYNC_DELAY_MS);
                    }
                }
            });
        return testContributionsComputation;
    }

    @Override
    protected String getLabelTitle() {
        return "Contributing tests at cursor:";
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

    public static class Settings extends TestContributionsWidgetSettings {
        public Settings(IMemento memento) {
            super(memento);
        }

        public Settings() {
            super();
        }

        @Override
        public String prefix(String base) {
            return "line.";
        }
    }
}
