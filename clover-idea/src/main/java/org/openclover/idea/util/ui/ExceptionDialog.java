package org.openclover.idea.util.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.UIUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import static com.intellij.openapi.ui.Messages.getQuestionIcon;

public class ExceptionDialog extends DialogWrapper {
    private final String description;
    private final String question;
    private final String exceptionStr;
    private final String exceptionTitle;
    private final boolean isYesNoDialog;
    private JComponent exceptionPane;
    private final JPanel thePanel = new JPanel();
    private CellConstraints exceptionCC;

    protected ExceptionDialog(@Nullable Project project, String description, String question, Throwable exception, String title, boolean yesNoDialog) {
        super(project, false);
        this.description = description;
        this.question = question;
        isYesNoDialog = yesNoDialog;
        this.exceptionStr = getExceptionString(exception);
        this.exceptionTitle = exception.getLocalizedMessage();

        setTitle(title);

        init();
    }

    public static int showYesNoDialog(Project project, String description, String question, Throwable exception, String title) {
        final ExceptionDialog dialog = new ExceptionDialog(project, description, question, exception, title, true);
        dialog.show();
        return dialog.getExitCode();
    }

    public static void showOKDialog(@Nullable Project project, String description, String question, Throwable exception, String title) {
        final ExceptionDialog dialog = new ExceptionDialog(project, description, question, exception, title, false);
        dialog.show();
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {

        FormLayout formLayout = new FormLayout("3dlu, pref, 3dlu, 15dlu, 300dlu, 3dlu",
                                               "3dlu, pref, 3dlu, pref, 3dlu, pref, 9dlu, pref, 9dlu");
        CellConstraints iconCC = new CellConstraints(2, 2, 1, 5);
        CellConstraints descrCC = new CellConstraints(4, 2, 2, 1);
        CellConstraints exceptionTitleCC = new CellConstraints(5, 4, 1, 1);
        exceptionCC = new CellConstraints(5, 6, 1, 1);
        CellConstraints questionCC = new CellConstraints(4, 8, 2, 1);

        thePanel.setLayout(formLayout);

        JLabel iconLabel = new JLabel(getQuestionIcon());
        iconLabel.setVerticalAlignment(JLabel.TOP);
        thePanel.add(iconLabel, iconCC);

        JTextArea descrArea = new JTextArea(description);
        descrArea.setEditable(false);
        descrArea.setBackground(UIUtil.getOptionPaneBackground());
        thePanel.add(descrArea, descrCC);

        final JLabel exTitleLabel = new JLabel(exceptionTitle);
        exTitleLabel.setFont(exTitleLabel.getFont().deriveFont(Font.ITALIC));
        thePanel.add(exTitleLabel, exceptionTitleCC);

        JTextArea exceptionArea = new JTextArea(10, 60);
        exceptionArea.setText(exceptionStr);
        exceptionArea.setEditable(false);
        exceptionArea.setCaretPosition(0);
        exceptionArea.setTabSize(2);
        exceptionPane = new JScrollPane(exceptionArea);

        JTextArea questionArea = new JTextArea(question);
        questionArea.setEditable(false);
        questionArea.setBackground(UIUtil.getOptionPaneBackground());
        thePanel.add(questionArea, questionCC);

        return thePanel;
    }

    @Override
    protected Action[] createActions() {
        return isYesNoDialog ? new Action[]{new DetailsAction(), getOKAction(), getCancelAction()}
                : new Action[] {new DetailsAction(), getOKAction()};
    }

    private static String getExceptionString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.getBuffer().toString();
    }

    private class DetailsAction extends AbstractAction {
        private static final String SHOW_TXT = "Show Exception Details";
        private static final String HIDE_TXT = "Hide Exception Details";

        public DetailsAction() {
            putValue(Action.NAME, SHOW_TXT);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (exceptionPane.getParent() != null) {
                thePanel.remove(exceptionPane);
                putValue(Action.NAME, SHOW_TXT);
            } else {
                thePanel.add(exceptionPane, exceptionCC);
                putValue(Action.NAME, HIDE_TXT);
            }
            pack();
        }
    }

}
