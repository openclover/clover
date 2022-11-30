package com.atlassian.clover.idea.config;

import com.atlassian.clover.CloverLicense;
import com.atlassian.clover.CloverLicenseDecoder;
import com.atlassian.clover.idea.util.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LicenseConfigPanel extends JPanel implements ActionListener {
    private JPanel summaryPanel;

    private final JLabel licenseStatusLabel = new JLabel();
    private final JLabel licenseTypeLabel = new JLabel();
    private final JLabel licenseMessageLabel = new JLabel();
    private final JTextArea licenseTextArea = new JTextArea();

    public LicenseConfigPanel() {
        initComponents();
    }

    public String getLicenseText() {
        return licenseTextArea.getText();
    }

    public void setLicenseText(String text) {
        licenseTextArea.setText(text);
        verifyLicense();
    }

    private void initComponents() {
        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);

        add(getSummaryPanel(), new GBC(1, 1).setFill(GBC.BOTH).setWeight(1.0, 0.0).setAnchor(GBC.NORTHWEST));
    }

    private static JLabel makeLabel(String text) {
        final JLabel label = new JLabel(text, JLabel.RIGHT);
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }

    private Component getSummaryPanel() {
        if (summaryPanel == null) {
            summaryPanel = new JPanel();
            summaryPanel.setLayout(new GridBagLayout());

            summaryPanel.setBorder(BorderFactory.createTitledBorder("Summary"));

            int vPos = 1;
            summaryPanel.add(makeLabel("Status: "),
                             new GBC(1, ++vPos).setFill(GBC.BOTH).setWeight(0.0, 0.0).setAnchor(GBC.NORTHWEST).setInsets(3, 3, 0, 0));
            summaryPanel.add(licenseStatusLabel,
                             new GBC(2, vPos).setFill(GBC.BOTH).setWeight(1.0, 0.0).setAnchor(GBC.NORTHWEST).setInsets(3, 3, 0, 0));
            summaryPanel.add(makeLabel("Type: "),
                             new GBC(1, ++vPos).setFill(GBC.BOTH).setWeight(0.0, 0.0).setAnchor(GBC.NORTHWEST).setInsets(3, 3, 0, 0));
            summaryPanel.add(licenseTypeLabel,
                             new GBC(2, vPos).setFill(GBC.BOTH).setWeight(1.0, 0.0).setAnchor(GBC.NORTHWEST).setInsets(3, 3, 0, 0));
            summaryPanel.add(makeLabel("Message: "),
                             new GBC(1, ++vPos).setFill(GBC.BOTH).setWeight(0.0, 0.0).setAnchor(GBC.NORTHWEST).setInsets(3, 3, 8, 0));
            summaryPanel.add(licenseMessageLabel,
                             new GBC(2, vPos).setFill(GBC.BOTH).setWeight(1.0, 0.0).setAnchor(GBC.NORTHWEST).setInsets(3, 3, 8, 0));
        }
        return summaryPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("paste".equals(e.getActionCommand())) {
            licenseTextArea.setText("");
            licenseTextArea.paste();
            verifyLicense();
        } else if ("load".equals(e.getActionCommand())) {
            boolean loaded = loadLicenseFile();
            if (loaded) {
                verifyLicense();
            }
        } else if ("verify".equals(e.getActionCommand())) {
            verifyLicense();
        }
    }

    private void verifyLicense() {
        try {
            final CloverLicense license = CloverLicenseDecoder.decode("");
            final String status = "<html><body><font color=\"green\">Licensed</font></body></html>";
            licenseStatusLabel.setText(status);
            licenseTypeLabel.setText(license.getLicenseName());
            licenseMessageLabel.setText(license.getOwnerStatement() != null ? license.getOwnerStatement() : "-");
        } catch (Exception e) {
            licenseStatusLabel.setText("<html><body><font color=\"red\">Invalid</font></body></html>");
            licenseMessageLabel.setText("");
            licenseTypeLabel.setText("");
        }
    }

    private boolean loadLicenseFile() {
        final VirtualFile[] vf = FileChooser.chooseFiles(this, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
        if (vf.length == 1 && !vf[0].isDirectory()) {
            try {
                byte[] contents = vf[0].contentsToByteArray();
                setLicenseText(new String(contents, StandardCharsets.UTF_8));
                return true;
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(null, e1.getMessage(), "Loading license file", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }
}
