package com.atlassian.clover.idea.config;

import com.atlassian.clover.CloverLicense;
import com.atlassian.clover.CloverLicenseDecoder;
import com.atlassian.clover.LicenseDecoderException;
import com.atlassian.clover.idea.CloverPlugin;
import com.atlassian.clover.idea.util.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com_atlassian_clover.CloverVersionInfo;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    private static final long MAINT_SLACK = 7L * 24L * 60L * 60L * 1000L; // from CloverStartup

    private String lastCheckedLicenseText;

    private void verifyLicense() {
        lastCheckedLicenseText = licenseTextArea.getText();

        if (lastCheckedLicenseText.trim().length() == 0) {
            handleEmptyLicense();
            return;
        }
        CloverLicense license;
        try {
            license = CloverLicenseDecoder.decode(lastCheckedLicenseText);
        } catch (LicenseDecoderException e) {
            licenseStatusLabel.setText("<html><body><font color=\"red\">Invalid</font></body></html>");
            licenseMessageLabel.setText("Please paste a valid license.");
            licenseTypeLabel.setText("");
            return;
        } catch (Exception e) {
            licenseStatusLabel.setText("<html><body><font color=\"red\">Invalid</font></body></html>");
            licenseMessageLabel.setText(e.getLocalizedMessage());
            licenseTypeLabel.setText("");
            return;
        }

        final String status;

        if (license.isExpired()) {
            status = "Your license has expired";
        } else {
            SimpleDateFormat df = new SimpleDateFormat("MMMM dd yyyy", Locale.US);
            long buildDate;
            try {
                buildDate = df.parse(CloverVersionInfo.BUILD_DATE).getTime();
            } catch (ParseException e) {
                throw new RuntimeException("Internal Error: could not determine build date.");
            }
            if (license.maintenanceExpires() && buildDate > (license.getMaintenanceExpiryDate() + MAINT_SLACK)) {
                status = "<html>Clover upgrades for your license ended "
                        + df.format(new Date(license.getMaintenanceExpiryDate()))
                        + ", and this version of Clover was built " + CloverVersionInfo.BUILD_DATE;
            } else {
                final String preExpiry = processPreExpiryStmt(license);
                status = (preExpiry.trim().length() != 0) ? preExpiry : "<html><body><font color=\"green\">Licensed</font></body></html>";
            }
        }

        licenseStatusLabel.setText(status);
        licenseTypeLabel.setText(license.getLicenseName());
        licenseMessageLabel.setText(license.getOwnerStatement() != null ? license.getOwnerStatement() : "-");
    }

    private void handleEmptyLicense() {
        final long installDate = CloverPlugin.getPlugin().getInstallDate();
        final long now = System.currentTimeMillis();
        final long millisLeft = installDate + CloverPlugin.EVALUATION_PERIOD - now;

        if (millisLeft < 0) {
            licenseStatusLabel.setText("<html><body><font color=\"red\">Evaluation license expired");
            licenseMessageLabel.setText("<html>Please generate new evaluation license using the link below if you wish to continue your evaluation.");
        } else {
            long daysLeft = millisLeft / CloverLicense.ONE_DAY;
            licenseStatusLabel.setText("You have " + daysLeft + " day(s) before your evaluation license expires.");
            licenseMessageLabel.setText("");
        }

        licenseTypeLabel.setText("Clover Evaluation License");
    }

    private static String processPreExpiryStmt(CloverLicense license) {
        String preExpiryStmt = license.getPreExpiryStatement();
        if (preExpiryStmt.contains("$daysleft")) {
            long daysLeft = license.getDaysTillExpiry();
            if (daysLeft < 0) {
                daysLeft = 0;
            }
            preExpiryStmt = preExpiryStmt.replace("$daysleft", String.valueOf(daysLeft));
        }
        return preExpiryStmt;
    }

    private boolean loadLicenseFile() {
        final VirtualFile[] vf = FileChooser.chooseFiles(this, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
        if (vf.length == 1 && !vf[0].isDirectory()) {
            try {
                byte[] contents = vf[0].contentsToByteArray();
                setLicenseText(new String(contents, "UTF-8"));
                return true;
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(null, e1.getMessage(), "Loading license file", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }
}
