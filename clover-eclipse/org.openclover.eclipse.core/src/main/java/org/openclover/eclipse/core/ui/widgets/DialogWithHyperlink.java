package org.openclover.eclipse.core.ui.widgets;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.BrowserUtils;
import org.openclover.eclipse.core.ui.CloverPluginIcons;

public class DialogWithHyperlink extends IconAndMessageDialog {

    private final String linkText;

    private final String title;
    
    public DialogWithHyperlink(Shell parentShell, String title, String message) {
        super(parentShell);
        this.title = title;
        setShellStyle(getShellStyle() | SWT.RESIZE);
        this.message = null;
        this.linkText = message;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        createMessageArea(parent);
        getShell().setText(title);

        if (linkText != null) {
            final Link link = new Link(parent, getMessageLabelStyle());
            link.setText(linkText);
            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    BrowserUtils.openExternalBrowser(e.text);
                }
            });

            final int lineHeight = convertHeightInCharsToPixels(1);
            final int minimumWidth = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
            final int computedMinimumHeight = link.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).y + 1 * lineHeight;

            GridDataFactory
            .fillDefaults()
            .align(SWT.FILL, SWT.FILL)
            .grab(true, true)
            .minSize(1, computedMinimumHeight)
            .hint(minimumWidth, SWT.DEFAULT)
            .applyTo(link);
        }

        return super.createDialogArea(parent);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }


    @Override
    protected Image getImage() {
        return CloverPlugin.getImage(CloverPluginIcons.CLOVER_LOGO_ICON);
    }

}