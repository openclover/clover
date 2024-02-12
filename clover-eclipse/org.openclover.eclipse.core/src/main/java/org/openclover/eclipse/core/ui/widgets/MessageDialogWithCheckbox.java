package org.openclover.eclipse.core.ui.widgets;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A message dialog box that has a "checkbox" associated with it
 */
public class MessageDialogWithCheckbox extends MessageDialog {
    /**
     * The results from the dialog
     */
    public static class Result {
        private boolean yesSelected = true;
        private boolean checked;

        public boolean isYesSelected() {
            return yesSelected;
        }

        public void setYesSelected(boolean yesSelected) {
            this.yesSelected = yesSelected;
        }

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    private Button mCheckBox;
    private final String mCheckText;
    private final boolean mInitialChecked;
    private boolean mCheckedValue;

    public MessageDialogWithCheckbox(Shell parentShell,
                                     String dialogTitle, Image dialogTitleImage,
                                     String dialogMessage, int dialogImageType,
                                     String[] dialogButtonLabels, int defaultIndex,
                                     String checkText, boolean isChecked) {
        super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
        mCheckText = checkText;
        mInitialChecked = isChecked;
    }


    @Override
    protected Control createCustomArea(Composite parent) {
        //Filler
        new Label(parent, SWT.NONE);
        
        mCheckBox = new Button(parent, SWT.CHECK);
        mCheckBox.setText(mCheckText);
        mCheckBox.setSelection(mInitialChecked);

        return mCheckBox;
    }

    @Override
    public boolean close() {
        mCheckedValue = mCheckBox.getSelection();
        return super.close();
    }

    /**
     *
     * @param checkedText What the "checkbox" text should say
     * @param isChecked whether the checkbox is initially checked
     * @param result the results of the dialog are stored here
     */
    public static void openQuestion(Shell parent, String title, String message,
                                    boolean isYesSelected,
                                    String checkedText, boolean isChecked,
                                    Result result) {
        MessageDialogWithCheckbox dialog = new MessageDialogWithCheckbox(
            parent,
            title,
            null, // accept the default window icon
            message,
            QUESTION,
            new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL},
            isYesSelected ? 0 : 1,
            checkedText, isChecked);

        result.setYesSelected(dialog.open() == 0);
        result.checked = dialog.mCheckedValue;
    }

}
