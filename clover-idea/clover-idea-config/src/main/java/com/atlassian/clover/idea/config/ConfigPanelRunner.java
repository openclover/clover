package com.atlassian.clover.idea.config;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ConfigPanelRunner {

    public static final void run(ConfigPanel configPanel) {

        JFrame frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        frame.setSize(configPanel.getPreferredSize());
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(configPanel, BorderLayout.CENTER);
        frame.validate();
        frame.setVisible(true);
    }
}
