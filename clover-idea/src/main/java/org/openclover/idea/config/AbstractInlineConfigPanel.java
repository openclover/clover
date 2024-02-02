package com.atlassian.clover.idea.config;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static org.openclover.util.Lists.newArrayList;

public abstract class AbstractInlineConfigPanel extends ConfigPanel implements ActionListener {

    private List<JCheckBox> checkboxes = null;


    public AbstractInlineConfigPanel() {
        initLayout();
        initListeners();
    }

    private void initLayout() {
        setBorder(getEnabledBorder());

        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);

        List boxes = getCheckboxes();

        int[][] layout = getCheckboxLayout();
        int span = layout[0].length;
        if (getHelpText() != null) {
            add(getHelpText(), new GBC(1, 1).setSpan(span, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0));
        }

        for (int i = 0; i < layout.length; i++) {
            for (int j = 0; j < layout[i].length; j++) {
                int index = layout[i][j];
                if (index != -1) {
                    add((JComponent) boxes.get(index), new GBC(j + 1, i + 2).setAnchor(GBC.WEST));
                }
            }
        }
        add(new JPanel(), new GBC(1, layout.length + 2).setSpan(span, 1).setFill(GBC.BOTH).setWeight(1.0, 1.0));

    }

    private void initListeners() {
        // no active components.
    }

    public abstract JTextArea getHelpText();

    protected List<JCheckBox> getCheckboxes() {
        if (checkboxes == null) {
            checkboxes = newArrayList();
            for (String text : getDisplayNames()) {
                checkboxes.add(new JCheckBox(text));
            }
        }
        return checkboxes;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    public abstract List<String> getDisplayNames();

    public abstract int[][] getCheckboxLayout();

    @Override
    public void enableConfig(boolean b) {
        if (b) {
            setBorder(getEnabledBorder());
        } else {
            setBorder(getDisabledBorder());
        }
        if (getHelpText() != null) {
            getHelpText().setEnabled(b);
        }
        for (JCheckBox box : getCheckboxes()) {
            if (box != null) {
                box.setEnabled(b);
            }
        }
    }
}
