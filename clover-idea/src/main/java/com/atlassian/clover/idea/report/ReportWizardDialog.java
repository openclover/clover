package com.atlassian.clover.idea.report;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

public class ReportWizardDialog extends JDialog {

    private static final Border DEFAULT_BORDER = new EmptyBorder(8, 8, 8, 8);

    private NextAction nextAction;
    private PreviousAction previousAction;
    private FinishAction finishAction;
    private CancelAction cancelAction;

    private ReportWizard wizard;

    private ReportWizardPage currentPage;

    private int exitCode = -1;

    public ReportWizardDialog(ReportWizard wizard) {
        super(JOptionPane.getRootFrame(), "Generate Coverage Report", true);
        this.wizard = wizard;
    }

    public void setCurrentPage(ReportWizardPage c) {
        currentPage = c;
        Dimension currentSize = getSize();

        init();

        if (currentSize.getHeight() < getPreferredSize().getHeight()) {
            pack();
        } else {
            validate();
        }
    }

    public ReportWizardPage getCurrentPage() {
        return currentPage;
    }

    protected JComponent createCenterPanel() {
        return getCurrentPage();
    }

    protected Action[] createActions() {
        return new Action[]{getPreviousAction(), getNextAction(), getFinishAction(), getCancelAction()};
    }

    private JPanel createButtons(Action[] actions) {
        JPanel buttonsPanel = new JPanel(new GridLayout(1, actions.length, 5, 0));
        for (Action action : actions) {
            JButton button = new JButton(action);
            buttonsPanel.add(button);
        }
        return buttonsPanel;
    }

    protected JComponent createSouthPanel() {
        Action[] actions = createActions();

        final JPanel panel = new JPanel(new GridBagLayout());
        if (actions.length > 0) {
            int gridx = 0;

            panel.add(// left strut
                    Box.createHorizontalGlue(),
                      new GridBagConstraints(gridx++, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                             new Insets(8, 0, 0, 0), 0, 0));
            if (actions.length > 0) {
                JPanel buttonsPanel = createButtons(actions);
                panel.add(buttonsPanel,
                          new GridBagConstraints(gridx++, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                 new Insets(8, 0, 0, 0), 0, 0));
            }

        }
        return panel;
    }

    protected void init() {

        JComponent contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(DEFAULT_BORDER);

        JComponent centerPanel = createCenterPanel();
        if (centerPanel != null) {
            contentPane.add(centerPanel, BorderLayout.CENTER);
        }
        JComponent southPanel = createSouthPanel();
        if (southPanel != null) {
            contentPane.add(southPanel, BorderLayout.SOUTH);
        }
        getContentPane().removeAll();
        getContentPane().add(contentPane);
        getContentPane().validate();
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

    public final void close(int exitCode) {
        this.exitCode = exitCode;
        dispose();
    }

    public int getExitCode() {
        return exitCode;
    }
}
