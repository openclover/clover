package org.openclover.idea.report;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;

/**
 * The report wizard dialog. Built on IntelliJ's {@link DialogWrapper} so that showing it does not
 * spin up a raw AWT modal event loop from within an action's coroutine context (which the platform
 * logs as "Thread context was already set").
 */
public class ReportWizardDialog extends DialogWrapper {

    private final ReportWizard wizard;

    private final JPanel centerPanel = new JPanel(new BorderLayout());

    private ReportWizardPage currentPage;

    private NextAction nextAction;
    private PreviousAction previousAction;
    private FinishAction finishAction;
    private CancelAction cancelAction;

    public ReportWizardDialog(ReportWizard wizard) {
        super(true);
        this.wizard = wizard;
        setTitle("Generate Coverage Report");
        init();
    }

    public void setCurrentPage(ReportWizardPage c) {
        currentPage = c;

        centerPanel.removeAll();
        centerPanel.add(c, BorderLayout.CENTER);
        centerPanel.revalidate();
        centerPanel.repaint();

        // Grow the dialog if the new page needs more room, but never shrink it (mirrors the old behavior).
        final Window window = getWindow();
        if (window != null && window.isShowing()) {
            final Dimension current = window.getSize();
            final Dimension preferred = window.getPreferredSize();
            if (preferred.width > current.width || preferred.height > current.height) {
                window.setSize(Math.max(current.width, preferred.width),
                        Math.max(current.height, preferred.height));
            }
            window.validate();
        }
    }

    public ReportWizardPage getCurrentPage() {
        return currentPage;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return centerPanel;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getPreviousAction(), getNextAction(), getFinishAction(), getCancelAction()};
    }

    protected Action getPreviousAction() {
        if (previousAction == null) {
            previousAction = new PreviousAction();
        }
        return previousAction;
    }

    protected Action getNextAction() {
        if (nextAction == null) {
            nextAction = new NextAction();
        }
        return nextAction;
    }

    protected Action getFinishAction() {
        if (finishAction == null) {
            finishAction = new FinishAction();
        }
        return finishAction;
    }

    protected Action getCancelAction() {
        if (cancelAction == null) {
            cancelAction = new CancelAction();
        }
        return cancelAction;
    }

    protected class NextAction extends AbstractAction {
        public NextAction() {
            super("Next >");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            wizard.doNext();
        }
    }

    protected class PreviousAction extends AbstractAction {
        public PreviousAction() {
            super("< Previous");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            wizard.doPrevious();
        }
    }

    protected class FinishAction extends AbstractAction {
        public FinishAction() {
            super("Finish");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            wizard.doFinish();
        }
    }

    protected class CancelAction extends AbstractAction {
        public CancelAction() {
            super("Cancel");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            wizard.doCancel();
        }
    }
}
