package org.openclover.idea.config.regexp;

import org.openclover.idea.config.ContextFilterRegexpType;
import org.openclover.idea.config.GBC;
import org.openclover.idea.util.ui.CloverIcons;
import com.intellij.ui.LayeredIcon;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

public class RegexpListPanel extends JPanel implements Observer, ListSelectionListener {

    private RegexpConfigModel model;

    private JList filterList;

    private boolean isUpdating;

    public RegexpListPanel() {
        initLayout();
        initListeners();
    }

    private void initLayout() {
        setLayout(new GridBagLayout());
        add(new JScrollPane(getFilterList()), new GBC(0, 0).setFill(GBC.BOTH).setWeight(0.01, 0.01));
    }

    private void initListeners() {
        // update the selected regexp.
        getFilterList().addListSelectionListener(this);
        getFilterList().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled()) {
                    return;
                }
                final Point location = e.getPoint();
                if (!IdeaRegexpCellRenderer.getInstance().inCbRange(location.x)) {
                    return;
                }
                int index = getFilterList().locationToIndex(location);
                if (index != -1) {
                    Regexp regexp = (Regexp) getFilterList().getModel().getElementAt(index);
                    regexp.setEnabled(!regexp.isEnabled());
                    regexp.notifyObservers();
                    repaint();
                }
            }
        });
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!isUpdating) { // we are reading from the model, so lets not write
            // to it at the same time...
            model.setSelected((Regexp) getFilterList().getSelectedValue());
            model.notifyObservers();
        }
    }

    public void setModel(RegexpConfigModel configModel) {
        if (model != null) {
            model.deleteObserver(this);
        }

        synchronized (this) {
            try {
                isUpdating = true;
                model = configModel;
                // sync list model and config model.
                syncModels();
            } finally {
                isUpdating = false;
            }
        }

        if (model != null) {
            model.addObserver(this);
        }
    }

    private void syncModels() {
        DefaultListModel listModel = (DefaultListModel) getFilterList().getModel();
        listModel.clear();

        if (model == null) {
            return;
        }
        Iterator regexps = model.getRegexps().iterator();

        if (regexps.hasNext()) {

            while (regexps.hasNext()) {
                Regexp next = (Regexp) regexps.next();
                listModel.addElement(next);
            }

            if (model.getSelected() != null) {
                getFilterList().setSelectedValue(model.getSelected(), true);
            }
        }
    }

    @Override
    public void update(Observable o, Object arg) {

        synchronized (this) {
            try {
                isUpdating = true;
                // monitor for changes in the lists..
                // either need to refresh the list (add,remove)
                // or redraw the list (edit).
                if (arg == RegexpConfigModel.EDIT) {
                    getFilterList().repaint();
                } else {
                    syncModels();
                }
            } finally {
                isUpdating = false;
            }
        }
    }

    private JList getFilterList() {
        if (filterList == null) {
            filterList = new JList();
            filterList.setModel(new DefaultListModel());
            filterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            filterList.setVisibleRowCount(7);
            filterList.setCellRenderer(IdeaRegexpCellRenderer.getInstance());
        }
        return filterList;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        getFilterList().setEnabled(b);
    }

    /**
     *
     */
    static class IdeaRegexpCellRenderer implements ListCellRenderer {
        private static final LayeredIcon INVALID_METHOD_ICON = new LayeredIcon(2);
        private static final LayeredIcon INVALID_STATEMENT_ICON = new LayeredIcon(2);

        static {
            INVALID_METHOD_ICON.setIcon(CloverIcons.METHOD_ICON, 0);
            INVALID_METHOD_ICON.setIcon(CloverIcons.CONFIGURATION_INVALID, 1);
            INVALID_STATEMENT_ICON.setIcon(CloverIcons.STATEMENT_ICON, 0);
            INVALID_STATEMENT_ICON.setIcon(CloverIcons.CONFIGURATION_INVALID, 1);
        }

        private static final JCheckBox CHECKBOX = new JCheckBox();
        private static final JLabel LABEL = new JLabel();
        private static final JPanel COMPONENT = new JPanel();
        private static final Border NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

        static {
            COMPONENT.setLayout(new GridBagLayout());

            COMPONENT.add(CHECKBOX, new GBC(0, 0));
            COMPONENT.add(LABEL, new GBC(1, 0).setFill(GBC.BOTH).setWeight(1, 0));
            CHECKBOX.setBorder(NO_FOCUS_BORDER);

        }


        private static final Color DISABLED_BG = UIManager.getColor("inactiveCaption");

        @Override
        public Component getListCellRendererComponent(JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            final Color bg = isSelected ? (list.isEnabled() ? list.getSelectionBackground() : DISABLED_BG) : list.getBackground();
            final Color fg = isSelected ? list.getSelectionForeground() : list.getForeground();

            COMPONENT.setBackground(bg);
            COMPONENT.setForeground(fg);

            CHECKBOX.setBackground(bg);
            CHECKBOX.setForeground(fg);

            LABEL.setBackground(bg);
            LABEL.setForeground(fg);

            CHECKBOX.setEnabled(list.isEnabled());
            COMPONENT.setEnabled(list.isEnabled());
            COMPONENT.setBorder((cellHasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder") : NO_FOCUS_BORDER);

            if (value instanceof Regexp) {
                Regexp ctx = (Regexp) value;

                if (ctx.getType() == ContextFilterRegexpType.Method.ordinal()) {
                        if (ctx.isValid()) {
                            LABEL.setIcon(CloverIcons.METHOD_ICON);
                        } else {
                            LABEL.setIcon(INVALID_METHOD_ICON);
                        }
                } else if (ctx.getType() == ContextFilterRegexpType.Statement.ordinal()) {
                        if (ctx.isValid()) {
                            LABEL.setIcon(CloverIcons.STATEMENT_ICON);
                        } else {
                            LABEL.setIcon(INVALID_STATEMENT_ICON);
                        }
                } else {
                        LABEL.setIcon(null);
                }
                LABEL.setText(ctx.getName());
                CHECKBOX.setSelected(ctx.isEnabled());
                if (ctx.isChanged()) {
                    LABEL.setForeground(Color.BLUE);
                }
            }
            return COMPONENT;
        }

        boolean inCbRange(int x) {
            Rectangle r = CHECKBOX.getBounds();
            return r.getMinX() <= x && r.getMaxX() >= x;
        }

        private static final IdeaRegexpCellRenderer INSTANCE = new IdeaRegexpCellRenderer();

        private static IdeaRegexpCellRenderer getInstance() {
            return INSTANCE;
        }
    }
}
