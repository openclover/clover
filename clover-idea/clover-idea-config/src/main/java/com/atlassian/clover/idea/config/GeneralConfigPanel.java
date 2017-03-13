package com.atlassian.clover.idea.config;

import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public class GeneralConfigPanel extends AbstractInlineConfigPanel {

    private static final List<String> DISPLAY_NAMES = newArrayList();

    static {
        DISPLAY_NAMES.add("Show gutter marks");
        DISPLAY_NAMES.add("Show shortcut marks");
        DISPLAY_NAMES.add("Show tooltips");
        DISPLAY_NAMES.add("Show summary in main toolbar");
        DISPLAY_NAMES.add("Show source highlights");
    }

    private static final int[][] LAYOUT = new int[][]{
            new int[]{0},
            new int[]{1},
            new int[]{2},
            new int[]{3},
            new int[]{4}
    };

    @Override
    public List<String> getDisplayNames() {
        return DISPLAY_NAMES;
    }

    @Override
    public int[][] getCheckboxLayout() {
        return LAYOUT;
    }

    @Override
    public JTextArea getHelpText() {
        return null;
    }

    @Override
    public String getTitle() {
        return "General";
    }

    @Override
    public void commitTo(CloverPluginConfig config) {
        IdeaCloverConfig iConfig = (IdeaCloverConfig) config;
        iConfig.setShowGutter(((JCheckBox) getCheckboxes().get(0)).isSelected());
        iConfig.setShowErrorMarks(((JCheckBox) getCheckboxes().get(1)).isSelected());
        iConfig.setShowTooltips(((JCheckBox) getCheckboxes().get(2)).isSelected());
        iConfig.setShowSummaryInToolbar(((JCheckBox) getCheckboxes().get(3)).isSelected());
        iConfig.setShowInline(((JCheckBox) getCheckboxes().get(4)).isSelected());
    }

    @Override
    public void loadFrom(CloverPluginConfig config) {
        IdeaCloverConfig iConfig = (IdeaCloverConfig) config;
        ((JCheckBox) getCheckboxes().get(0)).setSelected(iConfig.isShowGutter());
        ((JCheckBox) getCheckboxes().get(1)).setSelected(iConfig.isShowErrorMarks());
        ((JCheckBox) getCheckboxes().get(2)).setSelected(iConfig.isShowTooltips());
        ((JCheckBox) getCheckboxes().get(3)).setSelected(iConfig.isShowSummaryInToolbar());
        ((JCheckBox) getCheckboxes().get(4)).setSelected(iConfig.isShowInline());
    }

}
