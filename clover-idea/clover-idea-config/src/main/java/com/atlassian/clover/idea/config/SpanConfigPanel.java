package com.atlassian.clover.idea.config;

import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.idea.util.ui.UIUtils;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.GridBagLayout;

public class SpanConfigPanel extends ConfigPanel {

    private JTextArea helpText;

    private final JTextField spanField = new JTextField(8);
    private final JLabel spanLabel = new JLabel("Span: ");
    private final JLabel validationMessage = new JLabel();

    public SpanConfigPanel() {
        spanField.setMinimumSize(spanField.getPreferredSize());
        validationMessage.setForeground(Color.RED);

        initLayout();
        initListeners();
    }

    private void initLayout() {
        setBorder(getEnabledBorder());

        setLayout(new GridBagLayout());

        add(getHelpText(), new GBC(1, 1).setInsets(0, 6, 5, 6).setSpan(3, 1).setFill(GBC.HORIZONTAL).setWeight(1.0, 1));
        add(spanLabel, new GBC(1, 2).setInsets(0, 6, 6, 0).setFill(GBC.HORIZONTAL));
        add(spanField, new GBC(2, 2).setInsets(0, 0, 6, 0).setFill(GBC.HORIZONTAL));
        add(validationMessage, new GBC(3, 2).setInsets(0, 6, 6, 0).setFill(GBC.BOTH));
    }

    private void initListeners() {
        spanField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                verify();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                verify();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                verify();
            }

            private void verify() {
                final String span = spanField.getText();
                try {
                    if (span.length() != 0) {
                        new Interval(span);
                    }
                    validationMessage.setText("");
                } catch (NumberFormatException ex) {
                    validationMessage.setText("Invalid span");
                }
            }
        });
    }

    @Override
    public String getTitle() {
        return "Span";
    }

    private JTextArea getHelpText() {
        if (helpText == null) {
            helpText = UIUtils.getHelpTextArea("Specifies how far back data should be loaded, since last compile. " +
                    "(e.g. '30 s', '3 days', '2 mo', '1 year', or just blank to include all data since the last full rebuild)",
                                               getBackground(), getFont());
        }
        return helpText;
    }

    @Override
    public void commitTo(CloverPluginConfig config) {
        try {
            String spanText = spanField.getText();
            if (spanText.equals("")) {
                spanText = "0";
            }
            Interval span = new Interval(spanText);
            config.setSpan(span.toString());
        } catch (NumberFormatException e) {
            config.setSpan("0");
        }
    }

    @Override
    public void loadFrom(CloverPluginConfig config) {
        String span = config.getSpan();
        if (span != null && span.startsWith("0")) {
            span = "";
        }
        spanField.setText(span);
    }

    public static void main(String[] argv) {
        ConfigPanelRunner.run(new SpanConfigPanel());
    }
}
