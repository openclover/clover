package com.atlassian.clover.idea.config;

import com.atlassian.clover.idea.util.ui.UIUtils;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SourceHighlightConfigPanel extends ConfigPanel {

    private JColorSelectionBox errorStripeBox;
    private JColorSelectionBox errorHighlightBox;
    private JColorSelectionBox oodStripeBox;
    private JColorSelectionBox oodHighlightBox;
    private JColorSelectionBox coveredStripeBox;
    private JColorSelectionBox coveredHighlightBox;
    private JColorSelectionBox coveredFailedHighlightBox;
    private JColorSelectionBox coveredFailedStripeBox;
    private JColorSelectionBox filteredHighlightBox;
    private JColorSelectionBox filteredStripeBox;

    private JTextArea helpText;
    private JPanel colourPanel;

    public SourceHighlightConfigPanel() {
        initComponents();
    }

    private void initComponents() {
        setBorder(getEnabledBorder());

        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);

        add(getHelpText(), new GBC(0, 0).setInsets(6, 6, 10, 6).setSpan(1, 1).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST).setWeight(1.0, 0.0));
        add(getColourPanel(), new GBC(0, 1).setInsets(0, 6, 6, 6).setSpan(1, 1).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST).setWeight(1.0, 0.0));
    }

    private JTextArea getHelpText() {
        if (helpText == null) {
            helpText = UIUtils.getHelpTextArea("The source highlighting " +
                    "configurations allow you to customise the colour of " +
                    "source highlights and gutter marks.",
                                               getBackground(), getFont());
        }
        return helpText;
    }

    private JPanel getColourPanel() {
        if (colourPanel == null) {
            colourPanel = new JPanel();

            final FormLayout formLayout = new FormLayout("p, p, p, p, p, p, p:grow", "p, p");
            formLayout.setColumnGroups(new int[][]{{2, 3, 4, 5, 6}});
            colourPanel.setLayout(formLayout);
            final CellConstraints cc = new CellConstraints();

            colourPanel.add(new JLabel("Stripe:", JLabel.RIGHT), cc.xy(1, 1));
            colourPanel.add(getErrorStripeBox(), cc.xy(2, 1));
            colourPanel.add(getFailedCoveredStripeBox(), cc.xy(3, 1));
            colourPanel.add(getCoveredStripeBox(), cc.xy(4, 1));
            colourPanel.add(getFilteredStripeBox(), cc.xy(5, 1));
            colourPanel.add(getOODStripeBox(), cc.xy(6, 1));

            colourPanel.add(new JLabel("Highlight:", JLabel.RIGHT), cc.xy(1, 2));
            colourPanel.add(getErrorHighlightBox(), cc.xy(2, 2));
            colourPanel.add(getFailedCoveredHighlightBox(), cc.xy(3, 2));
            colourPanel.add(getCoveredHighlightBox(), cc.xy(4, 2));
            colourPanel.add(getFilteredHighlightBox(), cc.xy(5, 2));
            colourPanel.add(getOODHighlightBox(), cc.xy(6, 2));
        }
        return colourPanel;
    }

    private JColorSelectionBox getErrorStripeBox() {
        if (errorStripeBox == null) {
            errorStripeBox = createColourBox("Uncovered", "Choose error stripe color for zero coverage");
        }
        return errorStripeBox;
    }

    private JColorSelectionBox getErrorHighlightBox() {
        if (errorHighlightBox == null) {
            errorHighlightBox = createColourBox("Uncovered", "Choose error highlight color for zero coverage");
        }
        return errorHighlightBox;
    }

    private JColorSelectionBox getOODStripeBox() {
        if (oodStripeBox == null) {
            oodStripeBox = createColourBox("Out-of-date", "Choose out-of-date stripe color for zero coverage");
        }
        return oodStripeBox;
    }

    private JColorSelectionBox getOODHighlightBox() {
        if (oodHighlightBox == null) {
            oodHighlightBox = createColourBox("Out-of-date", "Choose out-of-date highlight color for zero coverage");
        }
        return oodHighlightBox;
    }

    private JColorSelectionBox getCoveredStripeBox() {
        if (coveredStripeBox == null) {
            coveredStripeBox = createColourBox("Covered", "Choose stripe color for covered code");
        }
        return coveredStripeBox;
    }

    private JColorSelectionBox getCoveredHighlightBox() {
        if (coveredHighlightBox == null) {
            coveredHighlightBox = createColourBox("Covered", "Choose highlight color for covered code");
        }
        return coveredHighlightBox;
    }

    private JColorSelectionBox getFailedCoveredStripeBox() {
        if (coveredFailedStripeBox == null) {
            coveredFailedStripeBox = createColourBox("Failed", "Choose stripe color for code covered by failed tests only");
        }
        return coveredFailedStripeBox;
    }

    private JColorSelectionBox getFailedCoveredHighlightBox() {
        if (coveredFailedHighlightBox == null) {
            coveredFailedHighlightBox = createColourBox("Failed", "Choose highlight color for code covered by failed tests only");
        }
        return coveredFailedHighlightBox;
    }

    private JColorSelectionBox getFilteredStripeBox() {
        if (filteredStripeBox == null) {
            filteredStripeBox = createColourBox("Excluded", "Choose stripe color for excluded code");
        }
        return filteredStripeBox;
    }

    private JColorSelectionBox getFilteredHighlightBox() {
        if (filteredHighlightBox == null) {
            filteredHighlightBox = createColourBox("Excluded", "Choose highlight color for excluded code");
        }
        return filteredHighlightBox;
    }

    private JColorSelectionBox createColourBox(String txt, String popupTxt) {
        return new JColorSelectionBox(txt, popupTxt);
    }

    private static class JColorSelectionBox extends JPanel {

        JColorSelectionBox(final String txt, final String colorSelectionTxt) {
            add(new JLabel(txt), new Insets(8, 8, 8, 8));
            setBorder(BorderFactory.createLineBorder(getBackground(), 2));

            final JColorSelectionBox self = this;
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        Color initialColour = self.getSelectedColour() != null ?
                                self.getSelectedColour() : getBackground();
                        Color newColor = JColorChooser.showDialog(self, colorSelectionTxt, initialColour);
                        if (newColor != null) {
                            // colour selection has been made
                            setSelectedColour(newColor);
                        }
                    });
                }
            });
        }

        void setSelectedColour(Color c) {
            setBackground(c);
        }

        Color getSelectedColour() {
            return getBackground();
        }
    }

    @Override
    public void commitTo(CloverPluginConfig config) {
        config.setCoveredHighlight(getCoveredHighlightBox().getSelectedColour());
        config.setCoveredStripe(getCoveredStripeBox().getSelectedColour());
        config.setNotCoveredHighlight(getErrorHighlightBox().getSelectedColour());
        config.setNotCoveredStripe(getErrorStripeBox().getSelectedColour());
        config.setOutOfDateHighlight(getOODHighlightBox().getSelectedColour());
        config.setOutOfDateStripe(getOODStripeBox().getSelectedColour());
        config.setFailedCoveredHighlight(getFailedCoveredHighlightBox().getSelectedColour());
        config.setFailedCoveredStripe(getFailedCoveredStripeBox().getSelectedColour());
        config.setFilteredHighlight(getFilteredHighlightBox().getSelectedColour());
        config.setFilteredStripe(getFilteredStripeBox().getSelectedColour());
    }

    @Override
    public void loadFrom(CloverPluginConfig config) {
        getCoveredHighlightBox().setSelectedColour(config.getCoveredHighlight());
        getCoveredStripeBox().setSelectedColour(config.getCoveredStripe());
        getErrorHighlightBox().setSelectedColour(config.getNotCoveredHighlight());
        getErrorStripeBox().setSelectedColour(config.getNotCoveredStripe());
        getOODHighlightBox().setSelectedColour(config.getOutOfDateHighlight());
        getOODStripeBox().setSelectedColour(config.getOutOfDateStripe());
        getFailedCoveredHighlightBox().setSelectedColour(config.getFailedCoveredHighlight());
        getFailedCoveredStripeBox().setSelectedColour(config.getFailedCoveredStripe());
        getFilteredHighlightBox().setSelectedColour(config.getFilteredHighlight());
        getFilteredStripeBox().setSelectedColour(config.getFilteredStripe());
    }

    @Override
    public String getTitle() {
        return "Source Highlighting";
    }

    @Override
    public void enableConfig(boolean b) {
        super.enableConfig(b);
    }
}

