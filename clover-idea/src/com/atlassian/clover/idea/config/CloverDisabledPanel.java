package com.atlassian.clover.idea.config;

import com.atlassian.clover.idea.CloverPlugin;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CloverDisabledPanel extends JPanel implements ActionListener {
    private final Project project;
    private JButton button;

    public CloverDisabledPanel(Project project) {
        this.project = project;
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        JTextArea brokenText = new JTextArea("\n\nThe Clover Plugin has been disabled due to invalid license. \n\n");// + problemMsg);
        brokenText.setLineWrap(true);
        brokenText.setWrapStyleWord(true);
        brokenText.setBackground(getBackground());
        brokenText.setFont(getFont());
        brokenText.setEditable(false);
        add(brokenText, BorderLayout.CENTER);

        button = new JButton("Go to license page");
        button.setActionCommand("licensePage");
        button.addActionListener(this);
        add(button, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ShowSettingsUtil.getInstance().editConfigurable(project, CloverPlugin.getPlugin().getConfigurable());

    }
}
