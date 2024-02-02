package com.atlassian.clover.idea.util.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.UIUtil;
import com.atlassian.clover.idea.util.ui.CloverIcons;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Container;

public class HTMLDialog extends DialogWrapper {
    private final String text;
    private final HyperlinkListener hyperlinkListener;

    public HTMLDialog(String title, String text) {
        super(false);
        setTitle(title);
        this.text = text;
        this.hyperlinkListener = hyperlinkEvent -> {
            if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(hyperlinkEvent.getURL());
            }
        };

        init();
    }

    @Override
    protected JComponent createNorthPanel() {
        final JPanel panel = new JPanel(new BorderLayout(15, 0));

        final Container iconContainer = new Container();
        iconContainer.setLayout(new BorderLayout());
        iconContainer.add(new JLabel(CloverIcons.CLOVER_BIG), BorderLayout.NORTH);
        panel.add(iconContainer, BorderLayout.WEST);

        final JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.addHyperlinkListener(hyperlinkListener);
        textPane.setText(text);
        textPane.setCaretPosition(0);
        textPane.setBackground(UIUtil.getOptionPaneBackground());
        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " +
                       "font-size: " + font.getSize() + "pt; }";
               ((HTMLDocument)textPane.getDocument()).getStyleSheet().addRule(bodyRule);

        textPane.setOpaque(false);
        textPane.setBorder(null);

        panel.add(textPane, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected JComponent createCenterPanel() {
        return null;
    }

    @Override
    protected Action[] createActions() {
        return new Action[] {getOKAction()};
    }
}
