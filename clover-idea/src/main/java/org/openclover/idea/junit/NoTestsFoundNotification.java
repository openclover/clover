package org.openclover.idea.junit;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.openclover.idea.CloverToolWindowId;
import org.openclover.idea.junit.config.TestOptimizationGlobalSettings;
import org.openclover.idea.util.l10n.CloverIdeaPluginMessages;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class NoTestsFoundNotification extends DialogWrapper {
    private JPanel contentPane;
    private JCheckBox displayThisDialogCheckBox;
    private JCheckBox notifyByBalloonCheckBox;
    private JLabel messageLabel;

    private final TestOptimizationGlobalSettings settings;

    public NoTestsFoundNotification(TestOptimizationGlobalSettings settings) {
        super(false);
        this.settings = settings;
        $$$setupUI$$$();

        setTitle("OpenClover Test Optimization");
        displayThisDialogCheckBox.setSelected(settings.isShowAllTestsOptimizedOutDialog());
        notifyByBalloonCheckBox.setSelected(settings.isShowAllTestsOptimizedOutBalloon());

        init();
    }

    public static void showNotificationDialog() {
        TestOptimizationGlobalSettings togs = TestOptimizationGlobalSettings.getInstance();
        new NoTestsFoundNotification(togs).show();
    }

    public static void showNotificationBalloon() {
        Project guessedProject = null;
        // try to find a focused component, next the master window and the data context
        final AsyncResult<DataContext> result = DataManager.getInstance().getDataContextFromFocus();
        // if found, look for a project data;
        if (result != null && result.getResult() != null) {
            guessedProject = DataKeys.PROJECT.getData(result.getResult());
        }
        // no project found? maybe we've got exactly one opened ... if yes take it
        if (guessedProject == null) {
            final Project[] projects = ProjectManager.getInstance().getOpenProjects();
            if (projects.length == 1) {
                guessedProject =  projects[0];
            }
        }

        // show popup in project window
        if (guessedProject != null) {
            ToolWindowManager.getInstance(guessedProject).notifyByBalloon(
                    CloverToolWindowId.TOOL_WINDOW_ID,
                    MessageType.INFO,
                    CloverIdeaPluginMessages.getString("launch.optimized.notestsfound"));
        }
    }

    public static void showNotifications() {
        TestOptimizationGlobalSettings togs = TestOptimizationGlobalSettings.getInstance();
        if (togs.isShowAllTestsOptimizedOutBalloon()) {
            showNotificationBalloon();
        }
        if (togs.isShowAllTestsOptimizedOutDialog()) {
            showNotificationDialog();
        }
    }

    @Override
    protected void doOKAction() {
        settings.setShowAllTestsOptimizedOutDialog(displayThisDialogCheckBox.isSelected());
        settings.setShowAllTestsOptimizedOutBalloon(notifyByBalloonCheckBox.isSelected());
        super.doOKAction();
    }

    private void createUIComponents() {
        messageLabel = new JLabel(CloverIdeaPluginMessages.getString("launch.optimized.notestsfound"));
        messageLabel.setIcon(Messages.getInformationIcon());
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:18dlu:noGrow,fill:420px:noGrow", "center:max(d;4px):noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        CellConstraints cc = new CellConstraints();
        contentPane.add(messageLabel, cc.xyw(1, 1, 3));
        final JLabel label1 = new JLabel();
        label1.setText("Next time this happens:");
        contentPane.add(label1, cc.xy(3, 2));
        displayThisDialogCheckBox = new JCheckBox();
        displayThisDialogCheckBox.setText("Display this dialog");
        contentPane.add(displayThisDialogCheckBox, cc.xy(3, 4));
        notifyByBalloonCheckBox = new JCheckBox();
        notifyByBalloonCheckBox.setText("Notify by Balloon");
        contentPane.add(notifyByBalloonCheckBox, cc.xy(3, 6));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
