package org.openclover.idea.util.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/**
 */
public class UIUtils {
    public static void centerOnParent(Component aParent, Component aDialog) {
        int x = (aParent.getWidth() - aDialog.getWidth()) / 2 + aParent.getX();
        int y = (aParent.getHeight() - aDialog.getHeight()) / 2 + aParent.getY();

        aDialog.setLocation(Math.max(0, x), Math.max(0, y));

    }


    /**
     * @param aParent            the parent to the chooser dialog
     * @param aTextField         the text field representing the file choice
     * @param dotDotDot          the chooser popup button
     * @param aFileSelectionMode the argument to pass to {@link javax.swing.JFileChooser#setFileSelectionMode}.
     */
    public static Component wrapWithFileChooser(final Component aParent,
                                                final JTextField aTextField,
                                                final JButton dotDotDot,
                                                final int aFileSelectionMode) {
        JPanel pane = new JPanel(new BorderLayout());
        pane.add(aTextField, BorderLayout.CENTER);
        pane.add(dotDotDot, BorderLayout.EAST);
        dotDotDot.addActionListener(actionEvent -> {
            JFileChooser chooser = new JFileChooser(aTextField.getText());
            chooser.setFileSelectionMode(aFileSelectionMode);
            int choice = chooser.showDialog(aParent, "Select");
            if (choice == JFileChooser.APPROVE_OPTION) {
                aTextField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        return pane;
    }

    /**
     * @param aParent            the parent to the chooser dialog
     * @param aTextField         the text field representing the file choice
     * @param aFileSelectionMode the argument to pass to {@link javax.swing.JFileChooser#setFileSelectionMode}.
     */
    public static Component wrapWithFileChooser(final Component aParent,
                                                final JTextField aTextField,
                                                final int aFileSelectionMode) {
        return wrapWithFileChooser(aParent, aTextField, new JButton("..."), aFileSelectionMode);
    }

    public static ImageIcon createImageIcon(String filename, String description) {
        final String path = "/jfc_res/" + filename;
        return new ImageIcon(UIUtils.class.getResource(path), description);
    }

    public static Border createBorder(String title) {
        return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3),
                                                  BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title),
                                                                                     BorderFactory.createEmptyBorder(2, 2, 2, 2)));
    }

    public static Border createDisabledBorder(String title) {
        return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3),
                                                  BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(null, title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, Color.gray),
                                                                                     BorderFactory.createEmptyBorder(2, 2, 2, 2)));
    }

    public static Component createHorizontalLine() {
        JPanel p = new JPanel();
        p.add(Box.createHorizontalStrut(100));
        p.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        return p;
    }

    public static JTextArea getHelpTextArea(String help, Color background, Font font) {
        JTextArea helpText = new JTextArea(help);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpText.setBackground(background);
        helpText.setFont(font);
        helpText.setEditable(false);
        return helpText;
    }

}
