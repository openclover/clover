package org.openclover.idea.report;

import org.openclover.idea.config.GBC;
import org.openclover.idea.util.ui.UIUtils;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * UI Component that allows the user to select which type of report they would
 * like to generate.
 */
public class SelectReportUI extends ReportWizardPage {

    private JTextArea helpText;

    private JRadioButton htmlButton;
    private JRadioButton xmlButton;
    private JRadioButton pdfButton;
    private ButtonGroup buttonGroup;

    private JLabel htmlLabel = null;
    private JLabel pdfLabel = null;
    private JLabel xmlLabel = null;

    private JLabel htmlFileType = null;
    private JLabel pdfFileType = null;
    private JLabel xmlFileType = null;

    public SelectReportUI(ReportWizard wiz) {
        super(wiz);
        initComponents();
        initListeners();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEtchedBorder());
        add(getHelpText(), new GBC(0, 0).setSpan(3, 1).setInsets(5, 5, 5, 5).setAnchor(GBC.NORTHWEST).setFill(GBC.HORIZONTAL));
        add(getHtmlButton(), new GBC(0, 1).setInsets(5, 5, 5, 0).setAnchor(GBC.NORTHWEST).setFill(GBC.HORIZONTAL));
        add(getHtmlIconLabel(), new GBC(1, 1).setFill(GBC.BOTH));
        add(getHtmlButtonLabel(), new GBC(2, 1).setInsets(5, 0, 5, 5).setAnchor(GBC.NORTHWEST).setFill(GBC.HORIZONTAL).setWeight(0.01, 0.0));
        add(getPdfButton(), new GBC(0, 2).setInsets(0, 5, 5, 0).setAnchor(GBC.NORTHWEST).setFill(GBC.HORIZONTAL));
        add(getPdfIconLabel(), new GBC(1, 2).setFill(GBC.BOTH));
        add(getPdfButtonLabel(), new GBC(2, 2).setInsets(0, 0, 5, 5).setAnchor(GBC.NORTHWEST).setFill(GBC.HORIZONTAL));
        add(getXmlButton(), new GBC(0, 3).setInsets(0, 5, 5, 0).setAnchor(GBC.NORTHWEST).setFill(GBC.HORIZONTAL));
        add(getXmlIconLabel(), new GBC(1, 3).setFill(GBC.BOTH));
        add(getXmlButtonLabel(), new GBC(2, 3).setInsets(0, 0, 5, 5).setAnchor(GBC.NORTHWEST).setFill(GBC.HORIZONTAL));
        add(new JPanel(), new GBC(0, 6).setFill(GBC.VERTICAL).setWeight(0.0, 0.1));
    }

    private void initListeners() {
        MouseListener htmlListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (wizard.isHtmlAvailable()) {
                    getHtmlButton().setSelected(true);
                }
            }
        };
        getHtmlButtonLabel().addMouseListener(htmlListener);
        getHtmlIconLabel().addMouseListener(htmlListener);

        MouseListener pdfListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (wizard.isPdfAvailable()) {
                    getPdfButton().setSelected(true);
                }
            }
        };
        getPdfButtonLabel().addMouseListener(pdfListener);
        getPdfIconLabel().addMouseListener(pdfListener);

        MouseListener xmlListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (wizard.isXmlAvailable()) {
                    getXmlButton().setSelected(true);
                }
            }
        };
        getXmlButtonLabel().addMouseListener(xmlListener);
        getXmlIconLabel().addMouseListener(xmlListener);
    }

    private JTextArea getHelpText() {
        if (helpText == null) {
            helpText = UIUtils.getHelpTextArea("Please select the type of report " +
                    "you would like to generate.",
                                               getBackground(), getFont());
        }
        return helpText;
    }

    private ButtonGroup getButtonGroup() {
        if (buttonGroup == null) {
            buttonGroup = new ButtonGroup();
        }
        return buttonGroup;
    }

    private JLabel getHtmlButtonLabel() {
        if (htmlLabel == null) {
            htmlLabel = new JLabel("Html Report", /*CloverIcons.HTML_FILETYPE*/null, JLabel.LEFT);
            htmlLabel.setEnabled(wizard.isHtmlAvailable());
            if (!htmlLabel.isEnabled()) {
                htmlLabel.setToolTipText("You are not licensed to generate Html reports.");
            }
        }
        return htmlLabel;
    }

    private JLabel getHtmlIconLabel() {
        if (htmlFileType == null) {
            htmlFileType = new JLabel();
            htmlFileType.setEnabled(wizard.isHtmlAvailable());
            if (!htmlFileType.isEnabled()) {
                htmlFileType.setToolTipText("You are not licensed to generate Html reports.");
            }
        }
        return htmlFileType;
    }

    private JLabel getXmlButtonLabel() {
        if (xmlLabel == null) {
            xmlLabel = new JLabel("Xml Report", /*CloverIcons.XML_FILETYPE*/null, JLabel.LEFT);
            xmlLabel.setEnabled(wizard.isXmlAvailable());
            if (!xmlLabel.isEnabled()) {
                xmlLabel.setToolTipText("You are not licensed to generate Xml reports.");
            }
        }
        return xmlLabel;
    }

    private JLabel getXmlIconLabel() {
        if (xmlFileType == null) {
            xmlFileType = new JLabel();
            xmlFileType.setEnabled(wizard.isXmlAvailable());
            if (!xmlFileType.isEnabled()) {
                xmlFileType.setToolTipText("You are not licensed to generate Xml reports.");
            }
        }
        return xmlFileType;
    }

    private JLabel getPdfButtonLabel() {
        if (pdfLabel == null) {
            pdfLabel = new JLabel("Pdf Report", /*CloverIcons.PDF_FILETYPE*/null, JLabel.LEFT);
            pdfLabel.setEnabled(wizard.isPdfAvailable());
            if (!pdfLabel.isEnabled()) {
                pdfLabel.setToolTipText("You are not licensed to generate Pdf reports.");
            }
        }
        return pdfLabel;
    }

    private JLabel getPdfIconLabel() {
        if (pdfFileType == null) {
            pdfFileType = new JLabel();
            pdfFileType.setEnabled(wizard.isPdfAvailable());
            if (!pdfFileType.isEnabled()) {
                pdfFileType.setToolTipText("You are not licensed to generate Pdf reports.");
            }
        }
        return pdfFileType;
    }

    private JRadioButton getHtmlButton() {
        if (htmlButton == null) {
            htmlButton = new JRadioButton();
            htmlButton.setEnabled(wizard.isHtmlAvailable());
            getButtonGroup().add(htmlButton);
        }
        return htmlButton;
    }

    private JRadioButton getXmlButton() {
        if (xmlButton == null) {
            xmlButton = new JRadioButton();
            xmlButton.setEnabled(wizard.isXmlAvailable());
            getButtonGroup().add(xmlButton);
        }
        return xmlButton;
    }

    private JRadioButton getPdfButton() {
        if (pdfButton == null) {
            pdfButton = new JRadioButton();
            pdfButton.setEnabled(wizard.isPdfAvailable());
            getButtonGroup().add(pdfButton);
        }
        return pdfButton;
    }

    @Override
    public void readConfig(WizardConfig config) {
        // get the code index.
        if (config.isHtml()) {
            getHtmlButton().setSelected(true);
        } else if (config.isXml()) {
            getXmlButton().setSelected(true);
        } else if (config.isPdf()) {
            getPdfButton().setSelected(true);
        }
    }

    @Override
    public void writeConfig(WizardConfig reportConfig) {
        if (getHtmlButton().isSelected()) {
            reportConfig.setType(WizardConfig.HTML);
        } else if (getXmlButton().isSelected()) {
            reportConfig.setType(WizardConfig.XML);
        } else if (getPdfButton().isSelected()) {
            reportConfig.setType(WizardConfig.PDF);
        }
    }

    @Override
    public String validateSettings() {
        return null;
    }

    public void setPdfFileTypeIcon(Icon i) {
        getPdfIconLabel().setIcon(i);
    }

    public void setXmlFileTypeIcon(Icon i) {
        getXmlIconLabel().setIcon(i);
    }

    public void setHtmlFileTypeIcon(Icon i) {
        getHtmlIconLabel().setIcon(i);
    }

}
