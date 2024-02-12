package org.openclover.idea.config.regexp;

import com.intellij.openapi.ui.Messages;
import com.intellij.ui.DocumentAdapter;
import org.openclover.idea.config.ContextFilterRegexpType;
import org.openclover.idea.config.GBC;
import org.openclover.idea.util.ComparatorUtil;
import org.openclover.idea.util.l10n.CloverIdeaPluginMessages;
import org.openclover.idea.util.ui.CloverIcons;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

public class EditRegexpPanel extends JPanel implements Observer {

    private Regexp model;

    private static final String METHOD_HELP = "<html>" + CloverIdeaPluginMessages.METHOD_CONTEXT_FILTER_INSTRUCTIONS_HTML();
    private static final String STATEMENT_HELP = "<html>" + CloverIdeaPluginMessages.STATEMENT_CONTEXT_FILTER_INSTRUCTIONS_HTML();

    private JLabel nameLabel;
    private JLabel typeLabel;
    private JLabel regexpLabel;

    private JTextField nameField;
    private JComboBox typeComboBox;
    private JTextField regexpField;

    private boolean isUpdating;

    public EditRegexpPanel() {
        initLayout();
        initListeners();
    }

    public void setModel(Regexp configModel) {
        if (!isUpdating) {
            try {
                isUpdating = true;
                if (model != null) {
                    model.deleteObserver(this);
                }
                if (configModel != null) {
                    getNameField().setText(configModel.getName());
                    getRegexpField().setText(configModel.getRegex());
                    getTypeComboBox().setSelectedItem(getTypeNameFromId(configModel.getType()));
                } else {
                    getNameField().setText("");
                    getRegexpField().setText("");
                    getTypeComboBox().setSelectedItem(ContextFilterRegexpType.Method.name());
                }
                model = configModel;

                if (model != null) {
                    model.addObserver(this);
                }
                setEnabled(model != null);
            } finally {
                isUpdating = false;
            }
        }
    }

    private void initLayout() {

        setLayout(new GridBagLayout());
        add(getNameLabel(), new GBC(1, 1).setAnchor(GBC.EAST).setInsets(6, 6, 5, 5));
        add(getNameField(), new GBC(2, 1).setInsets(6, 0, 5, 6).setFill(GBC.HORIZONTAL).setWeight(1.0, 0.0));
        add(getTypeLabel(), new GBC(1, 2).setAnchor(GBC.EAST).setInsets(0, 6, 5, 5));
        add(getTypeComboBox(), new GBC(2, 2).setInsets(0, 0, 5, 6).setFill(GBC.HORIZONTAL));
        add(getRegexpLabel(), new GBC(1, 3).setAnchor(GBC.EAST).setInsets(0, 6, 5, 5));
        add(getRegexpField(), new GBC(2, 3).setInsets(0, 0, 5, 6).setFill(GBC.HORIZONTAL));
        add(new JPanel(), new GBC(1, 5).setFill(GBC.BOTH).setWeight(0.0, 1.0));
    }

    private void initListeners() {
        final DocumentListener l = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                handleEditUiUpdate();
            }
        };
        getNameField().getDocument().addDocumentListener(l);
        getRegexpField().getDocument().addDocumentListener(l);

        final ActionListener al = e -> handleEditUiUpdate();
        getTypeComboBox().addActionListener(al);

    }

    /**
     * An update has been made to the EditUI, copy these changes back to the
     * associated regex context setting and validate.
     */
    private void handleEditUiUpdate() {

        // the currently focused regexp context.
        if (!isUpdating) {
            try {
                isUpdating = true;
                if (model == null) {
                    return;
                }

                if (!ComparatorUtil.areEqual(model.getName(), getNameField().getText())) {
                    model.setName(getNameField().getText());
                    //model.setChanged(true); will be done by validator now

                }
                if (!ComparatorUtil.areEqual(model.getRegex(), getRegexpField().getText())) {
                    model.setRegex(getRegexpField().getText());
                    // model.setChanged(true); will be done by validator now
                }
                if (model.getType() != getTypeIdFromString(getTypeComboBox().getSelectedItem().toString())) {
                    model.setType(getTypeIdFromString(getTypeComboBox().getSelectedItem().toString()));
                    // model.setChanged(true); will be done by validator now
                }
                model.notifyObservers();
            } finally {
                isUpdating = false;
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {
    }

    private JLabel getNameLabel() {
        if (nameLabel == null) {
            nameLabel = new JLabel("Name:");
            nameLabel.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
        }
        return nameLabel;
    }

    private JLabel getTypeLabel() {
        if (typeLabel == null) {
            typeLabel = new JLabel("Type:");
            typeLabel.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
        }
        return typeLabel;
    }

    private JLabel getRegexpLabel() {
        if (regexpLabel == null) {
            regexpLabel = new JLabel("Regexp:", CloverIcons.HELP, JLabel.RIGHT);
            regexpLabel.setDisabledIcon(CloverIcons.HELP);
            regexpLabel.setHorizontalTextPosition(JLabel.LEFT);
            regexpLabel.setAlignmentX(JLabel.RIGHT_ALIGNMENT);

            regexpLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Messages.showInfoMessage(helpText, helpTitle);
                }
            });
        }
        return regexpLabel;
    }


    private JTextField getNameField() {
        if (nameField == null) {
            nameField = new JTextField();
        }
        return nameField;
    }

    private JTextField getRegexpField() {
        if (regexpField == null) {
            regexpField = new JTextField();
        }
        return regexpField;
    }

    private String helpTitle;
    private String helpText;

    private void setRegexpHelp(ContextFilterRegexpType selected) {
        helpText = selected == ContextFilterRegexpType.Method ? METHOD_HELP : STATEMENT_HELP;
        helpTitle = "Help for " + selected + " Regexp";
        getRegexpLabel().setToolTipText(helpText);
        getRegexpField().setToolTipText(helpText);

    }

    private JComboBox getTypeComboBox() {
        if (typeComboBox == null) {
            typeComboBox = new JComboBox();
            typeComboBox.addItem(ContextFilterRegexpType.Method.name());
            typeComboBox.addItem(ContextFilterRegexpType.Statement.name());

            setRegexpHelp(ContextFilterRegexpType.Method);

            typeComboBox.addItemListener(itemEvent -> {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    setRegexpHelp(ContextFilterRegexpType.valueOf(itemEvent.getItem().toString()));
                }
            });
        }
        return typeComboBox;
    }

    private int getTypeIdFromString(String str) {
        if (str.equals(ContextFilterRegexpType.Method.name())) {
            return ContextFilterRegexpType.Method.ordinal();
        } else if (str.equals(ContextFilterRegexpType.Statement.name())) {
            return ContextFilterRegexpType.Statement.ordinal();
        }
        return -1;
    }

    private String getTypeNameFromId(int type) {
        if (type == ContextFilterRegexpType.Method.ordinal()) {
            return ContextFilterRegexpType.Method.name();
        } else if (type == ContextFilterRegexpType.Statement.ordinal()) {
            return ContextFilterRegexpType.Statement.name();
        } else {
            return "Unknown";
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        final boolean doEnable = enabled && model != null;
        super.setEnabled(doEnable);

        getNameLabel().setEnabled(doEnable);
        getNameField().setEnabled(doEnable);
        getTypeLabel().setEnabled(doEnable);
        getTypeComboBox().setEnabled(doEnable);
        getRegexpLabel().setEnabled(doEnable);
        getRegexpField().setEnabled(doEnable);
    }

}
