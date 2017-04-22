package com.atlassian.clover.idea;

import com.atlassian.clover.CloverLicense;
import com.atlassian.clover.CloverLicenseInfo;
import com.atlassian.clover.idea.config.IdeaCloverConfig;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.atlassian.clover.idea.util.l10n.CloverIdeaPluginMessages;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.MessageFormat;

public class AboutDialog extends DialogWrapper {
    private final Project project;
    private JTabbedPane tabs;
    private final JCheckBox enabledCheckbox = new JCheckBox("Enable for current project");

    public AboutDialog(Project project) {
        super(project, false);
        this.project = project;
        if (project != null && !CloverLicenseInfo.TERMINATED) {
            enabledCheckbox.setSelected(ProjectPlugin.getPlugin(project).getConfig().isEnabled());
        } else {
            enabledCheckbox.setSelected(false);
            enabledCheckbox.setEnabled(false);
        }
        init();
    }

    @Override
    protected void doOKAction() {
        if (project != null) {
            final IdeaCloverConfig cloverConfig = ProjectPlugin.getPlugin(project).getConfig();
            cloverConfig.setEnabled(enabledCheckbox.isSelected());
            cloverConfig.notifyListeners();
        }
        super.doOKAction();
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        tabs = new JTabbedPane();
        tabs.addTab("Installation", getInstallationPanel());
        tabs.addTab("Acknowledgements", getAcknowledgementsPanel());

        return tabs;
    }

    private Component getInstallationPanel() {
        return makeEditorPane(getInstallationText());
    }

    private Component getAcknowledgementsPanel() {
        return makeEditorPane(getAcknowledgementsText());
    }

    private static JEditorPane makeEditorPane(String contents) {
        final JEditorPane panel = new JEditorPane("text/html", contents);
        panel.setEditable(false);
        panel.setOpaque(false);
        panel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        panel.addHyperlinkListener(new MyHyperlinkListener());

        return panel;
    }

    private String getAcknowledgementsText() {
        StringBuffer sb = new StringBuffer("<p>OpenClover is based on open source code of Atlassian Clover&reg;</p>");
        sb.append("<p>OpenClover makes use of the following 3rd party libraries:</p>");
        sb.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\">");
        addRow(sb,
                addProduct("Annotations (IntelliJ)", "http://www.jetbrains.com", "ANNOTATIONS-13.0-LICENSE.TXT"),
                addProduct("Ant", "http://ant.apache.org", "ANT-1.5.2-LICENSE.TXT"));
        addRow(sb,
                addProduct("ANTLR2 Library", "http://www.antlr2.org", "ANTLR-2.7.7-LICENSE.TXT"),
                addProduct("ANTLR3 Java Grammar", "http://www.antlr3.org", "ANTLR-JAVA-GRAMMAR-3.0-LICENSE.TXT"));
        addRow(sb,
                addProduct("Cajo", "http://java.net/projects/cajo/pages/Home", "CAJO-1.117-LICENSE.TXT"),
                addProduct("Commons Codec", "http://commons.apache.org", "COMMONS-CODEC-1.9-LICENSE.TXT"));
        addRow(sb,
                addProduct("Commons Collections", "http://commons.apache.org", "COMMONS-COLLECTIONS-3.2.1-LICENSE.TXT"),
                addProduct("Commons Lang", "http://commons.apache.org", "COMMONS-LANG3-3.3.2-LICENSE.TXT"));
        addRow(sb,
                addProduct("FastUtil", "http://fastutil.dsi.unimi.it/", "FASTUTIL-4.4.3-LICENSE.TXT"),
                addProduct("Groovy", "http://groovy.codehaus.org", "GROOVY-1.7.0-LICENSE.TXT"));
        addRow(sb,
                addProduct("GSON", "http://code.google.com/p/google-gson", "GSON-1.3-LICENSE.TXT"),
                addProduct("Guava", "http://code.google.com/p/guava-libraries/", "GUAVA-18.0-LICENSE.TXT"));
        addRow(sb,
                addProduct("iText", "http://itextpdf.com", "ITEXT-2.0.1-LICENSE.TXT"),
                addProduct("JCommon", "http://www.jfree.org/jfreechart/", "JCOMMON-1.0.23-LICENSE.TXT"));
        addRow(sb,
                addProduct("JDOM", "http://www.jdom.org/", "JDOM-1.0-LICENSE.TXT"),
                addProduct("JFreeChart", "http://www.jfree.org/jfreechart/", "JFREECHART-1.0.19-LICENSE.TXT"));
        addRow(sb,
                addProduct("JTreemap", "http://jtreemap.sourceforge.net/", "JTREEMAP-1.1.0-LICENSE.TXT"),
                addProduct("JIT", "http://thejit.org/", "JIT-1.1.2-LICENSE.TXT"));
        addRow(sb,
                addProduct("JSON", "http://www.json.org", "JSON-LICENSE.TXT"),
                addProduct("jQuery", "http://jquery.com", "JQUERY-1.8.3-LICENSE.TXT"));
        addRow(sb,
                addProduct("Log4j", "http://logging.apache.org/log4j/1.2", "LOG4J-1.2.17-LICENSE.TXT"),
                addProduct("Velocity", "http://velocity.apache.org/", "VELOCITY-1.7-LICENSE.TXT"));

        addRow(sb,
                "<td colspan=\"4\">OpenClover also reuses some icons from:</td>");
        addRow(sb,
                addProduct("IntelliJ IDEA", "http://www.jetbrains.com/", "INTELLIJ-9.0-ICONS-LICENSE.TXT"),
                "<td colspan=\"2\">&nbsp;</td>");
        sb.append("</table>");
        sb.append("<p>This product includes software developed by the<br/>Apache Software Foundation (<a href=\"http://www.apache.org/\">http://www.apache.org/</a>)</p>");

        return sb.toString();
    }

    // Notice the file://title/path hack used to display dialog title
    private static final MessageFormat LICENSE_ROW = new MessageFormat(
            "<td><a href=\"{1}\">{0}</a></td><td align=\"right\"><a href=\"file://{0}/licenses/{2}\">License</a></td>\n");

    private void addRow(StringBuffer sb, String ... cells) {
        sb.append("<tr>");
        for (String cell : cells) {
            sb.append(cell);
        }
        sb.append("</tr>");
    }
    private String addProduct(String name, String url, String license) {
        return LICENSE_ROW.format(new Object[]{name, url, license});
    }

    private String getInstallationText() {
        final StringBuffer sb = new StringBuffer();
        sb.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\">");

        addRow(sb,
                "<td>Version:</td>",
                "<td>" + PluginVersionInfo.RELEASE_NUMBER + " (" + PluginVersionInfo.BUILD_NUMBER + ")" + "</td>");
        addRow(sb,
                "<td>License:</td>",
                "<td>" + getLicenseInfo() + "</td>");
        addRow(sb,
                "<td colspan=\"2\">" + CloverIdeaPluginMessages.getString("clover.copyright") + "</td>");

        sb.append("</table>");
        return sb.toString();
    }

    private String getLicenseInfo() {
        final String licenseInfo;
        if (CloverLicenseInfo.TERMINATED) {
            licenseInfo = CloverLicenseInfo.TERMINATION_STMT;
        } else {
            StringBuilder sb = new StringBuilder(CloverLicenseInfo.OWNER_STMT);
            final String msg = CloverLicenseInfo.EXPIRED ? CloverLicenseInfo.POST_EXPIRY_STMT : CloverLicenseInfo.PRE_EXPIRY_STMT;
            if (msg.length() > 0) {
                sb.append("<br>").append(msg);
            }
            CloverLicense currentLicense = CloverPlugin.getPlugin().getLicense();
            if (currentLicense != null && currentLicense.isMaintenanceExpired()) {
                sb.append("<br>").append(CloverIdeaPluginMessages.LICENSE_MAINTENANCE_RENEW());
            }
            licenseInfo = sb.toString();
        }
        return licenseInfo;
    }

    @Override
    protected JComponent createSouthPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(super.createSouthPanel(), BorderLayout.EAST);
        enabledCheckbox.setVerticalAlignment(SwingConstants.CENTER);
        enabledCheckbox.setBorder(IdeBorderFactory.createEmptyBorder(new Insets(SystemInfo.isMacOSLeopard ? 8 : 16, 0, 0, 0)));
        panel.add(enabledCheckbox, BorderLayout.WEST);

        return panel;
    }

    @Override
    protected JComponent createNorthPanel() {
        JLabel label = new JLabel("", CloverIcons.CLOVER_LOGO, JLabel.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        return label;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }
}

class MyHyperlinkListener implements HyperlinkListener {
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            final URL url = e.getURL();
            if ("file".equals(url.getProtocol())) {
                displayFile(url);
            } else {
                BrowserUtil.browse(url);
            }
        }
    }

    private void displayFile(URL url) {
        // use the file://title/path hack
        final String title = url.getHost() + " license";
        final String path = url.getPath();

        Reader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return;
        }
        final StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int read = 0;
        try {
            try {
                while (read != -1) {
                    sb.append(buf, 0, read);
                    read = reader.read(buf);
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Messages.showMessageDialog(sb.toString(), title, null);

    }

}
