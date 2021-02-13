package com.atlassian.clover.eclipse.core.settings;

import com.atlassian.clover.CloverLicenseInfo;
import com.atlassian.clover.eclipse.core.CloverEclipsePluginMessages;
import com.atlassian.clover.eclipse.core.ui.SwtUtils;
import com.atlassian.clover.eclipse.core.licensing.LicenseUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.jface.dialogs.DialogPage;

public class LicenseSummaryPanel extends Composite {
    private static final int TEXT_FIELD_PREFERRED_CHARS = 60;

    private Label licenseType;
    private Label licensedStatus;
    private Label licensee;
    private LicensePreferencesPage preferencesPage;

    public LicenseSummaryPanel(Composite parent, LicensePreferencesPage preferencesPage) {
        super(parent, SWT.NONE);
        this.preferencesPage = preferencesPage;
        
        setLayout(new GridLayout());

        Group summaryGroup = new Group(this, SWT.NONE);
        summaryGroup.setText(CloverEclipsePluginMessages.LICENSE_SUMMARY());
        summaryGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL));

        summaryGroup.setLayout(new GridLayout(2, false));

        Label licensedStatusIntro = new Label(summaryGroup, SWT.NONE);
        licensedStatusIntro.setText(CloverEclipsePluginMessages.LICENSE_STATUS());
        licensedStatusIntro.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        int textWidth = preferencesPage.convertWidthInCharsToPixels(TEXT_FIELD_PREFERRED_CHARS);

        licensedStatus = SwtUtils.createMultilineLabel(summaryGroup, getDefaultText(), textWidth);

        Label licenseTypeIntro = new Label(summaryGroup, SWT.NONE);
        licenseTypeIntro.setText(CloverEclipsePluginMessages.LICENSE_TYPE());
        licenseTypeIntro.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        licenseType = SwtUtils.createMultilineLabel(summaryGroup, getDefaultText(), textWidth);

        Label licenseeIntro = new Label(summaryGroup, SWT.NONE);
        licenseeIntro.setText(CloverEclipsePluginMessages.LICENSE_STATEMENT());
        licenseeIntro.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        licensee = SwtUtils.createMultilineLabel(summaryGroup, getDefaultText(), textWidth);
    }

    private String getDefaultText() {
        return "-";
    }

    public void refresh() {
        if (CloverLicenseInfo.TERMINATED) {
            licensedStatus.setText(LicenseUtils.calcTerminationStatement());
            preferencesPage.setMessage(LicenseUtils.calcTerminationStatement(), DialogPage.ERROR);
        } else if (CloverLicenseInfo.EXPIRED) {
            licensedStatus.setText(LicenseUtils.calcExpiryStatement());
            preferencesPage.setMessage(LicenseUtils.calcExpiryStatement(), DialogPage.WARNING);
        } else {
            licensedStatus.setText(
                CloverLicenseInfo.PRE_EXPIRY_STMT.trim().length() > 0
                    ? CloverLicenseInfo.PRE_EXPIRY_STMT
                    : CloverEclipsePluginMessages.LICENSE_STATUS_LICENSED());
            preferencesPage.setMessage(null);
        }

        licenseType.setText(CloverLicenseInfo.NAME == null ? getDefaultText() : CloverLicenseInfo.NAME);
        licensee.setText(CloverLicenseInfo.OWNER_STMT == null ? getDefaultText() : CloverLicenseInfo.OWNER_STMT);

        getParent().layout();
    }
}
