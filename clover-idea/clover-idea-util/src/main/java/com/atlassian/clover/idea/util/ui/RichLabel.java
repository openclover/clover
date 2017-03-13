package com.atlassian.clover.idea.util.ui;

import com.intellij.util.ui.UIUtil;

import javax.swing.JLabel;

public class RichLabel extends JLabel {
    public RichLabel(String text) {
        super(text);
    }

    public RichLabel() {
        super();
    }

    @Override
    public void setEnabled(boolean enabled) {
        setForeground(enabled ? UIUtil.getActiveTextColor() : UIUtil.getInactiveTextColor());
        super.setEnabled(enabled);
    }
}
