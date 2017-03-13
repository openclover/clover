package com.atlassian.clover.idea.report.jfc;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

public class WarningBox extends JPanel {
    private JTextArea title;
    private JTextArea warningText;

    public WarningBox() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.black));

        title = new JTextArea("Warning");
        title.setBackground(new Color(0xff, 0xff, 0x60));
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setMargin(new Insets(1, 5, 1, 0));

        warningText = new JTextArea();
        warningText.setLineWrap(true);
        warningText.setWrapStyleWord(true);
        warningText.setEditable(false);
        warningText.setBackground(new Color(0xff, 0xff, 0xe0));
        warningText.setMargin(new Insets(5, 30, 5, 30));

        add(title, BorderLayout.NORTH);
        add(warningText, BorderLayout.CENTER);
    }

    public void setMessage(String message) {
        this.warningText.setText(message);
    }
}
