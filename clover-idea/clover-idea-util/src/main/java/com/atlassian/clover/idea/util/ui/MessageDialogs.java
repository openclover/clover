package com.atlassian.clover.idea.util.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

public class MessageDialogs {

    /**
     *
     * @param parent
     * @param message
     * @param title
     * @return
     */
    public static final int showYesNoDialog(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent,
                                             message,
                                             title,
                                             JOptionPane.YES_NO_OPTION,
                                             JOptionPane.QUESTION_MESSAGE,
                                             null);
    }

    /**
     *
     * @param parent
     * @param message
     * @param title
     */
    public static final void showErrorMessage(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     *
     * @param parent
     * @param message
     * @param title
     */
    public static final void showInfoMessage(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
