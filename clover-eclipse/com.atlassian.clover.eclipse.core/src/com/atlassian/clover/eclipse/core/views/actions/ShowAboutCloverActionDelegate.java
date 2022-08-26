package com.atlassian.clover.eclipse.core.views.actions;

import com.atlassian.clover.CloverLicenseInfo;
import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import com.atlassian.clover.eclipse.core.CloverPlugin;
import com.atlassian.clover.eclipse.core.PluginVersionInfo;
import com.atlassian.clover.eclipse.core.ui.BrowserUtils;
import com.atlassian.clover.eclipse.core.ui.CloverPluginIcons;
import com.atlassian.clover.eclipse.core.ui.SwtUtils;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ShowAboutCloverActionDelegate extends CloverProjectActionDelegate {

    class AboutDialog extends Dialog {
        public AboutDialog(Shell parentShell) {
            super(parentShell);
        }

        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText("About OpenClover Plugin for Eclipse");
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite top = new Composite(parent, SWT.NULL);
            top.setLayout(new GridLayout(1, false));

            Label iconLabel = new Label(top, SWT.NONE);
            iconLabel.setImage(CloverPlugin.getImage(CloverPluginIcons.CLOVER_LOGO_ICON));
            iconLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

            TabFolder tabFolder = new TabFolder(top, SWT.NONE);
            tabFolder.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

            createInstallationTab(tabFolder);
            createAckTab(tabFolder);

            tabFolder.pack();
            
            return tabFolder;
        }

        private Composite createInstallationTab(TabFolder tabFolder) {
            Composite topTabItem = new Composite(tabFolder, SWT.NONE);
            topTabItem.setLayout(new GridLayout(1, false));

            TabItem installationAbout = new TabItem(tabFolder, SWT.NONE);
            installationAbout.setText("Installation");
            installationAbout.setControl(topTabItem);

            Composite aboutComposite = new Composite(topTabItem, SWT.NONE);
            aboutComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            aboutComposite.setLayout(new GridLayout(2, false));

            // 1st row
            new Label(aboutComposite, SWT.NONE).setText("Version:");
            SwtUtils.createMultilineLabel(
                    aboutComposite,
                    PluginVersionInfo.RELEASE_NUM + " (" + PluginVersionInfo.BUILD_NUMBER + ")",
                    convertWidthInCharsToPixels(60));

            // 2nd row
            new Label(aboutComposite, SWT.NONE).setText("License:");
            SwtUtils.createMultilineLabel(
                    aboutComposite,
                    getLicenseDescription(),
                    convertWidthInCharsToPixels(60));

            // 4th row
            new Label(aboutComposite, SWT.NONE); // empty label
            SwtUtils.createMultilineLabel(
                    aboutComposite,
                    CloverEclipsePluginMessages.CLOVER_COPYRIGHT(),
                    convertWidthInCharsToPixels(60));

            return aboutComposite;
        }

        private String getLicenseDescription() {
            final String msg;
            if (CloverLicenseInfo.TERMINATED) {
                msg = CloverLicenseInfo.TERMINATION_STMT;
            } else if (CloverLicenseInfo.EXPIRED) {
                msg = CloverLicenseInfo.OWNER_STMT + "\n" + CloverLicenseInfo.POST_EXPIRY_STMT;
            } else {
                msg = CloverLicenseInfo.OWNER_STMT + "\n" + CloverLicenseInfo.PRE_EXPIRY_STMT;
            }
            return msg;
        }

        private void createAckTab(TabFolder tabFolder) {
            Composite topAck = new Composite(tabFolder, SWT.NONE);
            topAck.setLayout(new GridLayout(1, false));

            Composite ackComposite = new Composite(topAck, SWT.NONE);
            ackComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.GRAB_HORIZONTAL));

            ackComposite.setLayout(new GridLayout(2, false));
            ackComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Label basedOn = new Label(ackComposite, SWT.NONE);
            basedOn.setText("OpenClover is based on open source code of Atlassian Clover(R)");
            SwtUtils.setHorizontalSpan(basedOn, 2);

            Label generalAck = new Label(ackComposite, SWT.NONE);
            generalAck.setText("OpenClover makes use of the following 3rd party libraries:");
            SwtUtils.setHorizontalSpan(generalAck, 2);

            linkAndLicense("Annotations (IntelliJ)", "https://www.jetbrains.com", "ANNOTATIONS-13.0-LICENSE.TXT", ackComposite);
            linkAndLicense("Ant", "https://ant.apache.org", "ANT-1.5.2-LICENSE.TXT", ackComposite);
            linkAndLicense("ANTLR2 Library", "https://www.antlr2.org", "ANTLR-2.7.7-LICENSE.TXT", ackComposite);
            linkAndLicense("ANTLR3 Java Grammar", "https://www.antlr3.org", "ANTLR-JAVA-GRAMMAR-3.0-LICENSE.TXT", ackComposite);
            linkAndLicense("ASM", "https://asm.ow2.org", "ASM-5.0.3-LICENSE.TXT", ackComposite);
            linkAndLicense("Cajo", "https://java.net/projects/cajo/pages/Home", "CAJO-1.117-LICENSE.TXT", ackComposite);
            linkAndLicense("Commons Codec", "https://commons.apache.org", "COMMONS-CODEC-1.9-LICENSE.TXT", ackComposite);
            linkAndLicense("Commons Collections", "https://commons.apache.org", "COMMONS-COLLECTIONS-3.2.2-LICENSE.TXT", ackComposite);
            linkAndLicense("Commons Lang", "https://commons.apache.org", "COMMONS-LANG3-3.3.2-LICENSE.TXT", ackComposite);
            linkAndLicense("FastUtil", "https://fastutil.dsi.unimi.it/", "FASTUTIL-4.4.3-LICENSE.TXT", ackComposite);
            linkAndLicense("Groovy", "https://groovy.codehaus.org", "GROOVY-1.7.0-LICENSE.TXT", ackComposite);
            linkAndLicense("GSON", "https://code.google.com/p/google-gson", "GSON-1.3-LICENSE.TXT", ackComposite);
            linkAndLicense("Guava", "https://github.com/google/guava", "GUAVA-29.0-LICENSE.TXT", ackComposite);
            linkAndLicense("iText", "https://itextpdf.com", "ITEXT-2.0.1-LICENSE.TXT", ackComposite);
            linkAndLicense("JCommon", "https://www.jfree.org/jfreechart/", "JCOMMON-1.0.23-LICENSE.TXT", ackComposite);
            linkAndLicense("JDOM", "https://www.jdom.org", "JDOM-1.0-LICENSE.TXT", ackComposite);
            linkAndLicense("JFreechart", "https://www.jfree.org/jfreechart/", "JFREECHART-1.0.19-LICENSE.TXT", ackComposite);
            linkAndLicense("JIT", "https://thejit.org/", "JIT-1.1.2-LICENSE.TXT", ackComposite);
            linkAndLicense("jQuery", "https://jquery.com/", "JQUERY-1.8.3-LICENSE.TXT", ackComposite);
            linkAndLicense("JSON", "https://www.json.org", "JSON-LICENSE.TXT", ackComposite);
            linkAndLicense("KTreemap", "https://jtreemap.sourceforge.net/", "KTREEMAP-1.1.0-LICENSE.TXT", ackComposite);
            linkAndLicense("SLF4J", "https://www.slf4j.org/", "SLF4J-1.7.36-LICENSE.TXT", ackComposite);
            linkAndLicense("Velocity", "https://velocity.apache.org/", "VELOCITY-1.7-LICENSE.TXT", ackComposite);

            TabItem ackTab = new TabItem(tabFolder, SWT.NONE);
            ackTab.setText("Acknowledgements");
            ackTab.setControl(topAck);

            //Filler
            new Label(ackComposite, SWT.NONE);

            Label apacheAck = new Label(ackComposite, SWT.NONE);
            apacheAck.setText(
                "This product includes software developed by\n" +
                "the Apache Software Foundation (http://www.apache.org/).");
            apacheAck.setLayoutData(new GridData());
            SwtUtils.setHorizontalSpan(apacheAck, 2);
        }

        private void linkAndLicense(final String name, final String url, final String pathToLicense, Composite ackComposite) {
            Link link = new Link(ackComposite, SWT.NONE);
            link.setText("<a>" + name + "</a>");
            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    BrowserUtils.openExternalBrowser(url);
                }
            });

            Link licenseLink = new Link(ackComposite, SWT.NONE);
            licenseLink.setText("<a>View license</a>");
            licenseLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            licenseLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {

                    InputStream in = getClass().getResourceAsStream("/licenses/" + pathToLicense);
                    if (in != null) {

                        try {
                            Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                            final StringBuffer sb = new StringBuffer();
                            char[] buf = new char[1024];
                            int read = 0;
                            try {
                                while (read != -1) {
                                    sb.append(buf, 0, read);
                                    read = reader.read(buf);
                                }
                            } finally {
                                reader.close();
                            }

                            Dialog d = new Dialog(getShell()) {
                                private Font licenseFont = new Font(null, "Courier", 10, SWT.NONE);

                                @Override
                                protected void configureShell(Shell shell) {
                                    super.configureShell(shell);
                                    shell.setText(name + "licenses");
                                }

                                @Override
                                protected Control createDialogArea(Composite parent) {
                                    Composite composite = new Composite(parent, SWT.NULL);
                                    composite.setLayout(new GridLayout());
                                    composite.setFont(licenseFont);

                                    Text licenseText = new Text(composite, SWT.MULTI | SWT.SCROLL_LINE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
                                    licenseText.setLayoutData(new GridData(GridData.FILL_BOTH));
                                    licenseText.setFont(licenseFont);
                                    GC gc = new GC(licenseText);
                                    try {
                                        gc.setFont(licenseFont);
                                        FontMetrics fontMetrics = gc.getFontMetrics();

                                        ((GridData)licenseText.getLayoutData()).widthHint =
                                            convertWidthInCharsToPixels(fontMetrics,  80);
                                        ((GridData)licenseText.getLayoutData()).heightHint =
                                            convertHeightInCharsToPixels(fontMetrics, 40);
                                    } finally {
                                        gc.dispose();
                                    }


                                    licenseText.setText(sb.toString());

                                    return composite;
                                }
                            };

                            d.open();
                        } catch (IOException e) {
                        }
                    }
                }
            });
        }
    }

    @Override
    public void run(IAction action) {
        new AboutDialog(
            view.getViewSite().getPart().getSite().getShell())
            .open();
    }

}
