package org.openclover.idea.config;

import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation;
import org.openclover.idea.util.ui.UIUtils;
import com.atlassian.clover.util.ArrayUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.ui.UIUtil;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InstrumentationConfigPanel extends ConfigPanel implements ActionListener {

    private JTextArea helpText;

    private JTextField includeField;
    private JLabel includeLabel;
    private JTextField excludeField;
    private JLabel excludeLabel;

    private JCheckBox ignoreTests;
    private JLabel instrumentLambdaLabel;
    private ComboBox instrumentLambdaCombo;

    public InstrumentationConfigPanel() {
        initLayout();
        initListeners();
    }

    private void initLayout() {
        setBorder(getEnabledBorder());

        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);

        int row = 0;
        add(getHelpText(), new GBC(1, ++row).setInsets(0, 6, 5, 6).setSpan(2, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0));

        add(getIncludeLabel(), new GBC(1, ++row).setInsets(0, 6, 5, 5).setAnchor(GBC.WEST).setWeight(0.0, 0.0));
        add(getInclude(), new GBC(2, row).setInsets(0, 0, 5, 6).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0));

        add(getExcludeLabel(), new GBC(1, ++row).setInsets(0, 6, 5, 5).setAnchor(GBC.WEST).setWeight(0.0, 0.0));
        add(getExclude(), new GBC(2, row).setInsets(0, 0, 5, 6).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0));

        add(getInstrumentTests(), new GBC(1, ++row).setInsets(0, 6, 10, 6).setSpan(2, 1).setAnchor(GBC.WEST).setFill(GBC.BOTH).setWeight(1.0, 0.0));

        add(getInstrumentLambdaLabel(), new GBC(1, ++row).setInsets(0, 6, 5, 5).setAnchor(GBC.WEST).setWeight(0.0, 0.0));
        add(getInstrumentLambdaCombo(), new GBC(2, row).setInsets(0, 0, 5, 6).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0));

        add(new JPanel(), new GBC(1, ++row).setSpan(2, 1).setFill(GBC.BOTH).setWeight(0.0, 1.0));
    }

    private void initListeners() {

    }

    private static String processText(String str) {
        if (str == null) {
            return null;
        }
        final String trimmed = str.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    @Override
    public void commitTo(final CloverPluginConfig config) {
        final IdeaCloverConfig ideaConfig = (IdeaCloverConfig) config;
        // a little tricky here - commit null to the config rather then ''.
        // The default value is null, so unless the user enters data, we do not
        // want to be changing things...
        ideaConfig.setIncludes(processText(getInclude().getText()));
        ideaConfig.setExcludes(processText(getExclude().getText()));
        ideaConfig.setInstrumentTests(getInstrumentTests().isSelected());
        ideaConfig.setInstrumentLambda(LambdaInstrumentation.values()[getInstrumentLambdaCombo().getSelectedIndex()]);
    }

    @Override
    public void loadFrom(final CloverPluginConfig config) {
        final IdeaCloverConfig ideaConfig = (IdeaCloverConfig) config;
        getInclude().setText(ideaConfig.getIncludes());
        getExclude().setText(ideaConfig.getExcludes());
        getInstrumentTests().setSelected(ideaConfig.isInstrumentTests());
        getInstrumentLambdaCombo().setSelectedIndex(ideaConfig.getInstrumentLambda().ordinal());
    }

    private JTextArea getHelpText() {
        if (helpText == null) {
            helpText = UIUtils.getHelpTextArea("Fine tune which source " +
                    "files you want instrumented by Clover. Specify Ant style " +
                    "patternsets to include or exclude particular source files " +
                    "(comma or space separated).",
                                               getBackground(), getFont());
        }
        return helpText;
    }

    private JTextField getInclude() {
        if (includeField == null) {
            includeField = new JTextField();
        }
        return includeField;
    }

    private JLabel getIncludeLabel() {
        if (includeLabel == null) {
            includeLabel = new JLabel("Includes:");
            includeLabel.setAlignmentX(RIGHT_ALIGNMENT);
        }
        return includeLabel;
    }

    private JTextField getExclude() {
        if (excludeField == null) {
            excludeField = new JTextField();
        }
        return excludeField;
    }

    private JLabel getExcludeLabel() {
        if (excludeLabel == null) {
            excludeLabel = new JLabel("Excludes:");
            excludeLabel.setAlignmentX(RIGHT_ALIGNMENT);
        }
        return excludeLabel;
    }

    private JCheckBox getInstrumentTests() {
        if (ignoreTests == null) {
            ignoreTests = new JCheckBox("<html>Instrument test source folders to enable per-test coverage and test optimization.");
        }
        return ignoreTests;
    }

    public JLabel getInstrumentLambdaLabel() {
        if (instrumentLambdaLabel == null) {
            instrumentLambdaLabel = new JLabel("Instrument lambda functions:");
            instrumentLambdaLabel.setAlignmentX(RIGHT_ALIGNMENT);
        }
        return instrumentLambdaLabel;
    }

    public ComboBox getInstrumentLambdaCombo() {
        if (instrumentLambdaCombo == null) {
            instrumentLambdaCombo = new ComboBox(ArrayUtil.toLowerCaseStringArray(LambdaInstrumentation.values()), 100);
            instrumentLambdaCombo.setToolTipText(
                    "<html><body>Select whether lambda functions introduced in Java8 shall be instrumented by Clover so that you can track <br/>" +
                    "code coverage for them and show them in reports similarly as normal methods. <br/>" +
                    "You can also limit instrumentation to certain forms of lambda functions: <br/>" +
                    " <li> written as expressions, e.g. '(a + b) -> a + b' </li>" +
                    " <li> written as code blocks, e.g. '() -> { return xyz(); }'</li>" +
                    " <li> written in any form except method references, e.g. 'Math::abs'</li></body></html>");

        }
        return instrumentLambdaCombo;
    }

    @Override
    public String getTitle() {
        return "Instrumentation";
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void enableConfig(boolean b) {
        super.enableConfig(b);
        getInstrumentTests().setForeground(b ? UIUtil.getActiveTextColor() : UIUtil.getInactiveTextColor());
    }
}
