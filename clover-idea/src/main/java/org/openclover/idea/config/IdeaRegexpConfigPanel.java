package com.atlassian.clover.idea.config;

import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.atlassian.clover.idea.config.regexp.EditRegexpPanel;
import com.atlassian.clover.idea.config.regexp.Regexp;
import com.atlassian.clover.idea.config.regexp.RegexpConfigModel;
import com.atlassian.clover.idea.config.regexp.RegexpListPanel;
import com.atlassian.clover.idea.util.l10n.CloverIdeaPluginMessages;
import com.atlassian.clover.idea.util.ui.MessageDialogs;
import com.atlassian.clover.idea.util.ui.RichLabel;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Key;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.EtchedBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static org.openclover.util.Lists.newArrayList;

public class IdeaRegexpConfigPanel extends ConfigPanel implements Observer {

    private static final Key<IdeaRegexpConfigPanel> COMPONENT_KEY = Key.create(IdeaRegexpConfigPanel.class.getName());

    private final RegexpConfigModel model;

    private RegexpListPanel listPanel;
    private EditRegexpPanel editPanel;
    private BlankPanel blankPanel;
    private Project project;

    private final List<Component> components = newArrayList();

    public IdeaRegexpConfigPanel(Project project, ContextStore registry) {
        model = new RegexpConfigModel(registry);
        initLayout();
        initListeners();
        this.project = project;
        project.putUserData(COMPONENT_KEY, this);
    }

    public static IdeaRegexpConfigPanel getInstance(Project project) {
        return project.getUserData(COMPONENT_KEY);
    }

    public void cleanup() {
        project.putUserData(COMPONENT_KEY, null);
    }


    public RegexpConfigModel getModel() {
        return model;
    }


    public void doAdd() {
        Regexp newCtx = new Regexp(model.getModelContextRegexpValidator());
        newCtx.setName(Regexp.suggestNewName(model.getRegexps()));
        newCtx.setRegex("");
        newCtx.setChanged(true);
        newCtx.setEnabled(true);

        model.add(newCtx);
        model.setSelected(newCtx);
        model.notifyObservers();
    }

    public void doCopy() {
        Regexp orig = model.getSelected();
        Regexp copy = new Regexp(orig);
        copy.setName(Regexp.suggestCopyName(orig, model.getRegexps()));
        copy.setChanged(true);

        model.add(copy);
        model.setSelected(copy);
        model.notifyObservers();
    }

    public void doRemove() {
        // confirm that the user wishes to delete the selected regexp filter,
        // and has not pressed the icon by accident.
        int response = MessageDialogs.showYesNoDialog(this, "Are you sure you want to delete the selected filter?", "Confirm Filter Delete");
        if (response != 0) {
            return;
        }

        Regexp selectedValue = model.getSelected();
        model.remove(selectedValue);
        model.notifyObservers();
    }


    @Override
    public void commitTo(CloverPluginConfig data) {
        model.commitTo(data);
    }

    @Override
    public void loadFrom(CloverPluginConfig data) {
        model.loadFrom(data);
        // set the first element to be selected.
        Iterator i = model.getRegexps().iterator();
        if (i.hasNext()) {
            model.setSelected((Regexp) i.next());
        }
        Regexp selected = model.getSelected();
        getEditPanel().setModel(selected);

        if (selected != null) {
            getValidationPane().setData(selected.getValidationMessage());
        }
        model.notifyObservers();
    }

    @Override
    public String getTitle() {
        return "Custom Contexts";
    }

    @Override
    public void enableConfig(boolean b) {
        super.enableConfig(b);
        for (Component component : components) {
            component.setEnabled(b);
        }
        setEnabled(b);
    }

    @Override
    public void update(Observable o, Object arg) {

        final Regexp selected = model.getSelected();

        // watch for selection change.
        if (arg == null || arg.equals(RegexpConfigModel.SELECTED)) {
            getEditPanel().setModel(selected);
        }

        getValidationPane().setData(selected != null ? selected.getValidationMessage() : "");

        //updateVisiblePanelInEditorPane();

        boolean hasChanged = false;
        for (Regexp regexp : model.getRegexps()) {
            if (regexp.isChanged()) {
                hasChanged = true;
                break;
            }
        }
        if (hasChanged) {
            getGlobalInfoPane().setData("You made changes to some context filters. Changes will only take effect after a full rebuild of your project.");
        } else {
            getGlobalInfoPane().setData(null);
        }
    }

    private void initLayout() {
        setBorder(getEnabledBorder());

        GridBagLayout gb = new GridBagLayout();
        setLayout(gb);

        JLabel instructions = new RichLabel("<html>" + CloverIdeaPluginMessages.REGEXP_CONTEXT_FILTER_INSTRUCTIONS());
        add(instructions, new GBC(1, 0).setWeight(1, 0).setFill(GBC.HORIZONTAL).setInsets(5));

        Splitter splitter = new Splitter(false, 0.4f);
        splitter.setHonorComponentsMinimumSize(true);
        splitter.setShowDividerControls(true);
        splitter.setFirstComponent(createSelectPane());
        splitter.setSecondComponent(createEditPane());

        add(splitter, new GBC(1, 1).setFill(GBC.BOTH).setWeight(1.0, 1.0).setAnchor(GBC.NORTHWEST));
        add(getGlobalInfoPane(), new GBC(1, 2).setFill(GBC.HORIZONTAL).setWeight(1, 0).setInsets(3));
    }

    private void initListeners() {

        getListPanel().setModel(model);
        model.addObserver(this);
    }

    private JPanel createSelectPane() {
        JPanel selectPane = new JPanel();
        selectPane.setLayout(new GridBagLayout());
        selectPane.add(createToolbar(), new GBC(1, 1).setAnchor(GBC.NORTHWEST).setWeight(1.0, 0.0).setFill(GBC.BOTH));
        selectPane.add(getListPanel(), new GBC(1, 2).setAnchor(GBC.NORTHWEST).setFill(GBC.BOTH).setWeight(1.0, 1.0));
        components.addAll(Arrays.asList(selectPane.getComponents()));
        return selectPane;
    }

    private JPanel createEditPane() {
        JPanel editPane = new JPanel();
        editPane.setLayout(new GridBagLayout());
        editPane.add(getBlankPanel(), new GBC(1, 1).setAnchor(GBC.EAST).setFill(GBC.BOTH).setWeight(1.0, 1.0));
        editPane.add(getEditPanel(), new GBC(1, 2).setAnchor(GBC.EAST).setFill(GBC.BOTH).setWeight(1.0, 1.0));
        editPane.add(getValidationPane(), new GBC(1, 3).setAnchor(GBC.WEST).setFill(GBC.HORIZONTAL).setWeight(0, 0).setInsets(3));
        components.addAll(Arrays.asList(editPane.getComponents()));
        return editPane;
    }

    private BlankPanel getBlankPanel() {
        if (blankPanel == null) {
            blankPanel = new BlankPanel();
        }
        return blankPanel;
    }

    private RegexpListPanel getListPanel() {
        if (listPanel == null) {
            listPanel = new RegexpListPanel();
        }
        return listPanel;
    }

    private EditRegexpPanel getEditPanel() {
        if (editPanel == null) {
            editPanel = new EditRegexpPanel();
        }
        return editPanel;
    }

    private ValidationResultPanel globalInfoPane;

    private ValidationResultPanel getGlobalInfoPane() {
        if (globalInfoPane == null) {
            globalInfoPane = new ValidationResultPanel(false);
            globalInfoPane.setVisible(false);
        }
        return globalInfoPane;
    }

    private ValidationResultPanel validationPane;

    private ValidationResultPanel getValidationPane() {
        if (validationPane == null) {
            validationPane = new ValidationResultPanel(true);
            validationPane.setVisible(false);
        }
        return validationPane;
    }

    private JComponent createToolbar() {
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup actionGroup = (ActionGroup) actionManager.getAction("CloverPlugin.ConfigToolBar");
        return actionManager.createActionToolbar("CloverConfig", actionGroup, true).getComponent();
    }


    /**
     *
     */
    static class BlankPanel extends JPanel {
        private JTextPane pane;

        public BlankPanel() {
            initLayout();
        }

        private void initLayout() {

            setLayout(new GridBagLayout());

            DefaultStyledDocument doc = new DefaultStyledDocument();
            Style s = doc.addStyle(null, null);
            StyleConstants.setIcon(s, CloverIcons.GENERAL_ADD);
            Style d = doc.addStyle(null, null);
            StyleConstants.setFontFamily(d, getFont().getFamily());
            StyleConstants.setFontSize(d, getFont().getSize());
            try {
                doc.insertString(0, "Press the ", d);
                doc.insertString(10, " ", s);
                doc.insertString(11, "button to create a new Regexp Filter configuration.", d);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            pane = new JTextPane();
            pane.setBackground(getBackground());
            pane.setDocument(doc);
            pane.setEditable(false);

            add(pane, new GBC(1, 1).setAnchor(GBC.NORTHWEST).setFill(GBC.BOTH).setWeight(0.01, 0.0));
            add(new JPanel(), new GBC(1, 2).setFill(GBC.BOTH).setWeight(0.0, 1.0));
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            pane.setEnabled(enabled);
        }
    }

    /**
     *
     */
    static class ValidationResultPanel extends JPanel {

        private JLabel label;
        private final boolean isError;

        private ValidationResultPanel(boolean isError) {
            this.isError = isError;
            initLayout();
        }

        private void initLayout() {
            // Only draw the top of the etched border.
            setBorder(new EtchedBorder() {
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    g.translate(x, y);
                    g.setColor(etchType == LOWERED ? getShadowColor(c) : getHighlightColor(c));
                    g.drawLine(0, 0, width - 2, 0);
                    g.setColor(etchType == LOWERED ? getHighlightColor(c) : getShadowColor(c));
                    g.drawLine(1, 1, width - 3, 1);
                    g.translate(-x, -y);
                }
            });
            setLayout(new GridBagLayout());
            add(getMessageLabel(), new GBC(1, 1).setFill(GBC.HORIZONTAL).setAnchor(GBC.WEST).setWeight(0.001, 0.0));
        }

        private JLabel getMessageLabel() {
            if (label == null) {
                label = new RichLabel();
                label.setIcon(isError ? CloverIcons.CONFIGURATION_ERROR : CloverIcons.CONFIGURATION_INFO);
                label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            }
            return label;
        }

        public void setData(String msg) {
            if (msg != null && msg.length() > 0) {
                getMessageLabel().setText((isError ? "<html>Error: " : "<html>") + msg);
                setVisible(true);
            } else {
                setVisible(false);
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            label.setEnabled(enabled);
            super.setEnabled(enabled);
        }
    }

}