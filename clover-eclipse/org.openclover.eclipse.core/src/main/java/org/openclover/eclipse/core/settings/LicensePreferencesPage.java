package org.openclover.eclipse.core.settings;

import com.atlassian.clover.CloverLicenseDecoder;
import com.atlassian.clover.LicenseDecoderException;
import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.GLH;
import org.openclover.eclipse.core.ui.SwtUtils;
import org.openclover.eclipse.core.ui.BrowserUtils;
import org.openclover.eclipse.core.PluginVersionInfo;
import org.openclover.eclipse.core.licensing.LicenseUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public class LicensePreferencesPage
    extends BasePreferencePage {
    
    private Panel panel;

    @Override
    public void init(IWorkbench workbench) {
        super.init(workbench);
    }

    @Override
    protected Control createContents(Composite parent) {
        try {
            return panel = new Panel(parent);
        } catch (Exception e) {
            CloverPlugin.logError("Error creating license preferences panel", e);
            return null;
        }
    }

    public class Panel extends Composite {
        private LicenseSummaryPanel summaryPanel;

        public Panel(Composite parent) throws Exception {
            super(parent, SWT.NONE);
            setLayout(new GLH().marginHeight(0).marginWidth(0).getGridLayout());

            summaryPanel = new LicenseSummaryPanel(this, LicensePreferencesPage.this);
            summaryPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            loadFromPreferences();

            pack();
        }

        public void performApply() throws IOException {
            summaryPanel.refresh();
        }


        public boolean performOk() throws IOException {
            performApply();
            return true;
        }

        public void performDefaults() throws IOException {
            loadFromPreferences();
        }

        private void loadFromPreferences() throws IOException {
            summaryPanel.refresh();
        }
    }

    @Override
    protected void performDefaults() {
        try {
            panel.performDefaults();
        } catch (IOException e) {
            CloverPlugin.logError("Unable to load preferences", e);
        }
    }

    @Override
    protected void performApply() {
        try {
            panel.performApply();
        } catch (IOException e) {
            CloverPlugin.logError("Unable to persist preferences", e);
        }
    }


    @Override
    public boolean performOk() {
        try {
            return panel.performOk();
        } catch (IOException e) {
            CloverPlugin.logError("Unable to persist preferences", e);
            return true;
        }
    }
}
