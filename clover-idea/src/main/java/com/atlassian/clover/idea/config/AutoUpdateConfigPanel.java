package com.atlassian.clover.idea.config;

import com.atlassian.clover.idea.autoupdater.AutoUpdateConfiguration;
import com.atlassian.clover.idea.autoupdater.NewVersionNotifier;
import com.atlassian.clover.idea.autoupdater.AutoUpdateComponent;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.atlassian.clover.idea.util.l10n.CloverIdeaPluginMessages;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.panels.HorizontalBox;
import com.intellij.ui.components.panels.VerticalBox;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AutoUpdateConfigPanel extends JPanel {
    private final JCheckBox autoUpdate = new JCheckBox(CloverIdeaPluginMessages.getString("autoupdate.auto"));
    private final JCheckBox useMilestone = new JCheckBox(CloverIdeaPluginMessages.getString("autoupdate.usemilestone"));

    public AutoUpdateConfigPanel() {
        setLayout(new GridBagLayout());

        final VerticalBox box = new VerticalBox();
        box.setBorder(BorderFactory.createTitledBorder(CloverIdeaPluginMessages.getString("autoupdate.configtitle")));
        box.add(autoUpdate);
        box.add(useMilestone);

        final HorizontalBox buttons = new HorizontalBox();
        final JButton checkNow = new JButton(CloverIdeaPluginMessages.getString("autoupdate.checknow"));
        final JButton downloadManually = new JButton(CloverIdeaPluginMessages.getString("autoupdate.fromurl"));
        buttons.add(checkNow);
        buttons.add(downloadManually);

        box.add(buttons);

        add(box, new GBC(0, 0).setWeight(1, 0).setFill(GBC.BOTH));

        add(new JPanel(), new GBC(0, 1).setWeight(1, 1).setFill(GBC.BOTH));

        checkNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final NewVersionNotifier notifier = getAnyNotifier();
                if (notifier != null) {
                    notifier.checkNow(useMilestone.isSelected());
                }
            }
        });

        downloadManually.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final String url = Messages.showInputDialog(CloverIdeaPluginMessages.getString("autoupdate.enterurl"), CloverIdeaPluginMessages.getString("autoupdate.updatingplugin"), CloverIcons.CLOVER_BIG);
                if (url != null) {
                    AutoUpdateComponent.getInstance().performUpdate(url);
                }
            }
        });

        autoUpdate.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                useMilestone.setEnabled(autoUpdate.isSelected());
            }
        });
        useMilestone.setEnabled(autoUpdate.isSelected());

    }

    private NewVersionNotifier getAnyNotifier() {
        final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        final Project anyProject = openProjects.length > 0 ? openProjects[0] : ProjectManager.getInstance().getDefaultProject();
        final NewVersionNotifier notifier = NewVersionNotifier.getInstance(anyProject);
        return notifier;
    }

    public void setAutoUpdateConfig(AutoUpdateConfiguration config) {
        autoUpdate.setSelected(config.isAutoUpdate());
        useMilestone.setSelected(config.isUseMilestone());
    }

    public AutoUpdateConfiguration getAutoUpdateConfig() {
        return new AutoUpdateConfiguration(autoUpdate.isSelected(), useMilestone.isSelected());
    }

    
}
