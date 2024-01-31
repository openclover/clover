package org.openclover.eclipse.testopt.views.actions;

import org.openclover.eclipse.testopt.OptimizedLaunchingConstants;
import org.openclover.eclipse.testopt.OptimizationSessionListener;
import org.openclover.eclipse.testopt.TestOptimizationPlugin;
import com.atlassian.clover.optimization.OptimizationSession;

import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class SavingsNotificationContributionItem extends WorkbenchWindowControlContribution implements OptimizationSessionListener {
    private OptimizationSession lastOptimizationSession;

    private boolean dirty = true;

    private OptimizationSessionIcon sessionIcon;

    private final TestRunListener testRunListener = new TestRunListener() {
        @Override
        public void sessionStarted(final ITestRunSession isession) {
            Display.getDefault().asyncExec(() -> {
                final TestRunSession session = (TestRunSession) isession;
                dirty = true;
                // this is a test run without Clover optimization, clear stats
                if (!OptimizedLaunchingConstants.OPTIMIZED_MODE.equals(session.getLaunch().getLaunchMode())) {
                    sessionIcon.refresh(null);
                }
            });
        }
    };

    public SavingsNotificationContributionItem() {
        TestOptimizationPlugin.getDefault().addOptimizationSessionListener(this);
        JUnitCore.addTestRunListener(testRunListener);
    }

    @Override
    public void dispose() {
        super.dispose();
        TestOptimizationPlugin.getDefault().removeOptimizationSessionListener(this);
        JUnitCore.removeTestRunListener(testRunListener);
    }

    @Override
    protected Control createControl(Composite parent) {
        dirty = false;
        sessionIcon = new OptimizationSessionIcon(parent, lastOptimizationSession);
        return sessionIcon;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public void sessionFinished(final OptimizationSession session) {
        Display.getDefault().asyncExec(() -> {
            lastOptimizationSession = session;
            dirty = true;
            sessionIcon.refresh(session);
        });
    }

}

class OptimizationSessionIcon extends Composite {
    private Label label;
    private Label image;

    OptimizationSessionIcon(Composite parent, OptimizationSession session) {
        super(parent, SWT.NONE);

        setLayout(new GridLayout(2, false));

        image = new Label(this, SWT.NONE);
        image.setImage(TestOptimizationPlugin.getDefault().getTestOptimizationIcon());

        label = new Label(this, SWT.NONE);
        refresh(session);
    }

    public void refresh(OptimizationSession session) {
        final String tooltipText;
        if (session == null) {
            label.setText("-/-");
            tooltipText = "No test optimization in effect";
        } else {
            label.setText("" + session.getOptimizedTestableCount() + "/" + session.getOriginalTestableCount());
            tooltipText = session.getPlainSummary();
        }
        label.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).create());

        image.setToolTipText(tooltipText);
        label.setToolTipText(tooltipText);
        setToolTipText(tooltipText);

        update();
    }

}
